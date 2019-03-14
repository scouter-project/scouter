/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.http.model;

import scouter.lang.Counter;
import scouter.lang.CounterKey;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.NumberValue;
import scouter.lang.value.ValueEnum;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.ScouterTgMtConfig;
import scouter.server.core.app.MeterCounter;
import scouter.server.core.app.MeterCounterManager;
import scouter.server.core.cache.CounterTimeCache;
import scouter.util.HashUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class InfluxSingleLine {

    private String measurement;
    private String host;
    private String family;
    private String objType;
    private String objTypeIcon;
    private String objName;
    private int objHash;
    long receivedTime;
    long timestampOrigin;
    boolean debug;

    Map<String, String> tags;
    Map<CounterProtocol, NumberValue> numberFields = new HashMap<CounterProtocol, NumberValue>();

    private InfluxSingleLine(ScouterTgMtConfig tConfig,
                             String measurement,
                             Map<String, String> tags,
                             Map<String, String> fields,
                             long receivedTime,
                             long timestampOrigin,
                             boolean debug) {

        this.measurement = measurement;
        this.tags = tags;
        this.receivedTime = receivedTime;
        this.timestampOrigin = timestampOrigin;
        this.host = tConfig.toHost(tags);
        this.family = tConfig.toFamily(tags);
        this.objType = tConfig.toObjType(tags);
        this.objTypeIcon = tConfig.getObjTypeIcon();
        this.objName = tConfig.toObjName(host, tags);
        this.objHash = HashUtil.hash(objName);
        this.debug = debug;

        for (Map.Entry<String, String> field : fields.entrySet()) {
            addNumField(tConfig, field);
        }
    }

    private void addNumField(ScouterTgMtConfig tConfig, Map.Entry<String, String> field) {
        CounterProtocol counterProtocol = tConfig.getCounterProtocol(field.getKey());
        if (counterProtocol == null) {
            return;
        }

        String valueStr = field.getValue();
        char lastChar = valueStr.charAt(valueStr.length() - 1);

        if (lastChar == 'i') { //long
            long value = Long.parseLong(valueStr.substring(0, valueStr.length() - 1));
            numberFields.put(counterProtocol, new DecimalValue(value));
        } else if (lastChar >= '0' && lastChar <= '9') { //float
            numberFields.put(counterProtocol, new FloatValue(Float.parseFloat(valueStr)));
        } else {
            //skip - boolean or string
            return;
        }
    }

    /**
     * line string key is measurement + tag&values + first field keys (TODO check again)
     */
    public static String toLineStringKey(String lineString) {
        char[] chars = lineString.toCharArray();
        char sink = '\0';
        int mode = 0; //0: measurement, 1: tags

        StringBuilder lineKey = new StringBuilder(80);
        for (int pos = 0; pos < lineString.length(); pos++) {
            char c = chars[pos];
            if (mode == 0) { //measurement, tags
                if (sink == '\\') {
                    lineKey.append(c);
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ' ':
                            lineKey.append(' ');
                            mode++;
                            break;
                        default:
                            lineKey.append(c);
                            break;
                    }
                }

            } else if (mode == 1) { //fields
                if (sink == '\\') {
                    lineKey.append(c);
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ' ':
                            mode++;
                            break;
                        case '=':
                            mode++;
                            break;
                        case ',':
                            mode++;
                            break;
                        default:
                            lineKey.append(c);
                            break;
                    }
                }
            } else {
                break;
            }

        }

        return lineKey.toString();
    }

    public static InfluxSingleLine of(String lineStr, Configure configure, long receivedTime) {
        char[] chars = lineStr.toCharArray();
        char sink = '\0';
        int mode = 0; //0: measurement, 1: tags, 2: fields, 3: timestamp

        StringBuilder measurementSb = new StringBuilder();

        StringBuilder tagKeySb = new StringBuilder();
        StringBuilder tagValueSb = new StringBuilder();
        boolean tagKeyMode = true; //if false then tag value mode
        Map<String, String> tags = new HashMap<String, String>();

        StringBuilder fieldKeySb = new StringBuilder();
        StringBuilder fieldValueSb = new StringBuilder();
        boolean fieldKeyMode = true; //if false then field value mode
        Map<String, String> fields = new HashMap<String, String>();

        StringBuilder timestampSb = new StringBuilder();

        for (int pos = 0; pos < lineStr.length(); pos++) {
            char c = chars[pos];
            if (mode == 0) { //measurement
                if (sink == '\\') {
                    measurementSb.append(c);
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ',':
                            mode++;
                            break;
                        default:
                            measurementSb.append(c);
                            break;
                    }
                }

            } else if (mode == 1) { //tags
                if (sink == '\\') {
                    if (tagKeyMode) {
                        tagKeySb.append(c);
                    } else {
                        tagValueSb.append(c);
                    }
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            sink = '\\';
                            break;
                        case ' ':
                            mode++;
                            if (tagKeySb.length() > 0) {
                                tags.put(tagKeySb.toString(), tagValueSb.toString());
                            }
                            break;
                        case '=':
                            tagKeyMode = false;
                            break;
                        case ',':
                            tagKeyMode = true;
                            tags.put(tagKeySb.toString(), tagValueSb.toString());
                            tagKeySb = new StringBuilder();
                            tagValueSb = new StringBuilder();
                            break;
                        default:
                            if (tagKeyMode) {
                                tagKeySb.append(c);
                            } else {
                                tagValueSb.append(c);
                            }
                            break;
                    }
                }

            } else if (mode == 2) { //fields
                if (sink == '\\') {
                    if (fieldKeyMode) {
                        fieldKeySb.append(c);
                    } else {
                        fieldValueSb.append(c);
                    }
                    sink = '\0';

                } else {
                    switch (c) {
                        case '\\':
                            if (sink != '"') {
                                sink = '\\';
                            }
                            break;
                        case '"':
                            if (sink == '"') {
                                sink = '\0';
                            } else {
                                sink = '"';
                            }
                            break;
                        case ' ':
                            if (sink != '"') {
                                mode++;
                                if (fieldKeySb.length() > 0) {
                                    fields.put(fieldKeySb.toString(), fieldValueSb.toString());
                                }
                            }
                            break;
                        case '=':
                            if (sink != '"') {
                                fieldKeyMode = false;
                            }
                            break;
                        case ',':
                            if (sink != '"') {
                                fieldKeyMode = true;
                                fields.put(fieldKeySb.toString(), fieldValueSb.toString());
                                fieldKeySb = new StringBuilder();
                                fieldValueSb = new StringBuilder();
                            }
                            break;
                        default:
                            if (sink != '"') {
                                if (fieldKeyMode) {
                                    fieldKeySb.append(c);
                                } else {
                                    fieldValueSb.append(c);
                                }
                            }
                            break;
                    }
                }

            } else if (mode == 3) { // timestamp
                timestampSb.append(c);
            }
        }

        String measurement = measurementSb.toString();
        ScouterTgMtConfig tConfig = configure.telegrafInputConfigMap.get(measurement);

        if (tConfig == null) {
            return null;
        }
        if (!configure.input_telegraf_debug_enabled && tConfig.isDebugEnabled()) {
            Logger.println("TG006", "[line protocol received] " + lineStr);
        }
        if (!tConfig.isEnabled()) {
            return null;
        }
        if (!tConfig.isValidConfig()) {
            return null;
        }
        if (!tConfig.isTagFilterMatching(tags)) {
            return null;
        }

        try {
            //line protocol timestamp is nano second -> divide 1000,000 -> to millis
            return new InfluxSingleLine(tConfig, measurement, tags, fields, receivedTime, Long.parseLong(timestampSb.toString()) / 1000000, tConfig.isDebugEnabled());
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public String getMeasurement() {
        return measurement;
    }

    public String getHost() {
        return host;
    }

    public String getFamily() {
        return family;
    }

    public String getObjType() {
        return objType;
    }

    public String getObjTypeIcon() {
        return objTypeIcon;
    }

    public String getObjName() {
        return objName;
    }

    public int getObjHash() {
        return objHash;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    public long getTimestampOrigin() {
        return timestampOrigin;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<CounterProtocol, NumberValue> getNumberFields() {
        return numberFields;
    }

    public boolean isDebug() {
        return debug;
    }

    public ObjectPack toObjectPack(String address, int deadTime) {
        ObjectPack objPack = new ObjectPack();
        objPack.objHash = this.objHash;
        objPack.objName = this.objName;
        objPack.objType = this.objType;
        objPack.address = address;
        MapValue tagMap = new MapValue();
        tagMap.put(ObjectPack.TAG_KEY_DEAD_TIME, deadTime);
        objPack.tags = tagMap;

        return objPack;
    }

    public PerfCounterPack toPerfCounterPack() {
        PerfCounterPack perfPack = new PerfCounterPack();
        perfPack.time = this.receivedTime;
        perfPack.timetype = TimeTypeEnum.REALTIME;
        perfPack.objName = this.objName;

        for (Map.Entry<CounterProtocol, NumberValue> counterValueEntry : numberFields.entrySet()) {
            CounterProtocol counterProtocol = counterValueEntry.getKey();

            Counter normalCounter = counterProtocol.toNormalCounter(tags);
            NumberValue counterValue = counterValueEntry.getValue();

            if (normalCounter != null) {
                if (counterProtocol.getNormalizeSec() > 0 && !counterProtocol.hasDeltaCounter()) {
                    MeterCounter meterCounter = MeterCounterManager.getInstance().getMeterCounter(objHash, normalCounter.getName());
                    meterCounter.add(counterValue.doubleValue());
                    double valueNormalized = (meterCounter.getAvg(counterProtocol.getNormalizeSec()));

                    perfPack.data.put(normalCounter.getName(), new DoubleValue(valueNormalized));
                } else {
                    perfPack.data.put(normalCounter.getName(), counterValue);
                }
            }

            Counter deltaCounter = counterProtocol.toDeltaCounter(tags);
            if (deltaCounter != null) {
                CounterKey counterKey = new CounterKey(this.objHash, deltaCounter.getName(), TimeTypeEnum.REALTIME);
                NumberValueWithTime prev = CounterTimeCache.get(counterKey);

                if (prev != null) {
                    float deltaPerSec;

                    switch (counterValue.getValueType()) {
                        case ValueEnum.DECIMAL:
                            long delta = counterValue.longValue() - prev.getValue().longValue();
                            deltaPerSec = delta * 1000.0f / (this.timestampOrigin - prev.getTime());
                            break;
                        default:
                            float delta_f = counterValue.floatValue() - prev.getValue().floatValue();
                            deltaPerSec = delta_f * 1000.0f / (this.timestampOrigin - prev.getTime());
                            break;
                    }

                    if (counterProtocol.getNormalizeSec() > 0) {
                        MeterCounter meterCounter = MeterCounterManager.getInstance().getMeterCounter(objHash, deltaCounter.getName());
                        meterCounter.add(deltaPerSec);
                        deltaPerSec = (float) (meterCounter.getAvg(counterProtocol.getNormalizeSec()));

                    } else {
                        Configure conf = Configure.getInstance();
                        if (conf.input_telegraf_delta_counter_normalize_default) {
                            MeterCounter meterCounter = MeterCounterManager.getInstance().getMeterCounter(objHash, deltaCounter.getName());
                            meterCounter.add(deltaPerSec);
                            deltaPerSec = (float) (meterCounter.getAvg(conf.input_telegraf_delta_counter_normalize_default_seconds));
                        }
                    }

                    perfPack.data.put(deltaCounter.getName(), new FloatValue(deltaPerSec));
                }

                CounterTimeCache.put(counterKey, new NumberValueWithTime(counterValue, this.timestampOrigin));
            }
        }
        return perfPack;
    }

    @Override
    public String toString() {
        return "InfluxSingleLine{" +
                "measurement='" + measurement + '\'' +
                ", host='" + host + '\'' +
                ", objType='" + objType + '\'' +
                ", objName='" + objName + '\'' +
                ", objHash=" + objHash +
                ", receivedTime=" + receivedTime +
                ", timestampOrigin=" + timestampOrigin +
                ", tags=" + tags +
                ", numberFields=" + numberFields +
                '}';
    }
}
