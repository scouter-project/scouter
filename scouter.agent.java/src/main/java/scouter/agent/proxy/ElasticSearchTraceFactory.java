package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;

public class ElasticSearchTraceFactory {

    private static IElasticSearchTracer tracer;
    private static Object lock = new Object();

    private static final String CLIENT = "scouter.xtra.java8.ElasticSearchTracer";
    public static final IElasticSearchTracer dummy = new IElasticSearchTracer() {
        @Override
        public String getRequestDescription(TraceContext ctx, Object httpRequestBase) {
            return "-";
        }

        @Override
        public String getNode(TraceContext ctx, Object hostOrNode) {
            return "Unknown-ElasticSearch";
        }

        @Override
        public Throwable getResponseError(Object httpRequestBase0, Object httpResponse0) {
            return null;
        }
    };

    public static IElasticSearchTracer create(ClassLoader parent) {
        try {
            if (tracer == null) {
                synchronized (lock) {
                    if (tracer == null) {
                        ClassLoader loader = LoaderManager.getOnlyForJava8Plus(parent);
                        if (loader == null) {
                            Logger.println("IElasticSearchTracer Client Load Error.. Dummy Loaded");
                            tracer = dummy;
                        } else {
                            Class c = Class.forName(CLIENT, true, loader);
                            tracer = (IElasticSearchTracer) c.newInstance();
                        }
                    }
                }
            }
            return tracer;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.println("SC-145", "fail to create", e);
            return dummy;
        }
    }

}
