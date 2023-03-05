package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;

public class RedissonTraceFactory {

    private static IRedissonTrace redissonTrace;
    private static Object lock = new Object();

    private static final String CLIENT = "scouter.xtra.redis.RedissonTracer";
    public static final IRedissonTrace dummy = new IRedissonTrace() {
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
    public static IRedissonTrace create(ClassLoader parent) {
        try {
            if (redissonTrace == null) {
                synchronized (lock) {
                    if (redissonTrace == null) {
                        ClassLoader loader = LoaderManager.getRedisClient(parent);
                        if (loader == null) {
                            Logger.println("Lettuce Client Load Error.. Dummy Loaded");
                            redissonTrace = dummy;
                        } else {
                            Class c = Class.forName(CLIENT, true, loader);
                            redissonTrace = (IRedissonTrace) c.newInstance();
                        }
                    }
                }
            }
            return redissonTrace;
        } catch (Throwable e) {
            e.printStackTrace();
            Logger.println("SC-146.1", "fail to create", e);
            return dummy;
        }
    }

}
