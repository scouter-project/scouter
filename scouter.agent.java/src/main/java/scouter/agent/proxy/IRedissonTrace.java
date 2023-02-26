package scouter.agent.proxy;

import scouter.agent.trace.TraceContext;

public interface IRedissonTrace {

    void startRedis(TraceContext ctx, Object channel);

    String getCommand(Object command);

    String parseArgs(Object object);

}
