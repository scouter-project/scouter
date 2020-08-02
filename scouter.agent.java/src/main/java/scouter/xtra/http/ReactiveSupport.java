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

package scouter.xtra.http;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;
import kotlinx.coroutines.ThreadContextElementKt;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;
import scouter.agent.AgentCommonConstant;
import scouter.agent.proxy.IReactiveSupport;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactiveSupport implements IReactiveSupport {

    @Override
    public Object subscriptOnContext(Object mono0, final TraceContext traceContext) {
        try {
            if (traceContext.isReactiveTxidMarked) {
                return mono0;
            }
            Mono<?> mono = (Mono<?>) mono0;
            return mono.subscriberContext(new Function<Context, Context>() {
                @Override
                public Context apply(Context context) {
                    traceContext.isReactiveTxidMarked = true;
                    return context.put(AgentCommonConstant.TRACE_ID, traceContext.txid);
                }
            }).doOnSuccess(new Consumer<Object>() {
                @Override
                public void accept(Object o) {
                    TraceMain.endHttpService(new TraceMain.Stat(traceContext), null);
                }
            }).doOnError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    TraceMain.endHttpService(new TraceMain.Stat(traceContext), throwable);
                }
            }).doOnCancel(new Runnable() {
                @Override
                public void run() {
                    TraceMain.endCanceledHttpService(traceContext);
                }
            }).doFinally(new Consumer<SignalType>() {
                @Override
                public void accept(SignalType signalType) {
                    TraceContextManager.clearAllContext(traceContext);
                }
            }).doAfterTerminate(new Runnable() {
                @Override
                public void run() {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return mono0;
        }
    }

    @Override
    public void contextOperatorHook() {
        try {
            Hooks.onEachOperator(AgentCommonConstant.TRACE_ID, Operators.lift(new BiFunction<Scannable, CoreSubscriber<? super Object>,
                    CoreSubscriber<? super Object>>() {
                @Override
                public CoreSubscriber<? super Object> apply(Scannable s, CoreSubscriber<? super Object> subscriber) {
                    return new TxidLifter(subscriber);
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object monoCoroutineContextHook(Object _coroutineContext, TraceContext traceContext) {
        CoroutineContext coroutineContext = (CoroutineContext) _coroutineContext;

        TraceContextManager.startByCoroutine(traceContext);

        ThreadContextElement<Long> threadContextElement = ThreadContextElementKt
                .asContextElement(TraceContextManager.txidByCoroutine, traceContext.txid);
        return coroutineContext.plus(threadContextElement);
    }

    public static class TxidLifter<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<T> coreSubscriber;

        public TxidLifter(CoreSubscriber<T> coreSubscriber) {
            this.coreSubscriber = coreSubscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            coreSubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            copyToThread(coreSubscriber.currentContext());
            coreSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            coreSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            coreSubscriber.onComplete();
        }

        @Override
        public Context currentContext() {
            return coreSubscriber.currentContext();
        }

        void copyToThread(Context context) {
            if (context != null && !context.isEmpty()) {
                Long txid = context.getOrDefault(AgentCommonConstant.TRACE_ID,  null);
                if (txid != null) {
                    TraceContextManager.setTxidLocal(txid);
                } else {
                    System.out.println("!!! copy to thread of txid is null, thread : " + Thread.currentThread().getName());
                }
            } else {
                TraceContextManager.setTxidLocal(null);
            }
        }
    }
}
