package scouter.plugin.server.kafka.profile;

import java.io.Closeable;
import java.time.Duration;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import scouter.lang.TextTypes;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.CommonSpanStep;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodStep2;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.lang.step.SocketStep;
import scouter.lang.step.SqlStep;
import scouter.lang.step.Step;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.lang.step.ThreadSubmitStep;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.server.plugin.PluginHelper;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

public class KafkaProfilePlugin implements Closeable {

    private static final Configure conf = Configure.getInstance();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static final String[] STEP_FIELDS = {
            "parent",
            "index",
            "start_time",
            "start_cpu",
            "start_mem",
            "elapsed",
            "cputime",
            "hash",
            "message",
            "ipaddr",
            "port",
            "error",
            "threaded",
            "txid",
            "address",
            "version",
            "opt",
            "updated",
            "sql",
            "param",
            "paramSql",
            "method",
            "apicall",
            "name",
            "desc",
            "time",
            "value",
            "level",
            "paramString",
            "xtype",
            "async",
            "start",
            "nameTemp"
    };

    private static final AtomicLong lastErrorLog = new AtomicLong();
    private static final AtomicLong dropped = new AtomicLong();
    private static final AsyncPublisher publisher = new AsyncPublisher(
            Math.max(1, conf.getInt("ext_plugin_kafka_profile_queue_capacity", 256)),
            KafkaProfilePlugin::reportDrops);
    private static long nextInitAttempt;
    private static long reportedDrops;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(KafkaProfilePlugin::closeQuietly,
                "scouter-kafka-profile-shutdown"));
    }

    private static volatile KafkaProducer<String, String> producer;
    private static volatile ObjectMapper objectMapper;

    private final Set<Integer> includeObjHash;
    final PluginHelper helper = PluginHelper.getInstance();

    public KafkaProfilePlugin() {
        this.includeObjHash = parseIntCsv(conf.getValue("ext_plugin_kafka_profile_include_objHashs", ""));
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_XLOG)
    public void xlog(final XLogPack p) {
        if (!conf.getBoolean("ext_plugin_kafka_profile_enabled", true) || !shouldPublish(p.objHash)) {
            return;
        }
        final XLogPack copy = new XLogPack();
        copy.objHash = p.objHash;
        copy.txid = p.txid;
        copy.endTime = p.endTime;
        copy.elapsed = p.elapsed;
        copy.service = p.service;
        copy.text1 = p.text1;
        copy.error = p.error;
        enqueue(() -> publishXlog(copy));
    }

    private void publishXlog(final XLogPack p) {
        ObjectPack op = AgentManager.getAgent(p.objHash);
        
        if(Objects.isNull(op)){
            return;
        }

        try {
            if (!initIfNeeded()) {
                dropped.incrementAndGet();
                return;
            }

            String topic = conf.getValue("ext_plugin_kafka_profile_topic", "scouter.xlog.profile");

            StringBuilder sb = new StringBuilder(64);
			sb.append(p.objHash)
              .append(':')
              .append(p.txid);
			String key = sb.toString();

			long time = p.endTime - p.elapsed;
            String yyyymmdd = DateUtil.yyyymmdd(time);

            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("plugin", "scouter-xlog-kafka");
            root.put("time", time);
            root.put("date", yyyymmdd);

            root.put("objHash", p.objHash);
            root.put("objHashHex", Hexa32.toString32(p.objHash));
            root.put("objName", safeText(yyyymmdd, TextTypes.OBJECT, p.objHash));

            root.put("serviceHash", p.service);
            root.put("serviceName", safeText(yyyymmdd, TextTypes.SERVICE, p.service));

            root.put("txid", p.txid);
            root.put("txidHex", Hexa32.toString32(p.txid));
            root.put("elapsed", p.elapsed);
            /** 신규 시작             */
            root.put("endTime",p.endTime);
            root.put("gid",p.text1);
            if(StringUtil.isNotEmpty(helper.getErrorString(p.error))) root.put("error",helper.getErrorString(p.error));
            /** 신규 종료             */
            String json = objectMapper.writeValueAsString(root);

            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logError("S1630", "kafka send error : " + exception.getMessage());
                } else if (conf.getBoolean("ext_plugin_kafka_profile_debug", false)) {
                    Logger.println("[KafkaProfilePlugin] sent topic=" + metadata.topic()
                            + ", partition=" + metadata.partition()
                            + ", offset=" + metadata.offset());
                }
            });
        } catch (Throwable t) {
            logError("S1631", "profile processing error : " + t.getMessage());
        }
    }

    @ServerPlugin(PluginConstants.PLUGIN_SERVER_PROFILE)
    public void profile(XLogProfilePack pack) {
        if (!conf.getBoolean("ext_plugin_kafka_profile_enabled", true) || !shouldPublish(pack.objHash)) {
            return;
        }
        // Bound both the number and size of retained profiles before copying their bytes.
        if (pack.profile != null && pack.profile.length > Math.max(1,
                conf.getInt("ext_plugin_kafka_profile_max_profile_bytes", 1048576))) {
            dropped.incrementAndGet();
            return;
        }
        final XLogProfilePack copy = new XLogProfilePack();
        copy.time = pack.time;
        copy.objHash = pack.objHash;
        copy.service = pack.service;
        copy.txid = pack.txid;
        copy.elapsed = pack.elapsed;
        copy.profile = pack.profile == null ? null : pack.profile.clone();
        enqueue(() -> publishProfile(copy));
    }

    private static void enqueue(Runnable task) {
        if (!publisher.offer(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                logError("S1631", "publisher error : " + t.getMessage());
            }
        })) {
            dropped.incrementAndGet();
        }
    }

    public static long getDroppedCount() {
        return dropped.get();
    }

    private void publishProfile(XLogProfilePack pack) {
        try {
            if (!initIfNeeded()) {
                dropped.incrementAndGet();
                return;
            }

            String topic = conf.getValue("ext_plugin_kafka_profile_topic", "scouter.xlog.profile");
            String key = buildKafkaKey(pack);

            Map<String, Object> payload = buildPayload(pack);
            String json = objectMapper.writeValueAsString(payload);

            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logError("S1630", "kafka send error : " + exception.getMessage());
                } else if (conf.getBoolean("ext_plugin_kafka_profile_debug", false)) {
                    Logger.println("[KafkaProfilePlugin] sent topic=" + metadata.topic()
                            + ", partition=" + metadata.partition()
                            + ", offset=" + metadata.offset());
                }
            });
        } catch (Throwable t) {
            logError("S1631", "profile processing error : " + t.getMessage());
        }
    }

    // Called only by the single publisher worker.
    private static boolean initIfNeeded() {
        if (INITIALIZED.get()) {
            return true;
        }
        if (nextInitAttempt != 0 && System.nanoTime() - nextInitAttempt < 0) {
            return false;
        }
        try {
            objectMapper = new ObjectMapper();
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

            Properties props = new Properties();
            props.put("bootstrap.servers", conf.getValue("ext_plugin_kafka_profile_bootstrap", "127.0.0.1:9092"));
            props.put("client.id", conf.getValue("ext_plugin_kafka_profile_client_id", "scouter-profile-plugin"));
            props.put("acks", conf.getValue("ext_plugin_kafka_profile_acks", "1"));
            props.put("retries", Integer.toString(conf.getInt("ext_plugin_kafka_profile_retries", 0)));
            props.put("linger.ms", Integer.toString(conf.getInt("ext_plugin_kafka_profile_linger_ms", 10)));
            props.put("batch.size", Integer.toString(conf.getInt("ext_plugin_kafka_profile_batch_size", 32768)));
            props.put("compression.type", conf.getValue("ext_plugin_kafka_profile_compression", "lz4"));
            props.put("max.request.size", Integer.toString(conf.getInt("ext_plugin_kafka_profile_max_request_size", 1048576)));
            props.put("delivery.timeout.ms", Integer.toString(conf.getInt("ext_plugin_kafka_profile_delivery_timeout_ms", 3000)));
            props.put("request.timeout.ms", Integer.toString(conf.getInt("ext_plugin_kafka_profile_request_timeout_ms", 1000)));
            props.put("buffer.memory", Integer.toString(conf.getInt("ext_plugin_kafka_profile_buffer_memory", 8388608)));
            props.put("max.block.ms", Integer.toString(Math.max(0,
                    conf.getInt("ext_plugin_kafka_profile_max_block_ms", 100))));
            props.put("key.serializer", org.apache.kafka.common.serialization.StringSerializer.class.getName());
            props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.class.getName());

            producer = new KafkaProducer<String, String>(props);

            INITIALIZED.set(true);
            Logger.println("[KafkaProfilePlugin] initialized");
        } catch (Throwable t) {
            INITIALIZED.set(false);
            nextInitAttempt = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            logError("S1632", "init error : " + t.getMessage());
            return false;
        }
        return true;
    }

    private static void reportDrops() {
        long count = dropped.get();
        if (count != reportedDrops && logError("S1635", "publish queue/size/init rejection")) {
            reportedDrops = count;
        }
    }

    private static boolean logError(String code, String message) {
        long now = System.nanoTime();
        long previous = lastErrorLog.get();
        if ((previous == 0 || now - previous >= TimeUnit.SECONDS.toNanos(30))
                && lastErrorLog.compareAndSet(previous, now)) {
            Logger.println(code, "[KafkaProfilePlugin] " + message + ", dropped=" + dropped.get());
            return true;
        }
        return false;
    }

    private static Map<String, Object> buildPayload(XLogProfilePack pack) {
        String yyyymmdd = DateUtil.yyyymmdd(pack.time);

        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("plugin", "scouter-profile-kafka");
        root.put("time", pack.time);
        root.put("date", yyyymmdd);

        root.put("objHash", pack.objHash);
        root.put("objName", safeText(yyyymmdd, TextTypes.OBJECT, pack.objHash));

        root.put("serviceHash", pack.service);
        root.put("serviceName", safeText(yyyymmdd, TextTypes.SERVICE, pack.service));

        root.put("txid", pack.txid);
        root.put("txidHex", Hexa32.toString32(pack.txid));
        root.put("elapsed", pack.elapsed);

        if (pack.profile != null) {
            root.put("steps", decodeSteps(pack.profile, yyyymmdd));
        } else {
            root.put("steps", Collections.emptyList());
        }

        return root;
    }

    private static List<Map<String, Object>> decodeSteps(byte[] profileBytes, String yyyymmdd) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        try {
            List<Step> steps = Step.toObjectList(profileBytes);
            if (steps == null) {
                return result;
            }

            for (Step step : steps) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();

                item.put("stepClass", step.getClass().getName());
                item.put("stepType", step.getStepType());
                item.put("stepTypeName", step.getStepTypeName());

                copyKnownFields(step, item);

                String hashText = resolveRepresentativeText(step, yyyymmdd);
                putIfNotNull(item, "hashText", hashText);

                result.add(item);
            }
        } catch (Throwable t) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("decodeError", t.getMessage());
            result.add(error);
        }

        return result;
    }

    private static void copyKnownFields(Step step, Map<String, Object> item) {
        for (String fieldName : STEP_FIELDS) {
            copyFieldIfExists(step, item, fieldName);
        }
    }

    private static String resolveRepresentativeText(Step step, String yyyymmdd) {
        String text;

        if (step instanceof MethodStep) {
            text = safeText(yyyymmdd, TextTypes.METHOD, ((MethodStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof SqlStep) {
            text = safeText(yyyymmdd, TextTypes.SQL, ((SqlStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof ApiCallStep) {
            text = safeText(yyyymmdd, TextTypes.APICALL, ((ApiCallStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof ThreadSubmitStep) {
            text = safeText(yyyymmdd, TextTypes.APICALL, ((ThreadSubmitStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof DispatchStep) {
            text = safeText(yyyymmdd, TextTypes.APICALL, ((DispatchStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof ThreadCallPossibleStep) {
            text = safeText(yyyymmdd, TextTypes.APICALL, ((ThreadCallPossibleStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof CommonSpanStep) {
            text = safeText(yyyymmdd, TextTypes.SERVICE, ((CommonSpanStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof HashedMessageStep) {
            text = safeText(yyyymmdd, TextTypes.HASH_MSG, ((HashedMessageStep) step).getHash());
            if (text != null) {
                return text;
            }
        }

        if (step instanceof ParameterizedMessageStep) {
            ParameterizedMessageStep pmStep = (ParameterizedMessageStep) step;
            String format = safeText(yyyymmdd, TextTypes.HASH_MSG, pmStep.getHash());
            if (format != null) {
                text = pmStep.buildMessasge(format);
                if (text != null) {
                    return text;
                }
            }
        }

        if (step instanceof MessageStep) {
            text = ((MessageStep) step).getMessage();
            if (text != null) {
                return text;
            }
        }

        if (step instanceof SocketStep) {
            text = step.toString();
            if (text != null) {
                return text;
            }
        }

        Integer error = extractError(step);
        if (error != null && error.intValue() != 0) {
            text = safeText(yyyymmdd, TextTypes.ERROR, error.intValue());
            if (text != null) {
                return text;
            }
        }

        return null;
    }

    private static Integer extractError(Step step) {
        if (step instanceof SqlStep) {
            return Integer.valueOf(((SqlStep) step).getError());
        }

        if (step instanceof MethodStep2) {
            return Integer.valueOf(((MethodStep2) step).getError());
        }

        if (step instanceof ApiCallStep) {
            return Integer.valueOf(((ApiCallStep) step).getError());
        }

        if (step instanceof ThreadSubmitStep) {
            return Integer.valueOf(((ThreadSubmitStep) step).getError());
        }

        if (step instanceof DispatchStep) {
            return Integer.valueOf(((DispatchStep) step).getError());
        }

        if (step instanceof CommonSpanStep) {
            return Integer.valueOf(((CommonSpanStep) step).getError());
        }

        if (step instanceof SocketStep) {
            return Integer.valueOf(((SocketStep) step).getError());
        }

        return getIntField(step, "error");
    }

    private static void putIfNotNull(Map<String, Object> item, String key, Object value) {
        if (value != null) {
            item.put(key, value);
        }
    }

    private static String safeText(String yyyymmdd, String textType, int hash) {
        try {
            return TextRD.getString(yyyymmdd, textType, hash);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void copyFieldIfExists(Object source, Map<String, Object> target, String fieldName) {
        try {
            Field f = findField(source.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object value = f.get(source);
            target.put(fieldName, value);
        } catch (Throwable ignore) {
        }
    }

    private static Integer getIntField(Object source, String fieldName) {
        try {
            Field f = findField(source.getClass(), fieldName);
            if (f == null) {
                return null;
            }
            f.setAccessible(true);
            Object value = f.get(source);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return Integer.valueOf(((Number) value).intValue());
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> cursor = clazz;
        while (cursor != null && cursor != Object.class) {
            try {
                return cursor.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignore) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static String buildKafkaKey(XLogProfilePack pack) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(pack.objHash)
          .append(':')
          .append(pack.txid);
        return sb.toString();
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private static void closeQuietly() {
        publisher.close(() -> {
            try {
                if (producer != null) {
                    producer.close(Duration.ofMillis(1000));
                }
            } catch (Throwable t) {
                logError("S1633", "close error : " + t.getMessage());
            }
        });
    }

    private boolean shouldPublish(int objHash) {
        if (includeObjHash.isEmpty()) {
            return true;
        }
        return includeObjHash.contains(objHash);
    }

    private Set<Integer> parseIntCsv(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> result = new LinkedHashSet<Integer>();
        String[] tokens = value.split(",");

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                Logger.println("S1634", "[KafkaProfilePlugin] config except objHashs parsing error : " + e.getMessage());
                return Collections.emptySet();
            }
        }

        return result;
    }
}