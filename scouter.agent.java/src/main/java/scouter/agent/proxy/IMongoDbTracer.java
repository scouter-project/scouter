package scouter.agent.proxy;

import scouter.agent.trace.StepTransferMap;
import scouter.agent.trace.TraceContext;

public interface IMongoDbTracer {

    StepTransferMap.ID generateAndTransferMongoQueryStep(TraceContext ctx, Object _this, Object connection);

    Object wrapCallback(StepTransferMap.ID id, Object namespace, Object command, Object readPreference, Object payload, Object callback);

    Object genCallback(StepTransferMap.ID id, Object namespace, Object command, Object readPreference, Object payload);

    void doCallback(Object callback, Object o, Throwable t);
}
