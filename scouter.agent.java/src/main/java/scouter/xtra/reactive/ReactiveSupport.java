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
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IReactiveSupport;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.agent.util.Tuple;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.util.StringUtil;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactiveSupport implements IReactiveSupport {

    static Configure configure = Configure.getInstance();
    private Method subscriberContextMethod;
    private static Method isCheckpoint;
    private static boolean isReactor34;

    public ReactiveSupport() {
        isReactor34 = ReactiveSupportUtils.isSupportReactor34();
        try {
            if (isReactor34) {
                subscriberContextMethod = Mono.class.getMethod("contextWrite", Function.class);
                Class<?> assemblySnapshotClass = Class.forName("reactor.core.publisher.FluxOnAssembly$AssemblySnapshot");
                isCheckpoint = assemblySnapshotClass.getDeclaredMethod("isCheckpoint");
                isCheckpoint.setAccessible(true);
            } else {
                subscriberContextMethod = Mono.class.getMethod("subscriberContext", Function.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object subscriptOnContext(Object mono0, final TraceContext traceContext) {
        try {
            if (traceContext.isReactiveTxidMarked) {
                return mono0;
            }
            Mono<?> mono = (Mono<?>) mono0;
            traceContext.isReactiveTxidMarked = true;

            Mono<?> monoChain;
            Function<Context, Context> func = new Function<Context, Context>() {
                @Override
                public Context apply(Context context) {
                    return context.put(TraceContext.class, traceContext);
                }
            };

            monoChain = (Mono<?>) subscriberContextMethod.invoke(mono, func);
            return monoChain.doOnSuccess(new Consumer<Object>() {
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
        } catch (Throwable e) {
            Logger.println("R201", e.getMessage(), e);
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
                    try {
                        if (scannable instanceof Fuseable.ScalarCallable) {
                            return subscriber;
                        }
                        Context context = subscriber.currentContext();
                        TraceContext traceContext = getTraceContext(scannable, context);

                        if (traceContext != null) {
                            return new TxidLifter(subscriber, scannable, null, traceContext);
                        } else {
                            return subscriber;
                        }
                    } catch (Exception e) {
                        Logger.println("R1660", e.getMessage(), e);
                        return subscriber;
                    }
                }
            }));
        } catch (Throwable e) {
            Logger.println("R166", e.getMessage(), e);
        }
    }

    private TraceContext getTraceContext(Scannable scannable, Context currentContext) {
        if (scannable == null || currentContext == null) {
            return null;
        }
        return currentContext.getOrDefault(TraceContext.class, null);
    }

    @Override
    public Object monoCoroutineContextHook(Object _coroutineContext, TraceContext traceContext) {
        return _coroutineContext;
    }

    public static class SubscribeDepth {}
    public static class TxidLifter<T> implements SpanSubscription<T>, Scannable {

        private final CoreSubscriber<T> coreSubscriber;
        private final Context ctx;
        private final Scannable scannable;
        private final Publisher publisher;
        private final TraceContext traceContext;
        private final String checkpointDesc;
        private final Integer depth;
        private Subscription orgSubs;

        private enum ReactorCheckPointType {
            ON_SUBSCRIBE,
            ON_COMPLETE,
            ON_ERROR,
            ON_CANCEL
        }

        public TxidLifter(CoreSubscriber<T> coreSubscriber, Scannable scannable, Publisher publisher,
                          TraceContext traceContext) {
            this.coreSubscriber = coreSubscriber;
            Context context = coreSubscriber.currentContext();
            this.scannable = scannable;
            this.publisher = publisher;
            this.traceContext = traceContext;

            Tuple.StringLongPair checkpointPair = ScouterOptimizableOperatorProxy
                    .nameOnCheckpoint(scannable, configure.profile_reactor_checkpoint_search_depth, isReactor34, isCheckpoint);
            checkpointDesc = checkpointPair.aString;

            Integer parentDepth = context.getOrDefault(SubscribeDepth.class, 0);
            depth = (!"".equals(checkpointDesc)) ? parentDepth + 1 : parentDepth;
            this.ctx = context.put(SubscribeDepth.class, depth);

            //todo parent something
//            this.ctx = parent != null
//                    && !parent.equals(ctx.getOrDefault(TraceContext.class, null))
//                    ? ctx.put(TraceContext.class, parent) : ctx;
        }

        @Override
        public void onSubscribe(Subscription subs) {
            copyToThread(currentContext(), traceContext);
            try {
                traceContext.scannables.put(scannable.hashCode(),
                        new TraceContext.TimedScannable(System.currentTimeMillis(), scannable));
                profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_SUBSCRIBE, null);
            } catch (Throwable e) {
                Logger.println("[R109]", "reactive support onSubscribe error.", e);
            }
            this.orgSubs = subs;
            coreSubscriber.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            copyToThread(currentContext(), traceContext);
            coreSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            copyToThread(currentContext(), traceContext);
            try {
                TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_ERROR, timedScannable);
            } catch (Throwable e) {
                Logger.println("[R110]", "reactive support onError error.", e);
            }
            coreSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            copyToThread(currentContext(), traceContext);
            try {
                TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_COMPLETE, timedScannable);
            } catch (Throwable e) {
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
            copyToThread(currentContext(), traceContext);
            try {
                TraceContext.TimedScannable timedScannable = traceContext.scannables.remove(scannable.hashCode());
                profileCheckPoint(scannable, traceContext, ReactorCheckPointType.ON_CANCEL, timedScannable);
            } catch (Throwable e) {
                Logger.println("[R112]", "reactive support onCancel error.", e);
            }
            this.orgSubs.cancel();
        }

        @Override
        public Context currentContext() {
            return ctx;
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

        private void copyToThread(Context context, TraceContext traceContext) {
            Long threadLocalTxid = TraceContextManager.getLocalTxid();
            if (threadLocalTxid == null) {
                TraceContextManager.setTxidLocal(traceContext.txid);
            } else if (threadLocalTxid != traceContext.txid) {
                TraceContextManager.setTxidLocal(traceContext.txid);
            }
        }

        private void profileCheckPoint(Scannable scannable, TraceContext traceContext, ReactorCheckPointType type,
                                       TraceContext.TimedScannable timedScannable) {
            if (!configure.profile_reactor_checkpoint_enabled) {
                return;
            }
            if (scannable.isScanAvailable()) {
                if (!"".equals(checkpointDesc)) {
                    boolean important = false;
                    if (checkpointDesc.startsWith("checkpoint")) {
                        important = true;
                    }
                    if (!configure.profile_reactor_more_checkpoint_enabled && !important) {
                        return;
                    }
                    String duration;
                    StringBuilder messageBuilder = new StringBuilder(300)
                            .append(StringUtil.padding((depth - 1) * 2, ' '))
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
                            .append("] near-cp -> ")
                            .append(checkpointDesc).toString();

                    ParameterizedMessageStep step = new ParameterizedMessageStep();
                    step.setMessage(DataProxy.sendHashedMessage(message), duration);
                    step.start_time = (int) (System.currentTimeMillis() - traceContext.startTime);

                    if (important) {
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

        ScouterOptimizableOperatorProxy.appendSources4Dump(scannable, builder, configure.profile_reactor_checkpoint_search_depth);
        return builder.toString();
    }

    @Override
    public boolean isReactor34() {
        return isReactor34;
    }
}
