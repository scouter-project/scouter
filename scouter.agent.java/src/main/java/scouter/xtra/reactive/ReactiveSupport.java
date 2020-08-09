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

package scouter.xtra.reactive;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;
import kotlinx.coroutines.ThreadContextElementKt;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.ScouterOptimizableOperatorProxy;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;
import scouter.agent.AgentCommonConstant;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IReactiveSupport;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.ParameterizedMessageStep;

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
            }).doOnNext(new Consumer<Object>() {
                @Override
                public void accept(Object o) {
                }
            }).doFirst(new Runnable() {
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
            Hooks.onEachOperator(AgentCommonConstant.TRACE_ID, Operators.lift(
                    new BiFunction<Scannable, CoreSubscriber<? super Object>, CoreSubscriber<? super Object>>() {
                @Override
                public CoreSubscriber<? super Object> apply(Scannable scannable, CoreSubscriber<? super Object> subscriber) {
                    if (scannable instanceof Fuseable.ScalarCallable) {
                        return subscriber;
                    }
                    return new TxidLifter(subscriber, scannable, null);
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

    public static class TxidLifter<T> implements SpanSubscription<T>, Scannable {

        private final CoreSubscriber<T> coreSubscriber;
        private final Scannable scannable;
        private final Publisher publisher;
        private Subscription orgSubs;

        private enum ReactorCheckPointType {
            ON_SUBSCRIBE,
            ON_COMPLETE,
            ON_ERROR,
            ON_CANCEL
        }

        public TxidLifter(CoreSubscriber<T> coreSubscriber, Scannable scannable, Publisher publisher) {
            this.coreSubscriber = coreSubscriber;
            this.scannable = scannable;
            this.publisher = publisher;
        }

        @Override
        public void onSubscribe(Subscription subs) {
            try {
                TraceContext traceContext = getTraceContext(scannable, coreSubscriber.currentContext());
                if (traceContext != null) {
                    traceContext.scannables.put(scannable.hashCode(),
                            new TraceContext.TimedScannable(System.currentTimeMillis(), scannable));
                    profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_SUBSCRIBE, null);
                }
            } catch (Exception e) {
                Logger.println("[R109]", "reactive support onSubscribe error.", e);
            }
            this.orgSubs = subs;
            coreSubscriber.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            copyToThread(coreSubscriber.currentContext());
            coreSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                TraceContext traceContext = getTraceContext(scannable, coreSubscriber.currentContext());
                if (traceContext != null) {
                    TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                    profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_ERROR, timedScannable);
                }
            } catch (Exception e) {
                Logger.println("[R110]", "reactive support onError error.", e);
            }
            coreSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            try {
                TraceContext traceContext = getTraceContext(scannable, coreSubscriber.currentContext());
                if (traceContext != null) {
                    TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                    profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_COMPLETE, timedScannable);
                }
            } catch (Exception e) {
                Logger.println("[R111]", "reactive support onComplete error.", e);
            }
            coreSubscriber.onComplete();
        }

        @Override
        public void request(long n) {
            this.orgSubs.request(n);
        }

        @Override
        public void cancel() {
            try {
                TraceContext traceContext = getTraceContext(scannable, coreSubscriber.currentContext());
                if (traceContext != null) {
                    TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                    profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_CANCEL, timedScannable);
                }
            } catch (Exception e) {
                Logger.println("[R112]", "reactive support onCancel error.", e);
            }
            this.orgSubs.cancel();
        }

        @Override
        public Context currentContext() {
            return coreSubscriber.currentContext();
        }

        @Override
        public Object scanUnsafe(Attr key) {
            if (key == Attr.PARENT) {
                return this.orgSubs;
            }
            else {
                return key == Attr.ACTUAL ? this.coreSubscriber : null;
            }
        }

        private void copyToThread(Context context) {
            if (context != null && !context.isEmpty()) {
                Long txid = context.getOrDefault(AgentCommonConstant.TRACE_ID,  null);
                if (txid != null) {
                    TraceContextManager.setTxidLocal(txid);
                } else {
                    Logger.println("R113", "copy to thread of txid is null, thread : " + Thread.currentThread().getName());
                }
            } else {
                TraceContextManager.setTxidLocal(null);
            }
        }

        private TraceContext getTraceContext(Scannable scannable, Context currentContext) {
            if (scannable == null || currentContext == null) {
                return null;
            }
            TraceContext traceContext;
            Long txid = currentContext.getOrDefault(AgentCommonConstant.TRACE_ID, null);
            if (txid == null) {
                traceContext = TraceContextManager.getContext();
            } else {
                traceContext = TraceContextManager.getContextByTxid(txid);
            }
            return traceContext;
        }

        private void profileCheckPoint(Scannable scannable, TraceContext traceContext, ReactorCheckPointType type,
                                       TraceContext.TimedScannable timedScannable) {

            if (scannable.isScanAvailable()) {
                String checkpointDesc = ScouterOptimizableOperatorProxy.nameOnCheckpoint(scannable);
                if (!"".equals(checkpointDesc)) {
                    String duration;
                    StringBuilder messageBuilder = new StringBuilder(300)
                            .append("[")
                            .append(type.name());

                    if (timedScannable != null) {
                        messageBuilder.append("(%sms): ");
                        duration = String.valueOf(System.currentTimeMillis() - timedScannable.start);
                    } else {
                        messageBuilder.append(": ");
                        duration = "";
                    }

                    String message = messageBuilder.append(scannable.name())
                            .append("] ")
                            .append(checkpointDesc).toString();

                    ParameterizedMessageStep step = new ParameterizedMessageStep();
                    step.setMessage(DataProxy.sendHashedMessage(message), duration);
                    step.start_time = (int) (System.currentTimeMillis() - traceContext.startTime);

                    if (checkpointDesc.startsWith("checkpoint")) {
                        step.setLevel(ParameterizedMessageLevel.INFO);
                    } else {
                        step.setLevel(ParameterizedMessageLevel.DEBUG);
                    }
                    traceContext.profile.add(step);
                }
            }
        }
    }

    public String dumpScannable(TraceContext traceContext, TraceContext.TimedScannable timedScannable, long now) {

        if (traceContext == null || timedScannable == null) {
            return null;
        }
        Scannable scannable = (Scannable) timedScannable.scannable;
        long duration = now - timedScannable.start;
        StringBuilder builder = new StringBuilder(1000)
                .append(scannable.name()).append(" ").append(duration).append("ms");

        ScouterOptimizableOperatorProxy.appendSources4Dump(scannable, builder);
        return builder.toString();
    }
}
