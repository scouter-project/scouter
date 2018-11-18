package scouter.agent.proxy;

import scouter.agent.Logger;

public class KafkaTraceFactory {

    private static IKafkaTracer kafkaTracer;
    private static Object lock = new Object();

    private static final String CLIENT = "scouter.xtra.kafka.KafkaTracer";
    public static final IKafkaTracer dummy = new IKafkaTracer() {
        public String getBootstrapServer(Object kafkaConfig) {
            return "dummy";
        }
    };
    public static IKafkaTracer create(ClassLoader parent) {
        try {
            if (kafkaTracer == null) {
                synchronized (lock) {
                    if (kafkaTracer == null) {
                        ClassLoader loader = LoaderManager.getKafkaClient(parent);
                        if (loader == null) {
                            Logger.println("Kafka Client Load Error.. Dummy Loaded");
                            kafkaTracer = dummy;
                        } else {
                            Class c = Class.forName(CLIENT, true, loader);
                            kafkaTracer = (IKafkaTracer) c.newInstance();
                        }
                    }
                }
            }
            return kafkaTracer;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.println("SC-145", "fail to create", e);
            return dummy;
        }
    }

}
