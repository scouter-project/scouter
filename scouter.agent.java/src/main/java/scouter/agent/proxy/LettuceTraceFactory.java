package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;

public class LettuceTraceFactory {

    private static ILettuceTrace lettuceTrace;
    private static Object lock = new Object();

    private static final String CLIENT = "scouter.xtra.redis.LettuceTracer";
    public static final ILettuceTrace dummy = new ILettuceTrace() {
        @Override
        public void startRedis(TraceContext ctx, Object channel) {
        }

        @Override
        public String getCommand(Object command) {
            return null;
        }

        @Override
        public String parseArgs(Object object) {
            return null;
        }
    };
    public static ILettuceTrace create(ClassLoader parent) {
        try {
            if (lettuceTrace == null) {
                synchronized (lock) {
                    if (lettuceTrace == null) {
                        ClassLoader loader = LoaderManager.getRedisClient(parent);
                        if (loader == null) {
                            Logger.println("Lettuce Client Load Error.. Dummy Loaded");
                            lettuceTrace = dummy;
                        } else {
                            Class c = Class.forName(CLIENT, true, loader);
                            lettuceTrace = (ILettuceTrace) c.newInstance();
                        }
                    }
                }
            }
            return lettuceTrace;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.println("SC-146", "fail to create", e);
            return dummy;
        }
    }

}
