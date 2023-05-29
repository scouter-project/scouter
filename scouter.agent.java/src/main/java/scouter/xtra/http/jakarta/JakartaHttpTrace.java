/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.xtra.http.jakarta;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import scouter.agent.error.ASYNC_SERVLET_TIMEOUT;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.agent.trace.TransferMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static scouter.agent.AgentCommonConstant.*;

public class JakartaHttpTrace extends JakartaHttpTraceBase implements IHttpTrace {

    public void addAsyncContextListener(Object ac) {
        TraceContext traceContext = TraceContextManager.getContext();
        if(traceContext == null) return;

        AsyncContext actx = (AsyncContext)ac;
        if(actx.getRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH) == null) {
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_INITIAL_TRACE_CONTEXT, traceContext);
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_TRACE_CONTEXT, traceContext);
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH, 1);
        } else {
            int dispatchCount = (Integer) actx.getRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH);
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_TRACE_CONTEXT, traceContext);
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH, ++dispatchCount);
        }

        List<TraceContext> dispatchedContexts = (List<TraceContext>)actx.getRequest().getAttribute(REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT);
        if(dispatchedContexts == null) {
            dispatchedContexts = new ArrayList<TraceContext>();
            actx.getRequest().setAttribute(REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT, dispatchedContexts);
        }
        if(!dispatchedContexts.contains(traceContext)) {
            dispatchedContexts.add(traceContext);
        }

        actx.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent asyncEvent) throws IOException {
//                System.out.println("[scouter][asynccontext]onComplete:count: " + asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH) + " => " + this.toString());
//                System.out.println("[scouter][asynccontext]onComplete:thread: " + Thread.currentThread().getName());

                List<TraceContext> traceContextList = (List<TraceContext>) asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT);
                Iterator<TraceContext> iter = traceContextList.iterator();
                while(iter.hasNext()) {
                    TraceContext ctx = iter.next();
                    TraceMain.endHttpServiceFinal(ctx, asyncEvent.getSuppliedRequest(), asyncEvent.getSuppliedResponse(), ctx.asyncThrowable);
                    TraceContextManager.completeDeferred(ctx);
                }
            }

            @Override
            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
//                System.out.println("[scouter][asynccontext]onTimeout:count:" + asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH) + " => " + this.toString());
//                System.out.println("[scouter][asynccontext]onTimeout:thread: " + Thread.currentThread().getName());

                List<TraceContext> traceContextList = (List<TraceContext>) asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT);
                Iterator<TraceContext> iter = traceContextList.iterator();
                while(iter.hasNext()) {
                    TraceContext ctx = iter.next();
                    ctx.asyncThrowable = new ASYNC_SERVLET_TIMEOUT("exceed async servlet timeout! : " + asyncEvent.getAsyncContext().getTimeout() + "ms");
                }
            }

            @Override
            public void onError(AsyncEvent asyncEvent) throws IOException {
//                System.out.println("[scouter][asynccontext]onError:count:" + asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH) + " => " + this.toString());
//                System.out.println("[scouter][asynccontext]onError:thread: " + Thread.currentThread().getName());

                List<TraceContext> traceContextList = (List<TraceContext>) asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ALL_DISPATCHED_TRACE_CONTEXT);
                Iterator<TraceContext> iter = traceContextList.iterator();
                while(iter.hasNext()) {
                    TraceContext ctx = iter.next();
                    ctx.asyncThrowable = asyncEvent.getThrowable();
                }

            }

            @Override
            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
//                System.out.println("[scouter][asynccontext]onStartAsync:count:" + asyncEvent.getSuppliedRequest().getAttribute(REQUEST_ATTRIBUTE_ASYNC_DISPATCH) + " => " + this.toString());
//                System.out.println("[scouter][asynccontext]onStartAsync:thread: " + Thread.currentThread().getName());
            }
        });
    }

    public TraceContext getTraceContextFromAsyncContext(Object oAsyncContext) {
        AsyncContext asyncContext = (AsyncContext) oAsyncContext;
        TraceContext currentTraceContext = (TraceContext)asyncContext.getRequest().getAttribute(REQUEST_ATTRIBUTE_TRACE_CONTEXT);
        return currentTraceContext;
    }

    public void setDispatchTransferMap(Object oAsyncContext, long gxid, long caller, long callee, byte xType) {
        AsyncContext asyncContext = (AsyncContext) oAsyncContext;
        asyncContext.getRequest().setAttribute(REQUEST_ATTRIBUTE_CALLER_TRANSFER_MAP, new TransferMap.ID(gxid, caller, callee, xType));
    }

    public void setSelfDispatch(Object oAsyncContext, boolean self) {
        AsyncContext asyncContext = (AsyncContext) oAsyncContext;
        asyncContext.getRequest().setAttribute(REQUEST_ATTRIBUTE_SELF_DISPATCHED, self);
    }

    public boolean isSelfDispatch(Object oAsyncContext) {
        AsyncContext asyncContext = (AsyncContext) oAsyncContext;
        return Boolean.TRUE.equals(asyncContext.getRequest().getAttribute(REQUEST_ATTRIBUTE_SELF_DISPATCHED));
    }
}
