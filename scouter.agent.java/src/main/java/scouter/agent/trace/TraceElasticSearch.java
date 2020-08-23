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

package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.ElasticSearchTraceFactory;
import scouter.agent.proxy.IElasticSearchTracer;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.util.StringUtil;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2020/08/16
 */
public class TraceElasticSearch {

    private static String ES_COMMAND_MSG = "[ElasticSearch] %s";
    private static String ES_COMMAND_ERROR_MSG = "[ElasticSearch] %s\n[Exception:%s] %s";

    static IElasticSearchTracer tracer;
    static Configure conf = Configure.getInstance();

    public static void startRequest(Object httpRequestBase) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return;
        }
        if (tracer == null) {
            tracer = ElasticSearchTraceFactory.create(httpRequestBase.getClass().getClassLoader());
        }

        try {
            String esRequestDesc = tracer.getRequestDescription(ctx, httpRequestBase);

            ParameterizedMessageStep step = new ParameterizedMessageStep();
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            step.putTempMessage("desc", esRequestDesc);
            ctx.profile.add(step);
            StepTransferMap.put(System.identityHashCode(httpRequestBase), ctx, step);
        } catch (Throwable e) {
            Logger.println("ES001", e.getMessage(), e);
        }
    }

    public static void endRequest(Object httpUriRequest, Object httpHost, Object httpResponse) {
        endRequestFinal(httpUriRequest, httpResponse, httpHost, null);
    }

    public static void endFailRequest(Object httpUriRequest, Object node, Exception exception) {
        endRequestFinal(httpUriRequest, null, node, exception);
    }

    private static void endRequestFinal(Object httpRequestBase, Object httpResponseBase, Object hostOrNode, Throwable throwable) {
        if (httpRequestBase == null) {
            return;
        }

        try {
            int requestBaseHash = System.identityHashCode(httpRequestBase);
            StepTransferMap.ID id = StepTransferMap.get(requestBaseHash);
            if (id == null) {
                return;
            }
            StepTransferMap.remove(requestBaseHash);

            TraceContext ctx = id.ctx;
            ParameterizedMessageStep step = (ParameterizedMessageStep) id.step;
            if (ctx == null || step == null) return;

            if (tracer == null) {
                tracer = ElasticSearchTraceFactory.create(httpRequestBase.getClass().getClassLoader());
            }
            if (throwable == null && httpResponseBase != null) {
                throwable = tracer.getResponseError(httpRequestBase, httpResponseBase);
            }

            int elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - step.start_time;
            step.setElapsed(elapsed);

            String desc = step.getTempMessage("desc");

            if (StringUtil.isEmpty(desc)) desc = "-";

            if (throwable == null) {
                step.setMessage(DataProxy.sendHashedMessage(ES_COMMAND_MSG), desc);
                step.setLevel(ParameterizedMessageLevel.INFO);

            } else {
                String msg = throwable.toString();
                step.setMessage(DataProxy.sendHashedMessage(ES_COMMAND_ERROR_MSG), desc, throwable.getClass().getName(), msg);
                step.setLevel(ParameterizedMessageLevel.ERROR);

                if (ctx.error == 0 && conf.xlog_error_on_elasticsearch_exception_enabled) {
                    ctx.error = DataProxy.sendError(msg);
                }
                //TODO not yet error summary processing for es : ctx.offerErrorEntity(ErrorEntity.of(throwable, ctx.error, 0, 0));
            }
//            ctx.profile.pop(step);

            if (conf.counter_interaction_enabled) {
                String node = tracer.getNode(ctx, hostOrNode);
                int nodeHash = DataProxy.sendObjName(node);
                MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getElasticSearchCallMeter(conf.getObjHash(), nodeHash);
                if (meterInteraction != null) {
                    meterInteraction.add(elapsed, throwable != null);
                }
            }
        } catch (Throwable e) {
            Logger.println("ES002", e.getMessage(), e);
        }
    }
}
