package scouter.agent.proxy;

import scouter.agent.trace.TraceContext;

public interface ILettuceTrace {

    void startRedis(TraceContext ctx, Object channel);

    String getCommand(Object command);

    String parseArgs(Object object);

}
