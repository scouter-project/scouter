package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.StepTransferMap;
import scouter.agent.trace.TraceContext;

import static scouter.agent.trace.TraceMongoDB.V364;
import static scouter.agent.trace.TraceMongoDB.V382;
import static scouter.agent.trace.TraceMongoDB.V405;

public class MongoDbTraceFactory {

    private static IMongoDbTracer tracer;
    private static Object lock = new Object();

    private static final String CLIENT364 = "scouter.xtra.java8.MongoDbTracer364";
    private static final String CLIENT382 = "scouter.xtra.java8.MongoDbTracer382";
    private static final String CLIENT405 = "scouter.xtra.java8.MongoDbTracer405";
    public static final IMongoDbTracer dummy = new IMongoDbTracer() {
        @Override
        public StepTransferMap.ID generateAndTransferMongoQueryStep(TraceContext ctx, Object _this, Object connection) {
            return null;
        }
        @Override
        public Object wrapCallback(StepTransferMap.ID id, Object namespace, Object command, Object readPreference, Object payload, Object callback) {
            return callback;
        }

        @Override
        public Object genCallback(StepTransferMap.ID id, Object namespace, Object command, Object readPreference, Object payload) {
            return null;
        }

        @Override
        public void doCallback(Object callback, Object o, Throwable t) {
        }
    };

    public static IMongoDbTracer create(ClassLoader parent, String version) {
        try {
            if (tracer == null) {
                synchronized (lock) {
                    if (tracer == null) {
                        ClassLoader loader = LoaderManager.getOnlyForJava8Plus(parent);
                        if (loader == null) {
                            Logger.println("IMongoDBTracer Client Load Error.. Dummy Loaded");
                            tracer = dummy;
                        } else {
                            Class c = null;
                            if (version.equals(V405)) {
                                c = Class.forName(CLIENT405, true, loader);
                            } else if (version.equals(V382)) {
                                c = Class.forName(CLIENT382, true, loader);
                            } else if (version.equals(V364)) {
                                c = Class.forName(CLIENT364, true, loader);
                            } else {
                                return dummy;
                            }
                            tracer = (IMongoDbTracer) c.newInstance();
                        }
                    }
                }
            }
            return tracer;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.println("MD-c01", "fail to create", e);
            return dummy;
        }
    }

}
