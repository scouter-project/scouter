package scouter.agent.proxy;

import scouter.agent.trace.TraceContext;

public interface IElasticSearchTracer {

    String getRequestDescription(TraceContext ctx, Object httpRequestBase);

    String getNode(TraceContext ctx, Object hostOrNode);

    Throwable getResponseError(Object httpRequestBase0, Object httpResponse0);
}
