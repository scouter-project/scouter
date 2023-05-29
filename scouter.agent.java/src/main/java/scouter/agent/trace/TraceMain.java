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
package scouter.agent.trace;

import scouter.agent.AgentCommonConstant;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.asm.UserExceptionHandlerASM;
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.agent.counter.meter.MeterService;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.error.REQUEST_REJECT;
import scouter.agent.error.RESULTSET_LEAK_SUSPECT;
import scouter.agent.error.STATEMENT_LEAK_SUSPECT;
import scouter.agent.error.USERTX_NOT_CLOSE;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.AbstractPlugin;
import scouter.agent.plugin.PluginAppServiceTrace;
import scouter.agent.plugin.PluginBackThreadTrace;
import scouter.agent.plugin.PluginCaptureTrace;
import scouter.agent.plugin.PluginHttpServiceTrace;
import scouter.agent.plugin.PluginSpringControllerCaptureTrace;
import scouter.agent.proxy.HttpTraceFactory;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.proxy.IKafkaTracer;
import scouter.agent.proxy.ILettuceTrace;
import scouter.agent.proxy.IReactiveSupport;
import scouter.agent.proxy.IRedissonTrace;
import scouter.agent.proxy.KafkaTraceFactory;
import scouter.agent.proxy.LettuceTraceFactory;
import scouter.agent.proxy.ReactiveSupportFactory;
import scouter.agent.proxy.RedissonTraceFactory;
import scouter.agent.summary.ServiceSummary;
import scouter.agent.wrapper.async.WrTask;
import scouter.agent.wrapper.async.WrTaskCallable;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.DroppedXLogPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodStep2;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.lang.value.MapValue;
import scouter.util.ArrayUtil;
import scouter.util.ByteArrayKeyLinkedMap;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.KeyGen;
import scouter.util.ObjectUtil;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static scouter.lang.pack.XLogDiscardTypes.XLogDiscard;

public class TraceMain {

    public static final String[] EMPTY_PARAM = new String[0];

    public static class Stat {
        public TraceContext ctx;
        public Object req;
        public Object res;
        public boolean isStaticContents;

        public Stat(TraceContext ctx, Object req, Object res) {
            this.ctx = ctx;
            this.req = req;
            this.res = res;
        }

        public Stat(TraceContext ctx) {
            this.ctx = ctx;
        }
    }

    public static IHttpTrace http = null;
    public static IHttpTrace reactiveHttp = null;
    public static IReactiveSupport reactiveSupport = null;

    private static Configure conf = Configure.getInstance();
    private static Error REJECT = new REQUEST_REJECT("service rejected");
    private static Error userTxNotClose = new USERTX_NOT_CLOSE("UserTransaction missing commit/rollback Error");
    private static Error resultSetLeakSuspect = new RESULTSET_LEAK_SUSPECT("ResultSet Leak suspected!");
    private static Error statementLeakSuspect = new STATEMENT_LEAK_SUSPECT("Statement Leak suspected!");
    private static DelayedServiceManager delayedServiceManager = DelayedServiceManager.getInstance();
    public static ILoadController plController;

    public static Object startHttpService(Object req, Object res) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx != null) {
                return null;
            }
            if (TraceContextManager.startForceDiscard()) {
                return null;
            }
            return startHttp(req, res);
        } catch (Throwable t) {
            Logger.println("A143", "fail to deploy ", t);
        }
        return null;
    }

    public static Object startHttpFilter(Object req, Object res) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx != null) {
                return null;
            }
            if (TraceContextManager.startForceDiscard()) {
                return null;
            }
            return startHttp(req, res);
        } catch (Throwable t) {
            Logger.println("A144", "fail to deploy ", t);
        }
        return null;
    }

    public static void startReactiveInit(Object obj) {
    }

    public static void startReactiveHttpService(Object exchange) {
        try {
            if (reactiveSupport == null) {
                initReactiveSupport(exchange);
            }
        } catch (Throwable t) {
            Logger.println("A143", "fail to deploy ", t);
        }
        try {
            Object req = AbstractPlugin.invokeMethod(exchange, "getRequest");
            Object res = AbstractPlugin.invokeMethod(exchange, "getResponse");
            if (reactiveHttp == null) {
                initReactiveHttp(req);
            }

            String serverReqId = reactiveHttp.getRequestId(req);
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx != null && serverReqId != null && ctx.serverReqId == serverReqId) {
                return;
            }
            if (ctx != null && ctx.exchangeHashCode != exchange.hashCode()) {
                //Logger.trace("exchange hash is different on context : " + exchange.hashCode() + " : " + ctx.exchangeHashCode);
                ctx = null;
            }
            if (ctx != null) {
                return;
            }
            if (TraceContextManager.startForceDiscard()) {
                return;
            }
            startReactiveHttp(req, res, exchange);
        } catch (Throwable t) {
            Logger.println("A143", "fail to deploy ", t);
        }
    }

    public static Object startReactiveHttpServiceReturn(Object mono) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null) {
            return mono;
        }
        if (!ctx.isReactiveStarted) {
            return mono;
        }
        if (reactiveSupport == null) {
            return mono;
        }
        return reactiveSupport.subscriptOnContext(mono, ctx);
    }

    public static Object reject(Object stat, Object req, Object res) {
        Configure conf = Configure.getInstance();
        if (plController != null) {
            if (stat == null || req == null || res == null)
                return null;
            if (http == null) {
                initHttp(req);
            }
            Stat stat0 = (Stat) stat;
            if (stat0.isStaticContents) {
                return null;
            }
            if (plController.reject(stat0.ctx, req, res, http)) {
                endHttpService(stat0, REJECT);
                return REJECT;
            }
        }
        if (conf.control_reject_service_enabled) {
            if (stat == null || req == null || res == null)
                return null;
            if (http == null) {
                initHttp(req);
            }
            Stat stat0 = (Stat) stat;
            if (stat0.isStaticContents)
                return null;


            // reject by customized plugin
            if (PluginHttpServiceTrace.reject(stat0.ctx, req, res)
                    // reject by control_reject_service_max_count
                    || TraceContextManager.size() > conf.control_reject_service_max_count) {
                // howto reject
                if (conf.control_reject_redirect_url_enabled) {
                    http.rejectUrl(res, conf.control_reject_redirect_url); // by url
                } else {
                    http.rejectText(res, conf.control_reject_text);// just message
                }
                // close transaction
                endHttpService(stat0, REJECT);
                return REJECT;
            }
        }
        return null;
    }

    private static void addHttpServiceName(TraceContext ctx, Object req) {
        try {
            Configure conf = Configure.getInstance();

            StringBuilder sb = new StringBuilder();
            if (conf.trace_service_name_post_key != null) {
                String v = ctx.http.getParameter(req, conf.trace_service_name_post_key);
                if (v != null) {
                    if (sb.length() == 0) {
                        sb.append(ctx.serviceName);
                        sb.append('?').append(conf.trace_service_name_post_key).append("=").append(v);
                    } else {
                        sb.append('&').append(conf.trace_service_name_post_key).append("=").append(v);
                    }
                }
            }
            if (conf.trace_service_name_get_key != null && ctx.http_query != null) {
                int x = ctx.http_query.indexOf(conf.trace_service_name_get_key);
                if (x >= 0) {
                    String v = null;
                    int y = ctx.http_query.indexOf('&', x + 1);
                    if (y > x) {
                        v = ctx.http_query.substring(x, y);
                    } else {
                        v = ctx.http_query.substring(x);
                    }
                    if (sb.length() == 0) {
                        sb.append(ctx.serviceName);
                        sb.append('?').append(v);
                    } else {
                        sb.append('&').append(v);
                    }
                }
            }
            if (conf.trace_service_name_header_key != null) {
                String v = ctx.http.getHeader(req, conf.trace_service_name_header_key);
                ctx.serviceName = new StringBuilder(ctx.serviceName.length() + v.length() + 5).append(ctx.serviceName)
                        .append('-').append(v).toString();
            }
            if (sb.length() > 0) {
                ctx.serviceName = sb.toString();
            }
        } catch (Throwable t) {
        }
    }

    private static Object lock = new Object();

    private static Object startReactiveHttp(Object req, Object res, Object exchange) {
        return startHttp(req, res, reactiveHttp, true, exchange);
    }

    private static Object startHttp(Object req, Object res) {
        if (http == null) {
            initHttp(req);
        }
        return startHttp(req, res, http, false, null);
    }

    private static Object startHttp(Object req, Object res, IHttpTrace http0, boolean isReactive, Object exchange) {
        Configure conf = Configure.getInstance();
        TraceContext ctx = new TraceContext(false);
        if (isReactive) {
            ctx.initScannables();
            ctx.isReactiveStarted = true;
            ctx.exchangeHashCode = exchange.hashCode();
            ctx.serverReqId = reactiveHttp.getRequestId(req);
        }
        ctx.thread = Thread.currentThread();
        ctx.threadId = ctx.thread.getId();

        ctx.txid = KeyGen.next();
        ctx.startTime = System.currentTimeMillis();
        ctx.bytes = SysJMX.getCurrentThreadAllocBytes(conf.profile_thread_memory_usage_enabled);
        ctx.profile_thread_cputime = conf.profile_thread_cputime_enabled;
        if (ctx.profile_thread_cputime) {
            ctx.startCpu = SysJMX.getCurrentThreadCPU();
        }

        HashedMessageStep step = new HashedMessageStep();
        step.time = -1;
        ctx.threadName = ctx.thread.getName();
        if (StringUtil.isEmpty(ctx.threadName)) {
            ctx.threadName = ctx.thread.toString();
        }
        step.hash = DataProxy.sendHashedMessage("[driving thread] " + ctx.threadName);
        ctx.profile.add(step);

        http0.start(ctx, req, res);
        ctx._req = req;
        ctx._res = res;
        ctx.http = http0;

        if (ctx.isFullyDiscardService) {
            return null;
        }

        if (ctx.serviceName == null) {
            ctx.serviceName = "Non-URI";
        }

        TraceContextManager.start(ctx);

        Stat stat = new Stat(ctx, req, res);
        stat.isStaticContents = ctx.isStaticContents;

        if (!stat.isStaticContents) {
            if (ctx.xType != XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE) {
                PluginHttpServiceTrace.start(ctx, req, res, http0, isReactive);
            }

            if (plController != null) {
                plController.start(ctx, req, res);
            }
        }
        return stat;
    }

    private static void initHttp(Object req) {
        synchronized (lock) {
            if (http == null) {
                http = HttpTraceFactory.create(req.getClass().getClassLoader(), req);
            }
        }
    }

    private static void initReactiveHttp(Object req) {
        synchronized (lock) {
            if (reactiveHttp == null) {
                reactiveHttp = HttpTraceFactory.create(req.getClass().getClassLoader(), req);
            }
        }
    }

    private static void initReactiveSupport(Object obj) {
        synchronized (lock) {
            if (reactiveSupport == null) {
                reactiveSupport = ReactiveSupportFactory.create(obj.getClass().getClassLoader());
                reactiveSupport.contextOperatorHook();
            }
        }
    }

    public static void endReactiveHttpService() {
        TraceContext context = TraceContextManager.getContext(true);
        if (context == null) {
            return;
        }
        Stat stat = new Stat(context, context._req, context._res);
        endHttpService(stat, null);
    }

    public static void endCanceledHttpService(TraceContext traceContext) {
        if (traceContext != null && !traceContext.endHttpProcessingStarted) {
//            traceContext.error += 1;

            ParameterizedMessageStep step = new ParameterizedMessageStep();
            step.setMessage(DataProxy.sendHashedMessage("reactive stream canceled finish"), EMPTY_PARAM);
            step.setLevel(ParameterizedMessageLevel.INFO);
            step.start_time = (int) (System.currentTimeMillis() - traceContext.startTime);
            traceContext.profile.add(step);

            endHttpService(new Stat(traceContext, traceContext._req, traceContext._res), null);
        }
    }

    public static void endHttpService(Object stat, Throwable thr) {
        if (TraceContextManager.isForceDiscarded()) {
            TraceContextManager.clearForceDiscard();
            return;
        }
        try {
            Stat stat0 = (Stat) stat;
            if (stat0 == null) { // means already started on another previous method, so should do end job there.
                endHttpProcessingIfStatNull(thr);
                return;
            }
            TraceContext ctx = stat0.ctx;
            if (ctx == null) {
                return;
            }

            //wait on async servlet completion
            if (!ctx.asyncServletStarted) {
                Object req = ctx._req;
                Object res = ctx._res;
                ctx._req = null;
                ctx._res = null;
                endHttpServiceFinal(ctx, req, res, thr);
            } else {
                HashedMessageStep step = new HashedMessageStep();
                step.time = -1;
                step.hash = DataProxy.sendHashedMessage("end servlet and wait async complete");
                step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                ctx.profile.add(step);
                flushErrorSummary(ctx);
                TraceContextManager.end(ctx);
                ctx.latestCpu = SysJMX.getCurrentThreadCPU();
                ctx.latestBytes = SysJMX.getCurrentThreadAllocBytes(conf.profile_thread_memory_usage_enabled);
                TraceContextManager.toDeferred(ctx);
            }
        } catch (Throwable throwable) {
            Logger.println("A180", throwable.getMessage(), throwable);
        }
    }

    public static void endHttpServiceFinal(TraceContext ctx, Object request, Object response, Throwable thr) {
        if (TraceContextManager.isForceDiscarded()) {
            TraceContextManager.clearForceDiscard();
            return;
        }

        //prevent duplication invoke
        synchronized (ctx) {
            if (ctx.endHttpProcessingStarted) {
                Logger.println("[info] duplicated endHttpServiceFinal() called.: " + ctx.serviceName);
                return;
            }
            ctx.endHttpProcessingStarted = true;
        }

        try {
            if (conf.getEndUserPerfEndpointHash() == ctx.serviceHash) {
                TraceContextManager.end(ctx);
                return;
            }
            //additional service name
            addHttpServiceName(ctx, request);
            // add error summary
            flushErrorSummary(ctx);
            // HTTP END
            ctx.http.end(ctx, request, response);
            // static-contents -> stop processing
            if (ctx.isStaticContents) {
                TraceContextManager.end(ctx);
                return;
            }
            // Plug-in end
            if (ctx.xType != XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE) {
                PluginHttpServiceTrace.end(ctx, request, response, ctx.http, ctx.isReactiveStarted);
            }
            if (plController != null) {
                plController.end(ctx, request, response);
            }
            //profile rs
            if (conf.trace_rs_leak_enabled && ctx.unclosedRsMap.size() > 0) {
                MapValue mv = new MapValue();
                mv.put(AlertPack.HASH_FLAG + TextTypes.SERVICE + "_service-name", ctx.serviceHash);

                if (conf.profile_fullstack_rs_leak_enabled) {
                    String message = ctx.unclosedRsMap.values().nextElement();
                    if (message != null) {
                        message = "ResultSet Leak suspected!\n" + message;
                        HashedMessageStep step = new HashedMessageStep();
                        step.hash = DataProxy.sendHashedMessage(message);
                        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                        ctx.profile.add(step);
                        mv.put(AlertPack.HASH_FLAG + TextTypes.HASH_MSG + "_full-stack", step.hash);
                    }
                }
                DataProxy.sendAlert(AlertLevel.WARN, "RESULTSET_LEAK_SUSPECT", "ResultSet Leak suspected!", mv);
            }

            //profile stmt
            if (conf.trace_stmt_leak_enabled && ctx.unclosedStmtMap.size() > 0) {
                MapValue mv = new MapValue();
                mv.put(AlertPack.HASH_FLAG + TextTypes.SERVICE + "_service-name", ctx.serviceHash);

                if (conf.profile_fullstack_stmt_leak_enabled) {
                    String message = ctx.unclosedStmtMap.values().nextElement();
                    if (message != null) {
                        message = "Statement Leak suspected!\n" + message;
                        HashedMessageStep step = new HashedMessageStep();
                        step.hash = DataProxy.sendHashedMessage(message);
                        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                        ctx.profile.add(step);
                        mv.put(AlertPack.HASH_FLAG + TextTypes.HASH_MSG + "_full-stack", step.hash);
                    }
                }
                DataProxy.sendAlert(AlertLevel.WARN, "STATEMENT_LEAK_SUSPECT", "Statement Leak suspected!", mv);
            }

            // profile close
            TraceContextManager.end(ctx);

            Configure conf = Configure.getInstance();
            XLogPack pack = new XLogPack();
            // pack.endTime = System.currentTimeMillis();
            pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
            ctx.serviceHash = DataProxy.sendServiceName(ctx.serviceName);
            pack.service = ctx.serviceHash;
            pack.threadNameHash = DataProxy.sendHashedMessage(ctx.threadName);

            pack.xType = ctx.xType; //default 0 : XLogType.WEB_SERVICE
            pack.txid = ctx.txid;
            pack.gxid = ctx.gxid;
            pack.caller = ctx.caller;

            pack.status = ctx.status;
            pack.sqlCount = ctx.sqlCount;
            pack.sqlTime = ctx.sqlTime;
            pack.ipaddr = IPUtil.toBytes(ctx.remoteIp);
            pack.userid = ctx.userid;

            if (ctx.hasDumpStack) {
                pack.hasDump = 1;
            } else {
                pack.hasDump = 0;
            }
            if (ctx.error != 0) {
                pack.error = ctx.error;

            } else if (thr != null) {
                if (thr == REJECT) {
                    Logger.println("A145", ctx.serviceName);
                    String emsg = conf.control_reject_text;
                    pack.error = DataProxy.sendError(emsg);
                    ServiceSummary.getInstance().process(thr, pack.error, ctx.serviceHash, ctx.txid, 0, 0);

                } else {
                    String classHierarchyConcatString = buildClassHierarchyConcatString(thr.getClass());
                    String[] excludes = UserExceptionHandlerASM.exceptionExcludeClasseNames;
                    boolean ignore = false;
                    for (int i = 0; i < excludes.length; i++) {
                        if (classHierarchyConcatString.contains(excludes[i])) {
                            ignore = true;
                        }
                    }

                    if (!ignore) {
                        String emsg = thr.toString();
                        if (conf.profile_fullstack_service_error_enabled) {
                            StringBuffer sb = new StringBuffer();
                            sb.append(emsg).append("\n");
                            ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                            Throwable[] suppressed = thr.getSuppressed();
                            if (suppressed != null) {
                                for (Throwable sup : suppressed) {
                                    sb.append("\nSuppressed...\n");
                                    sb.append(sup.toString()).append("\n");
                                    ThreadUtil.getStackTrace(sb, sup, conf.profile_fullstack_max_lines);
                                }
                            }

                            Throwable thrCause = thr.getCause();
                            if (thrCause != null) {
                                thr = thrCause;
                                while (thr != null) {
                                    sb.append("\nCause...\n");
                                    ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                                    Throwable[] suppressed2 = thr.getSuppressed();
                                    if (suppressed2 != null) {
                                        for (Throwable sup : suppressed2) {
                                            sb.append("\nSuppressed...\n");
                                            sb.append(sup.toString()).append("\n");
                                            ThreadUtil.getStackTrace(sb, sup, conf.profile_fullstack_max_lines);
                                        }
                                    }

                                    thr = thr.getCause();
                                }
                            }
                            emsg = sb.toString();
                        }
                        pack.error = DataProxy.sendError(emsg);
                        ServiceSummary.getInstance().process(thr, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
                    }
                }
            } else if (ctx.userTransaction > 0 && conf.xlog_error_check_user_transaction_enabled) {
                pack.error = DataProxy.sendError("UserTransaction missing commit/rollback Error");
                ServiceSummary.getInstance().process(userTxNotClose, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
            } else if (conf.trace_rs_leak_enabled && ctx.unclosedRsMap.size() > 0) {
                pack.error = DataProxy.sendError("ResultSet Leak suspected!");
                ServiceSummary.getInstance().process(resultSetLeakSuspect, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
            } else if (conf.trace_stmt_leak_enabled && ctx.unclosedStmtMap.size() > 0) {
                pack.error = DataProxy.sendError("Statement Leak suspected!");
                ServiceSummary.getInstance().process(statementLeakSuspect, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
            }

            //check xlog sampling
            XLogDiscard discardMode = findXLogDiscard(ctx, conf, pack);

            pack.discardType = discardMode.byteFlag;
            ctx.discardType = discardMode;
            ctx.profile.close(discardMode == XLogDiscard.NONE || (!pack.isDriving() && !discardMode.isForceDiscard()));

            pack.profileSize = ctx.profileSize;
            pack.profileCount = ctx.profileCount;

            if (ctx.group != null) {
                pack.group = DataProxy.sendGroup(ctx.group);
            }
            pack.userAgent = ctx.userAgent;
            pack.referer = ctx.referer;
            // 2015.02.02
            pack.apicallCount = ctx.apicall_count;
            pack.apicallTime = ctx.apicall_time;
            if (ctx.login != null) {
                pack.login = DataProxy.sendLogin(ctx.login);
            }
            if (ctx.desc != null) {
                pack.desc = DataProxy.sendDesc(ctx.desc);
            }
            if (ctx.web_name != null) {
                pack.webHash = DataProxy.sendWebName(ctx.web_name);
                pack.webTime = ctx.web_time;
            }
            if (ctx.queuingHost != null) {
                pack.queuingHostHash = DataProxy.sendWebName(ctx.queuingHost);
                pack.queuingTime = ctx.queuingTime;
            }
            if (ctx.queuing2ndHost != null) {
                pack.queuing2ndHostHash = DataProxy.sendWebName(ctx.queuing2ndHost);
                pack.queuing2ndTime = ctx.queuing2ndTime;
            }
            pack.text1 = ctx.text1;
            pack.text2 = ctx.text2;
            pack.text3 = ctx.text3;
            pack.text4 = ctx.text4;
            pack.text5 = ctx.text5;

            pack.b3Mode = ctx.b3Mode;

            delayedServiceManager.checkDelayedService(pack, ctx.serviceName);

            if (!ctx.skipMetering) {
                metering(pack);
                meteringInteraction(ctx, pack);
            }

            //send all child xlogs, and check it again on the collector server. (follows parent's discard type)
            if ((discardMode != XLogDiscard.DISCARD_ALL && discardMode != XLogDiscard.DISCARD_ALL_FORCE)
                    || (!pack.isDriving() && discardMode == XLogDiscard.DISCARD_ALL)) {
                if (!ctx.isReactiveStarted) {
                    if (ctx.profile_thread_cputime) {
                        if (ctx.latestCpu > 0) {
                            pack.cpu = (int) (ctx.latestCpu - ctx.startCpu);
                        } else {
                            pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
                        }
                    }
                    if (ctx.latestBytes > 0) {
                        pack.kbytes = (int) ((ctx.latestBytes - ctx.bytes) / 1024.0d);
                    } else {
                        pack.kbytes = (int) ((SysJMX.getCurrentThreadAllocBytes(conf.profile_thread_memory_usage_enabled) - ctx.bytes) / 1024.0d);
                    }
                }
                DataProxy.sendXLog(pack);

            } else {
                DataProxy.sendDroppedXLog(new DroppedXLogPack(pack.gxid, pack.txid));
            }
        } catch (Throwable e) {
            Logger.println("A146", e.getMessage(), e);
        }
    }

    private static void flushErrorSummary(TraceContext ctx) {
        ErrorEntity errorEntity = null;
        while ((errorEntity = ctx.pollErrorEntity()) != null) {
            ServiceSummary.getInstance().process(errorEntity.getThrowable(), errorEntity.getMessage(), ctx.serviceHash, ctx.txid, errorEntity.getSql(), errorEntity.getApi());
        }
    }

    private static void endHttpProcessingIfStatNull(Throwable thr) {
        // FIXME do nothing, isn't it right??

//        if (thr == null) {
//            TraceContextManager.clearForceDiscard();
//            return;
//        }
//        try {
//            TraceContext ctx = TraceContextManager.getContext();
//            if (ctx != null && ctx.error == 0) {
//                Configure conf = Configure.getInstance();
//                String emsg = thr.toString();
//                if (conf.profile_fullstack_service_error_enabled) {
//                    StringBuffer sb = new StringBuffer();
//                    sb.append(thr.getClass().getName()).append("\n");
//                    ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
//                    Throwable thrCause = thr.getCause();
//                    if (thrCause != null) {
//                        thr = thrCause;
//                        while (thr != null) {
//                            sb.append("\nCause...\n");
//                            ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
//                            thr = thr.getCause();
//                        }
//                    }
//                    emsg = sb.toString();
//                }
//                ctx.error = DataProxy.sendError(emsg);
//                ServiceSummary.getInstance().process(thr, ctx.error, ctx.serviceHash, ctx.txid, 0, 0);
//            }
//        } catch (Throwable t) {
//        }
//
//        TraceContextManager.clearForceDiscard();
//        return;
    }

    public static void metering(XLogPack pack) {
        switch (pack.xType) {
            case XLogTypes.WEB_SERVICE:
            case XLogTypes.APP_SERVICE:
                MeterService.getInstance().add(pack.elapsed, pack.sqlTime, pack.apicallTime, pack.queuingTime, pack.error != 0);
                ServiceSummary.getInstance().process(pack);
                break;
            case XLogTypes.BACK_THREAD:
            case XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE:
            case XLogTypes.BACK_THREAD2:
        }
    }

    public static boolean isStaticContents(String serviceName) {
        int x = serviceName.lastIndexOf('.');
        if (x <= 0)
            return false;
        try {
            String ext = serviceName.substring(x + 1);
            return Configure.getInstance().isStaticContents(ext);
        } catch (Exception e) {
            return false;
        }
    }

    public static Object startServiceWithCustomTxid(String name, String className, String methodName, String methodDesc, Object _this,
                                                    Object[] arg, byte xType, String customTxid) {

        TraceContext ctx = TraceContextManager.getContextByCustomTxid(customTxid);
        if (ctx != null) {
            return null;
        }
        return startService0(name, className, methodName, methodDesc, _this, arg, xType, customTxid);
    }

    public static Object startService(String name, String className, String methodName, String methodDesc, Object _this,
                                      Object[] arg, byte xType) {

        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx != null) {
            return null;
        }
        return startService0(name, className, methodName, methodDesc, _this, arg, xType,  null);
    }

    public static Object startService0(String name, String className, String methodName, String methodDesc, Object _this,
                                      Object[] arg, byte xType, String customTxid) {
        try {
            if (TraceContextManager.startForceDiscard()) {
                return null;
            }

            Configure conf = Configure.getInstance();
            TraceContext ctx = new TraceContext(false);

            String service_name = AgentCommonConstant.normalizeHashCode(name);
            ctx.thread = Thread.currentThread();
            ctx.serviceHash = HashUtil.hash(service_name);
            ctx.serviceName = service_name;
            ctx.startTime = System.currentTimeMillis();
            ctx.txid = KeyGen.next();
            ctx.threadId = ctx.thread.getId();

            TraceContextManager.start(ctx);
            if (customTxid != null) {
                TraceContextManager.linkCustomTxid(customTxid, ctx.txid);
            }

            ctx.bytes = SysJMX.getCurrentThreadAllocBytes(conf.profile_thread_memory_usage_enabled);
            ctx.profile_thread_cputime = conf.profile_thread_cputime_enabled;
            if (ctx.profile_thread_cputime) {
                ctx.startCpu = SysJMX.getCurrentThreadCPU();
            }
            ctx.xType = xType;

            HashedMessageStep step = new HashedMessageStep();
            step.time = -1;
            ctx.threadName = ctx.thread.getName();
            if (StringUtil.isEmpty(ctx.threadName)) {
                ctx.threadName = ctx.thread.toString();
            }
            step.hash = DataProxy.sendHashedMessage("[driving thread] " + ctx.threadName);
            ctx.profile.add(step);

            if (ctx.xType != XLogTypes.BACK_THREAD && ctx.xType != XLogTypes.BACK_THREAD2) {
                PluginAppServiceTrace.start(ctx, new HookArgs(className, methodName, methodDesc, _this, arg));
            } else {
                PluginBackThreadTrace.start(ctx, new HookArgs(className, methodName, methodDesc, _this, arg));
            }

            if (ctx.xType == XLogTypes.BACK_THREAD) {
                MethodStep2 ms = new MethodStep2();
                ms.hash = DataProxy.sendMethodName(service_name);
                ms.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                if (ctx.profile_thread_cputime) {
                    ms.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
                }
                ctx.profile.push(ms);

                return new LocalContext(ctx, ms);
            } else {
                return new LocalContext(ctx, null);
            }
        } catch (Throwable t) {
            Logger.println("A147", t);
        }
        return null;
    }

    public static void endService(Object stat, Object returnValue, Throwable thr) {
        try {
            LocalContext localCtx = (LocalContext) stat;
            if (localCtx == null) {
                TraceContextManager.clearForceDiscard();
                return;
            }
            TraceContext ctx = localCtx.context;
            if (ctx == null) {
                TraceContextManager.clearForceDiscard();
                return;
            }
            // add error summary
            flushErrorSummary(ctx);
            if (ctx.xType == XLogTypes.BACK_THREAD) {
                MethodStep2 step = (MethodStep2) localCtx.stepSingle;
                step.elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - step.start_time;
                if (ctx.profile_thread_cputime) {
                    step.cputime = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu) - step.start_cpu;
                }
                step.error = errorCheck(ctx, thr);
                ctx.profile.pop(step);
                TraceContextManager.end(ctx);
                ctx.profile.close(true);
                return;
            }

            if (ctx.xType != XLogTypes.BACK_THREAD && ctx.xType != XLogTypes.BACK_THREAD2) {
                PluginAppServiceTrace.end(ctx);
            } else {
                PluginBackThreadTrace.end(ctx);
            }

            TraceContextManager.end(ctx);

            XLogPack pack = new XLogPack();
            pack.txid = ctx.txid;
            pack.gxid = ctx.gxid;
            pack.caller = ctx.caller;

            // pack.endTime = System.currentTimeMillis();
            pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
            pack.error = errorCheck(ctx, thr);

            //check xlog sampling
            XLogDiscard discardMode = findXLogDiscard(ctx, conf, pack);
            pack.discardType = discardMode.byteFlag;
            ctx.discardType = discardMode;
            ctx.profile.close(discardMode == XLogDiscard.NONE || (!pack.isDriving() && !discardMode.isForceDiscard()));

            pack.profileCount = ctx.profileCount;
            pack.profileSize = ctx.profileSize;

            DataProxy.sendServiceName(ctx.serviceHash, ctx.serviceName);
            pack.service = ctx.serviceHash;
            pack.userAgent = ctx.userAgent;
            pack.referer = ctx.referer;
            pack.threadNameHash = DataProxy.sendHashedMessage(ctx.threadName);
            pack.xType = ctx.xType;

            pack.status = ctx.status;
            pack.sqlCount = ctx.sqlCount;
            pack.sqlTime = ctx.sqlTime;
            pack.ipaddr = IPUtil.toBytes(ctx.remoteIp);
            pack.userid = ctx.userid;

            if (ctx.hasDumpStack) {
                pack.hasDump = 1;
            } else {
                pack.hasDump = 0;
            }

            // 2015.11.10
            if (ctx.group != null) {
                pack.group = DataProxy.sendGroup(ctx.group);
            }
            // 2015.02.02
            pack.apicallCount = ctx.apicall_count;
            pack.apicallTime = ctx.apicall_time;
            if (ctx.login != null) {
                pack.login = DataProxy.sendLogin(ctx.login);
            }
            if (ctx.desc != null) {
                pack.desc = DataProxy.sendDesc(ctx.desc);
            }
            pack.text1 = ctx.text1;
            pack.text2 = ctx.text2;
            pack.text3 = ctx.text3;
            pack.text4 = ctx.text4;
            pack.text5 = ctx.text5;

            delayedServiceManager.checkDelayedService(pack, ctx.serviceName);
            if (!ctx.skipMetering) {
                metering(pack);
                meteringInteraction(ctx, pack);
            }

            //send all child xlogs, and check it again on the collector server. (follows parent's discard type)
            if ((discardMode != XLogDiscard.DISCARD_ALL && discardMode != XLogDiscard.DISCARD_ALL_FORCE)
                    || (!pack.isDriving() && discardMode == XLogDiscard.DISCARD_ALL)) {

                if (ctx.profile_thread_cputime) {
                    pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
                }
                pack.kbytes = (int) ((SysJMX.getCurrentThreadAllocBytes(conf.profile_thread_memory_usage_enabled) - ctx.bytes) / 1024.0d);
                DataProxy.sendXLog(pack);
            } else {
                DataProxy.sendDroppedXLog(new DroppedXLogPack(pack.gxid, pack.txid));
            }
        } catch (Throwable t) {
            Logger.println("A148", "service end error", t);
        }
    }


    private static void meteringInteraction(TraceContext ctx, XLogPack pack) {
        switch (pack.xType) {
            case XLogTypes.WEB_SERVICE:
            case XLogTypes.APP_SERVICE:
                meteringInteraction0(ctx, pack);
                break;
            case XLogTypes.BACK_THREAD:
            case XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE:
            case XLogTypes.BACK_THREAD2:
                break;
            default:
                break;
        }
    }

    private static void meteringInteraction0(TraceContext ctx, XLogPack pack) {
        if (conf.counter_interaction_enabled) {
            if (ctx.callerObjHash != 0) {
                MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
                        .getApiIncomingMeter(ctx.callerObjHash, conf.getObjHash());
                if (meterInteraction != null) {
                    meterInteraction.add(pack.elapsed, pack.error > 0);
                }
            } else {
                MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
                        .getNormalIncomingMeter(0, conf.getObjHash());
                if (meterInteraction != null) {
                    meterInteraction.add(pack.elapsed, pack.error > 0);
                }
            }
        }
    }

    private static int errorCheck(TraceContext ctx, Throwable thr) {
        int error = 0;
        if (ctx.error != 0) {
            error = ctx.error;
        } else if (thr != null) {
            Configure conf = Configure.getInstance();
            String emsg = thr.toString();
            if (conf.profile_fullstack_service_error_enabled) {
                StringBuffer sb = new StringBuffer();
                sb.append(emsg).append("\n");
                ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                Throwable thrCause = thr.getCause();
                if (thrCause != null) {
                    thr = thrCause;
                    while (thr != null) {
                        sb.append("\nCause...\n");
                        ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                        thr = thr.getCause();
                    }
                }
                emsg = sb.toString();
            }
            error = DataProxy.sendError(emsg);
            ServiceSummary.getInstance().process(thr, error, ctx.serviceHash, ctx.txid, 0, 0);
        } else if (ctx.userTransaction > 0 && conf.xlog_error_check_user_transaction_enabled) {
            error = DataProxy.sendError("Missing Commit/Rollback Error");
            ServiceSummary.getInstance().process(userTxNotClose, error, ctx.serviceHash, ctx.txid, 0, 0);
        }
        return error;
    }

    public static void capArgs(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        // MessageStep step = new MessageStep();
        // step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        // if (ctx.profile_thread_cputime_enabled) {
        // step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        // }
        // step.message = toString("CAP-ARG", className, methodName, methodDesc,
        // arg);
        // ctx.profile.add(step);
        PluginCaptureTrace.capArgs(ctx, new HookArgs(className, methodName, methodDesc, this1, arg));
    }

    public static void jspServlet(Object[] arg) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null || arg.length < 3)
            return;
        HashedMessageStep step = new HashedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
        step.hash = DataProxy.sendHashedMessage("JSP " + arg[2]);
        ctx.profile.add(step);
    }

    private static String toString(String type, String className, String methodName, String methodDesc, Object[] arg) {
        StringBuffer sb = new StringBuffer();
        sb.append(type).append(" ");
        sb.append(className);
        sb.append(".");
        sb.append(methodName);
        if (ArrayUtil.isEmpty(arg)) {
            return sb.toString();
        }
        sb.append(" [");
        for (int i = 0, max = arg.length; i < max; i++) {
            if (i > 0)
                sb.append(",");
            String arstr = StringUtil.limiting(ObjectUtil.toString(arg[i]), 80);
            sb.append(arstr);
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toStringRTN(String type, String className, String methodName, String methodDesc, Object arg) {
        StringBuffer sb = new StringBuffer();
        sb.append(type).append(" ");
        sb.append(className);
        sb.append(".");
        sb.append(methodName);
        sb.append(" [");
        String arstr = StringUtil.limiting(ObjectUtil.toString(arg), 80);
        sb.append(arstr);
        sb.append("]");
        return sb.toString();
    }

    private static String toStringTHIS(String type, String className, String methodDesc, Object arg) {
        StringBuffer sb = new StringBuffer();
        sb.append(type).append(" ");
        sb.append(className);
        sb.append(" [");
        String arstr = StringUtil.limiting(ObjectUtil.toString(arg), 80);
        sb.append(arstr);
        sb.append("]");
        return sb.toString();
    }

    public static void capThis(String className, String methodDesc, Object this0) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        // MessageStep step = new MessageStep();
        // step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        // if (ctx.profile_thread_cputime_enabled) {
        // step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        // }
        // step.message = toStringTHIS("CAP-THIS", className, methodDesc,
        // this0);
        // ctx.profile.add(step);
        PluginCaptureTrace.capThis(ctx, className, methodDesc, this0);
    }

    public static void capReturn(String className, String methodName, String methodDesc, Object this1, Object rtn) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        PluginCaptureTrace.capReturn(ctx, new HookReturn(className, methodName, methodDesc, this1, rtn));
    }

    public static Object startMethod(int hash, String classMethod) {
        return startMethod(hash, classMethod, null);
    }

    public static Object startMethod(int hash, String classMethod, TraceContext ctx) {
        if (conf.profile_method_enabled == false) {
            return null;
        }

        if (TraceContextManager.isForceDiscarded()) {
            return null;
        }

        if (ctx == null) {
            ctx = TraceContextManager.getContext();
        }

        if (ctx == null) {
            //System.out.println("[Scouter][HookMethodCtxNull]" + classMethod);
            if (conf._trace_auto_service_enabled) {
                Object localContext = startService(classMethod, null, null, null, null, null, XLogTypes.APP_SERVICE);
                if (localContext != null) {
                    //service start
                    ((LocalContext) localContext).service = true;
                    if (conf._trace_auto_service_backstack_enabled) {
                        String stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2);
                        AutoServiceStartAnalyzer.put(classMethod, stack);
                        MessageStep m = new MessageStep();
                        m.message = "SERVICE BACKSTACK:\n" + stack;
                        ((LocalContext) localContext).context.profile.add(m);
                    }
                }
                return localContext;
            }
            return null;
        }
        MethodStep p = new MethodStep();
        p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
        p.hash = hash;
        ctx.profile.push(p);
        return new LocalContext(ctx, p);
    }

    public static void endMethod(Object localContext, Throwable thr) {
        if (localContext == null)
            return;
        LocalContext lctx = (LocalContext) localContext;
        if (lctx.service) {
            endService(lctx, null, thr);
            return;
        }
        MethodStep step = (MethodStep) lctx.stepSingle;
        if (step == null)
            return;
        TraceContext tctx = lctx.context;
        if (tctx == null)
            return;
        step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
        if (tctx.profile_thread_cputime) {
            step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
        }
        tctx.profile.pop(step);
    }

    public static void setSpringControllerName(String name) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null || name == null)
            return;
        if (!ctx.alreadySetControllerName) {
            ctx.alreadySetControllerName = true;
            ctx.serviceName = name;
            ctx.serviceHash = HashUtil.hash(name);
        }
    }

    public static void startSpringControllerMethod(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null)
            return;
        if (conf.profile_spring_controller_method_parameter_enabled) {
            if (arg == null) {
                return;
            }
            int start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            for (int i = 0; i < arg.length; i++) {
                if (arg[i] == null) continue;
                String value = "param: " + StringUtil.limiting(arg[i].toString(), 1024);

                MessageStep step = new MessageStep(value);
                step.start_time = start_time;
                ctx.profile.add(step);
            }
        }
        PluginSpringControllerCaptureTrace.capArgs(ctx, new HookArgs(className, methodName, methodDesc, this1, arg));
    }

    public static void setStatus(int httpStatus) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null)
            return;
        ctx.status = httpStatus;
    }

    private static XLogDiscard findXLogDiscard(TraceContext ctx, Configure conf, XLogPack pack) {
        if (ctx.forceNotSamplingDrop) {
            return XLogDiscard.NONE;
        }
        XLogDiscard discardMode = pack.error != 0 ? XLogDiscard.NONE : XLogSampler.getInstance().evaluateXLogDiscard(pack.elapsed, ctx.serviceName);
        //check xlog discard pattern
        if (XLogSampler.getInstance().isDiscardServicePattern(ctx.serviceName)) {
            discardMode = XLogDiscard.DISCARD_ALL_FORCE;
            if (pack.error != 0 && conf.xlog_discard_service_show_error) {
                discardMode = XLogDiscard.NONE;
            }
        }
        return discardMode;
    }

    public static XLogPack txperf(long endtime, long txid, int service_hash, String serviceName, int elapsed, int cpu,
                                  int sqlCount, int sqlTime, String remoteAddr, String error, long visitor) {
        XLogPack pack = new XLogPack();
        pack.cpu = cpu;
        pack.endTime = endtime;
        pack.elapsed = elapsed;
        DataProxy.sendServiceName(service_hash, serviceName);
        pack.service = service_hash;
        pack.kbytes = 0;
        pack.status = 0;
        pack.sqlCount = sqlCount;
        pack.sqlTime = sqlTime;
        pack.txid = txid;
        pack.ipaddr = IPUtil.toBytes(remoteAddr);
        pack.userid = visitor;
        if (error != null) {
            pack.error = DataProxy.sendError(error);
        }
        MeterService.getInstance().add(pack.elapsed, pack.sqlTime, pack.apicallTime, pack.queuingTime, error != null);
        DataProxy.sendXLog(pack);
        MeterUsers.add(pack.userid);
        return pack;
    }

    public static void addMessage(String msg) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        MessageStep p = new MessageStep();
        p.message = msg;
        p.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        if (ctx.profile_thread_cputime) {
            p.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
        }
        ctx.profile.add(p);
    }

    public static void endRequestAsyncStart(Object asyncContext) {
        TraceContext traceContext = TraceContextManager.getContext(true);
        if (traceContext == null) return;
        if (http == null) return;
        http.addAsyncContextListener(asyncContext);
        traceContext.asyncServletStarted = true;
    }

    public static void dispatchAsyncServlet(Object asyncContext, String url) {
        if (http == null) return;
        TraceContext ctx = http.getTraceContextFromAsyncContext(asyncContext);
        if (ctx == null) return;

        boolean self = http.isSelfDispatch(asyncContext);
        if (self) {
            //http.setSelfDispatch(asyncContext, false);
            //return;
        }

        if (ctx.gxid == 0) {
            ctx.gxid = ctx.txid;
        }
        long callee = KeyGen.next();
        http.setDispatchTransferMap(asyncContext, ctx.gxid, ctx.txid, callee, XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE);

        DispatchStep step = new DispatchStep();
        step.txid = callee;

        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);

        // It maybe another thread
        if (ctx.profile_thread_cputime) {
            step.start_cpu = -1;
            step.cputime = -1;
        }

        step.address = "dispatch";
        if (self) url = "[self]";
        step.hash = DataProxy.sendApicall(step.address + "://" + url);
        ctx.profile.add(step);
    }

    public static void selfDispatchAsyncServlet(Object asyncContext) {
        if (http == null) return;
        http.setSelfDispatch(asyncContext, true);
    }

    public static void asyncPossibleInstanceInitInvoked(Object keyObject) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx == null) return;

            if (TransferMap.get(System.identityHashCode(keyObject)) != null) {
                return;
            }

            long gxid = ctx.gxid == 0 ? ctx.txid : ctx.gxid;
            ctx.gxid = gxid;
            long callee = KeyGen.next();

            ThreadCallPossibleStep threadCallPossibleStep = new ThreadCallPossibleStep();
            threadCallPossibleStep.txid = callee;

            threadCallPossibleStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            String initObjName = keyObject.toString().replace("$ByteBuddy", "").replace("$$Lambda", "$$L");
            initObjName = AgentCommonConstant.normalizeHashCode(initObjName);

            threadCallPossibleStep.hash = DataProxy.sendApicall(initObjName);
            ctx.profile.add(threadCallPossibleStep);

            TransferMap.put(System.identityHashCode(keyObject), gxid, ctx.txid, callee, ctx.xType, Thread.currentThread().getId(), threadCallPossibleStep);
        } catch (Throwable t) {
            Logger.println("B1204", "Exception: asyncPossibleInstanceInitInvoked", t);
        }
    }

    public static Object startAsyncPossibleService(Object keyObject, String fullName,
                                                   String className, String methodName, String methodDesc,
                                                   Object _this, Object[] arg) {

        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            int objKey = System.identityHashCode(keyObject);
            TransferMap.ID id = TransferMap.get(objKey);
            TransferMap.remove(objKey);

            if (id == null) {
                return null;
            }

            if (ctx != null) {
                if (ctx.txid == id.caller) {
                    return null;
                } else {
                    Logger.trace("B109 - recevieAsyncPossibleStep -> caller txid : "
                            + id.caller + "=" + Hexa32.toString32(id.caller)
                            + " ctx.txid : " + ctx.txid + "=" + Hexa32.toString32(ctx.txid)
                            + " id.callee : " + id.callee + "=" + Hexa32.toString32(id.callee)
                            + " id.thread : " + id.callerThreadId
                            + " current.thread : " + Thread.currentThread().getName() + "=" + Thread.currentThread().getId());
                    return null;
                }
            }

            LocalContext localContext = (LocalContext) startService(fullName, className, methodName, methodDesc, _this, arg, XLogTypes.BACK_THREAD2);
            if (localContext == null) {
                return null;
            }
            localContext.service = true;
            if (id.gxid != 0) localContext.context.gxid = id.gxid;
            if (id.callee != 0) {
                long oldTxid = localContext.context.txid;
                localContext.context.txid = id.callee;
                TraceContextManager.takeoverTxid(localContext.context, oldTxid);
            }
            if (id.caller != 0) localContext.context.caller = id.caller;

            String serviceName = StringUtil.removeLastString(className, '/') + "#" + methodName + "() -- " + fullName;
            serviceName = serviceName.replace("$ByteBuddy", "");
            serviceName = serviceName.replace("$$Lambda", "$$L");
            serviceName = AgentCommonConstant.normalizeHashCode(serviceName);
            localContext.context.serviceHash = HashUtil.hash(serviceName);
            localContext.context.serviceName = serviceName;

            if (id.tcStep != null) {
                id.tcStep.threaded = 1;
                id.tcStep.hash = DataProxy.sendApicall(serviceName);
            }

            return localContext;
        } catch (Throwable t) {
            Logger.println("B1091", "Exception: startAsyncPossibleService", t);
            return null;
        }
    }

    public static void endAsyncPossibleService(Object oRtn, Object oLocalContext, Throwable thr) {
        try {
            if (oLocalContext == null)
                return;
            LocalContext lctx = (LocalContext) oLocalContext;
            if (lctx.service) {
                endService(lctx, oRtn, thr);
                return;
            }
        } catch (Throwable t) {
            Logger.println("B1201", "Exception: endAsyncPossibleService", t);
        }
    }

    public static void springAsyncExecutionSubmit(Object _this, Callable callable) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx == null) return;

            if (TransferMap.get(System.identityHashCode(callable)) != null) {
                return;
            }

            long gxid = ctx.gxid == 0 ? ctx.txid : ctx.gxid;
            ctx.gxid = gxid;
            long callee = KeyGen.next();

            ThreadCallPossibleStep threadCallPossibleStep = new ThreadCallPossibleStep();
            threadCallPossibleStep.txid = callee;
            threadCallPossibleStep.threaded = 1;

            threadCallPossibleStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            String threadCallName = (ctx.lastThreadCallName != null) ? ctx.lastThreadCallName : callable.toString();
            threadCallName = AgentCommonConstant.normalizeHashCode(threadCallName);
            ctx.lastThreadCallName = null;

            threadCallPossibleStep.hash = DataProxy.sendApicall(threadCallName);
            threadCallPossibleStep.nameTemp = threadCallName;
            ctx.profile.add(threadCallPossibleStep);
            ctx.lastThreadCallPossibleStep = threadCallPossibleStep;

            TransferMap.put(System.identityHashCode(callable), gxid, ctx.txid, callee, ctx.xType, Thread.currentThread().getId(), threadCallPossibleStep);
        } catch (Throwable t) {
            Logger.println("B1202", "Exception: springAsyncExecutionSubmit", t);
        }
    }

    public static void springAsyncDetermineExecutor(Method m) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null) return;
        if (m == null) return;

        ctx.lastThreadCallName = m.getDeclaringClass().getName() + "#" + m.getName() + "()";
    }

    public static void executorServiceSubmitted(Object callRunnable) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null) return;
        if (callRunnable == null) return;
        if (callRunnable instanceof WrTaskCallable) return;

        ctx.lastThreadCallName = callRunnable.getClass().getName();
    }

    public static void executorServiceExecuted(Object callRunnable) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx == null) return;
            if (callRunnable == null) return;
            if (callRunnable instanceof WrTaskCallable) return;
            if (ctx.lastThreadCallPossibleStep != null) {
                ctx.lastThreadCallPossibleStep = null;
                return;
            }

            if (TransferMap.get(System.identityHashCode(callRunnable)) != null) {
                return;
            }

            long gxid = ctx.gxid == 0 ? ctx.txid : ctx.gxid;
            ctx.gxid = gxid;
            long callee = KeyGen.next();

            ThreadCallPossibleStep threadCallPossibleStep = new ThreadCallPossibleStep();
            threadCallPossibleStep.txid = callee;
            threadCallPossibleStep.threaded = 1;

            threadCallPossibleStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            String threadCallName = (ctx.lastThreadCallName != null) ? ctx.lastThreadCallName : callRunnable.getClass().getName();
            threadCallName = StringUtil.removeLastString(threadCallName, '/');
            threadCallName = threadCallName.replace("$$Lambda", "$$L");
            threadCallName = AgentCommonConstant.normalizeHashCode(threadCallName);

            ctx.lastThreadCallName = null;

            threadCallPossibleStep.hash = DataProxy.sendApicall(threadCallName);
            threadCallPossibleStep.nameTemp = threadCallName;
            ctx.profile.add(threadCallPossibleStep);

            TransferMap.put(System.identityHashCode(callRunnable), gxid, ctx.txid, callee, ctx.xType, Thread.currentThread().getId(), threadCallPossibleStep);

        } catch (Throwable t) {
            Logger.println("B1204", "Exception: executorServiceExecuted", t);
        }
    }

    public static Runnable executorServiceGetTaskEnd(Runnable task) {
        if (task == null) {
            return null;
        }
        return new WrTask(task);
    }

    public static Object callRunnableCallInvoked(Object callRunnableObj) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            int objKey = System.identityHashCode(callRunnableObj);
            TransferMap.ID id = TransferMap.get(objKey);
            TransferMap.remove(objKey);

            if (id == null) {
                return null;
            }

            if (ctx != null) {
                if (ctx.txid == id.caller) {
                    return null;
                } else {
                    Logger.trace("B110 - receiveAsyncPossibleStep -> caller txid : "
                            + id.caller + "=" + Hexa32.toString32(id.caller)
                            + " ctx.txid : " + ctx.txid + "=" + Hexa32.toString32(ctx.txid)
                            + " id.callee : " + id.callee + "=" + Hexa32.toString32(id.callee)
                            + " id.thread : " + id.callerThreadId
                            + " current.thread : " + Thread.currentThread().getName() + "=" + Thread.currentThread().getId());
                    return null;
                }
            }

            if (id.tcStep != null) {
                id.tcStep.threaded = 1;
            }

            LocalContext localContext = (LocalContext) startService(id.tcStep.nameTemp, null, null, null, null, null, XLogTypes.BACK_THREAD2);
            if (localContext == null) {
                return null;
            }
            localContext.service = true;
            if (id.gxid != 0) localContext.context.gxid = id.gxid;
            if (id.callee != 0) {
                long oldTxid = localContext.context.txid;
                localContext.context.txid = id.callee;
                TraceContextManager.takeoverTxid(localContext.context, oldTxid);
            }
            if (id.caller != 0) localContext.context.caller = id.caller;

            return localContext;
        } catch (Throwable t) {
            Logger.println("B1111", "Exception: callRunnableCallInvoked", t);
            return null;
        }
    }

    public static void callRunnableCallEnd(Object oRtn, Object oLocalContext, Throwable thr) {
        endAsyncPossibleService(oRtn, oLocalContext, thr);
    }

    private static final ConcurrentMap<String, Method> REFLECT_METHODS = new ConcurrentHashMap<String, Method>();

    public static void hystrixPrepareInvoked(Object hystrixCommand) {
        TraceContext ctx = TraceContextManager.getContext(true);
        if (ctx == null) return;

        if (TransferMap.get(System.identityHashCode(hystrixCommand)) != null) {
            return;
        }
        Class<?> clazz = hystrixCommand.getClass();
        String getCommandGroupHashKey = clazz.getName() + "#getCommandKey";
        String getCommandKeyHashKey = clazz.getName() + "#getCommandKey";

        Method getCommandGroup = REFLECT_METHODS.get(getCommandGroupHashKey);
        Method getCommandKey = REFLECT_METHODS.get(getCommandKeyHashKey);

        try {
            if (getCommandGroup == null) {
                getCommandGroup = clazz.getMethod("getCommandGroup", null);
                REFLECT_METHODS.putIfAbsent(getCommandGroupHashKey, getCommandGroup);
            }
            if (getCommandKey == null) {
                getCommandKey = clazz.getMethod("getCommandKey", null);
                REFLECT_METHODS.putIfAbsent(getCommandKeyHashKey, getCommandKey);
            }

            ctx.lastThreadCallName = "[hystrix]" + getCommandGroup.invoke(hystrixCommand) + "#" + getCommandKey.invoke(hystrixCommand);

        } catch (NoSuchMethodException e) {
            Logger.println("S267", "Hystrix hooking failed. check scouter supporting hystrix version.", e);
        } catch (IllegalAccessException e) {
            Logger.println("S268", "Hystrix hooking failed. check scouter supporting hystrix version.", e);
        } catch (InvocationTargetException e) {
            Logger.println("S269", "Hystrix hooking failed. check scouter supporting hystrix version.", e);
        }

        callRunnableInitInvoked(hystrixCommand, true, true);
    }

    public static void callRunnableInitInvoked(Object callRunnableObj, boolean addStepToCtx, boolean isIgnoreIfNoThreaded) {
        try {
            TraceContext ctx = TraceContextManager.getContext(true);
            if (ctx == null) return;

            if (TransferMap.get(System.identityHashCode(callRunnableObj)) != null) {
                return;
            }

            long gxid = ctx.gxid == 0 ? ctx.txid : ctx.gxid;
            ctx.gxid = gxid;
            long callee = KeyGen.next();

            ThreadCallPossibleStep threadCallPossibleStep = new ThreadCallPossibleStep();
            threadCallPossibleStep.txid = callee;

            threadCallPossibleStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            String threadCallName = (ctx.lastThreadCallName != null) ? ctx.lastThreadCallName : callRunnableObj.toString();
            threadCallName = AgentCommonConstant.normalizeHashCode(threadCallName);

            ctx.lastThreadCallName = null;

            threadCallPossibleStep.hash = DataProxy.sendApicall(threadCallName);
            threadCallPossibleStep.nameTemp = threadCallName;
            if (isIgnoreIfNoThreaded) {
                threadCallPossibleStep.isIgnoreIfNoThreaded = true;
            }
            ctx.profile.add(threadCallPossibleStep);
            if (addStepToCtx) {
                ctx.lastThreadCallPossibleStep = threadCallPossibleStep;
            }

            TransferMap.put(System.identityHashCode(callRunnableObj), gxid, ctx.txid, callee, ctx.xType, Thread.currentThread().getId(), threadCallPossibleStep);
        } catch (Throwable t) {
            Logger.println("B1203", "Exception: callRunnableInitInvoked", t);
        }
    }

    public static void callRunnableInitInvoked(Object callRunnableObj) {
        callRunnableInitInvoked(callRunnableObj, false, false);
    }

    public static Callable wrap1stParamAsWrTaskCallable(Callable callable) {
        if (callable.getClass().getName().contains("$Lambda")) {
            return new WrTaskCallable(callable);
        } else {
            return callable;
        }
    }

    public static void endExceptionConstructor(String className, String methodDesc, Object this0) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        if (!(this0 instanceof Throwable)) {
            return;
        }
        if (ctx.error != 0) {
            return;
        }
        Throwable t = (Throwable) this0;

        String msg = t.getMessage();
        if (msg == null) {
            msg = "";
        }

        if (conf.profile_fullstack_hooked_exception_enabled) {
            StringBuffer sb = new StringBuffer();
            sb.append(msg).append("\n");
            ThreadUtil.getStackTrace(sb, t, conf.profile_fullstack_max_lines);
            Throwable cause = t.getCause();
            while (cause != null) {
                sb.append("\nCause...\n");
                ThreadUtil.getStackTrace(sb, cause, conf.profile_fullstack_max_lines);
                cause = cause.getCause();
            }
            msg = sb.toString();
        }

        int hash = DataProxy.sendError(msg);
        ctx.error = hash;
        ctx.offerErrorEntity(ErrorEntity.of(t, hash, 0, 0));
    }

    public static StringBuilder appendParentClassName(Class clazz, StringBuilder sb) {
        Class superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            sb.append(",").append(superClazz.getName());
            return appendParentClassName(superClazz, sb);
        } else {
            return sb;
        }
    }

    public static String buildClassHierarchyConcatString(Class clazz) {
        if (clazz == null) return null;
        StringBuilder sb = new StringBuilder(clazz.getName());
        appendParentClassName(clazz, sb);
        return sb.toString();
    }

    public static void startExceptionHandler(String className, String methodName, String methodDesc, Object this1, Object[] args) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) return;
        if (ctx.error != 0) return;
        if (args == null || args.length == 0) return;

        Throwable t = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                t = (Throwable) args[i];
                break;
            }
        }

        if (t == null) {
            return;
        }

        //skip exclude patterns
        String classHierarchyConcatString = buildClassHierarchyConcatString(t.getClass());
        String[] excludes = UserExceptionHandlerASM.exceptionExcludeClasseNames;
        for (int i = 0; i < excludes.length; i++) {
            if (classHierarchyConcatString.indexOf(excludes[i]) >= 0) {
                return;
            }
        }

        StringBuffer sb = new StringBuffer(64);
        sb.append(className).append("#").append(methodName).append(" handled exception: ").append(t.getMessage());

        if (conf.profile_fullstack_hooked_exception_enabled) {
            sb.append("\n");
            ThreadUtil.getStackTrace(sb, t, conf.profile_fullstack_max_lines);
            Throwable[] suppressed = t.getSuppressed();
            if (suppressed != null) {
                for (Throwable sup : suppressed) {
                    sb.append("\nSuppressed...\n");
                    sb.append(sup.toString()).append("\n");
                    ThreadUtil.getStackTrace(sb, sup, conf.profile_fullstack_max_lines);
                }
            }
            Throwable cause = t.getCause();
            while (cause != null) {
                sb.append("\nCause...\n");
                ThreadUtil.getStackTrace(sb, cause, conf.profile_fullstack_max_lines);
                Throwable[] suppressed2 = t.getSuppressed();
                if (suppressed2 != null) {
                    for (Throwable sup : suppressed2) {
                        sb.append("\nSuppressed...\n");
                        sb.append(sup.toString()).append("\n");
                        ThreadUtil.getStackTrace(sb, sup, conf.profile_fullstack_max_lines);
                    }
                }
                cause = cause.getCause();
            }
        }

        int hash = DataProxy.sendError(sb.toString());
        ctx.error = hash;
        ctx.offerErrorEntity(ErrorEntity.of(t, hash, 0, 0));
    }

    private static ByteArrayKeyLinkedMap<String> redisKeyMap =new ByteArrayKeyLinkedMap<String>().setMax(100);
    private static String JEDIS_COMMAND_MSG = "[REDIS]%s: %s";
    private static String JEDIS_COMMAND_ERROR_MSG = "[REDIS][ERROR]%s: %s [Exception:%s] %s";

    public static void setTraceJedisHostPort(String host, int port) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return;
        }
        if (TraceContextManager.isForceDiscarded()) {
            return;
        }

        ctx.lastRedisConnHost = host;
        ctx.lastRedisConnPort = port;
    }

    public static void setRedisKey(byte[] barr, Object key) {
        redisKeyMap.put(barr, key.toString());
    }

    public static Object startSendRedisCommand() {
        if (TraceContextManager.isForceDiscarded()) {
            return null;
        }

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }

        ParameterizedMessageStep step = new ParameterizedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        ctx.profile.push(step);

        return new LocalContext(ctx, step);
    }

    public static void endSendRedisCommand(byte[] cmd, byte[][] args, Object localContext, Throwable thr) {
        if (localContext == null)
            return;

        LocalContext lctx = (LocalContext) localContext;

        ParameterizedMessageStep step = (ParameterizedMessageStep) lctx.stepSingle;
        if (step == null) return;

        TraceContext tctx = lctx.context;
        if (tctx == null) return;

        String key = null;
        if (args.length > 0) {
            key = redisKeyMap.get(args[0]);
        }
        if (key == null) {
            if (conf.profile_redis_key_forcibly_stringify_enabled) {
                if (args.length > 0) {
                    key = new String(args[0]);
                } else {
                    key = "EMPTY";
                }
            } else {
                key = "-";
            }
        }
        String command = new String(cmd);


        int elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
        step.setElapsed(elapsed);

        if (thr == null) {
            step.setMessage(DataProxy.sendHashedMessage(JEDIS_COMMAND_MSG), command, key);
            step.setLevel(ParameterizedMessageLevel.INFO);

        } else {
            String msg = thr.toString();
            step.setMessage(DataProxy.sendHashedMessage(JEDIS_COMMAND_ERROR_MSG), command, key, thr.getClass().getName(), msg);
            step.setLevel(ParameterizedMessageLevel.ERROR);

            if (tctx.error == 0 && conf.xlog_error_on_redis_exception_enabled) {
                if (conf.profile_fullstack_redis_error_enabled) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(msg).append("\n");
                    ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                    Throwable cause = thr.getCause();
                    while (cause != null) {
                        sb.append("\nCause...\n");
                        ThreadUtil.getStackTrace(sb, cause, conf.profile_fullstack_max_lines);
                        cause = cause.getCause();
                    }
                    msg = sb.toString();
                }

                int hash = DataProxy.sendError(msg);
                tctx.error = hash;
            }
        }

        tctx.profile.pop(step);

        if (conf.counter_interaction_enabled) {
            String redisName = "redis";
            if (StringUtil.isNotEmpty(tctx.lastRedisConnHost)) {
                redisName = tctx.lastRedisConnHost + ":" + tctx.lastRedisConnPort;
            }
            int redisHash = DataProxy.sendObjName(redisName);
            MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getRedisCallMeter(conf.getObjHash(), redisHash);
            if (meterInteraction != null) {
                meterInteraction.add(elapsed, thr != null);
            }
        }
    }

    static IKafkaTracer kafkaTracer;
    private static String KAFKA_COMMAND_MSG = "[KAFKA] bootstrap : %s, topic : %s";
    private static String KAFKA_COMMAND_ERROR_MSG = "[KAFKA][ERROR] bootstrap : %s, topic : %s [Exception:%s] %s";

    public static Object startKafkaProducer(Object producerConfig, String topic) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }

        if (kafkaTracer == null) {
            kafkaTracer = KafkaTraceFactory.create(producerConfig.getClass().getClassLoader());
        }

        String bootstrapServer = kafkaTracer.getBootstrapServer(producerConfig);

        ParameterizedMessageStep step = new ParameterizedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        step.putTempMessage("bootstrap", bootstrapServer);
        step.putTempMessage("topic", topic);
        ctx.profile.push(step);
        return new LocalContext(ctx, step);
    }

    public static void endKafkaProducer(Object localContext, Throwable thr) {
        if (localContext == null)
            return;
        LocalContext lctx = (LocalContext) localContext;
        ParameterizedMessageStep step = (ParameterizedMessageStep) lctx.stepSingle;
        if (step == null) return;

        TraceContext tctx = lctx.context;
        if (tctx == null) return;

        int elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
        step.setElapsed(elapsed);

        String bootstrapServer = step.getTempMessage("bootstrap");
        String topic = step.getTempMessage("topic");

        if (StringUtil.isEmpty(bootstrapServer)) bootstrapServer = "-";
        if (StringUtil.isEmpty(topic)) topic = "-";

        if (thr == null) {
            step.setMessage(DataProxy.sendHashedMessage(KAFKA_COMMAND_MSG), bootstrapServer, topic);
            step.setLevel(ParameterizedMessageLevel.INFO);
        } else {
            String msg = thr.toString();
            step.setMessage(DataProxy.sendHashedMessage(KAFKA_COMMAND_ERROR_MSG), bootstrapServer, topic, thr.getClass().getName(), msg);
            step.setLevel(ParameterizedMessageLevel.ERROR);
        }

        tctx.profile.pop(step);

        //[KAFKA] bootstrap : [localhost:9092], topic : scouter-topic2 [0 ms]
        if (conf.counter_interaction_enabled) {
            String kafka = (bootstrapServer.length() > 1) ? bootstrapServer : "kafka";
            int kafkaHash = DataProxy.sendObjName(kafka);
            MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getKafkaCallMeter(conf.getObjHash(), kafkaHash);
            if (meterInteraction != null) {
                meterInteraction.add(elapsed, thr != null);
            }
        }
    }

    private static String RABBIT_COMMAND_MSG = "[RABBIT] exchange : %s, routing key : %s";
    private static String RABBIT_COMMAND_ERROR_MSG = "[RABBIT][ERROR] exchange : %s, routing key : %s [Exception:%s] %s";

    public static Object startRabbitPublish(String exchange, String routingKey) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }

        ParameterizedMessageStep step = new ParameterizedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        step.putTempMessage("exchange", exchange);
        step.putTempMessage("routingKey", routingKey);
        ctx.profile.push(step);
        return new LocalContext(ctx, step);
    }

    public static void endRabbitPublish(Object localContext, Throwable thr) {
        if (localContext == null)
            return;
        LocalContext lctx = (LocalContext) localContext;
        ParameterizedMessageStep step = (ParameterizedMessageStep) lctx.stepSingle;
        if (step == null) return;

        TraceContext tctx = lctx.context;
        if (tctx == null) return;

        int elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
        step.setElapsed(elapsed);

        String exchange = step.getTempMessage("exchange");
        String routingKey = step.getTempMessage("routingKey");

        if (StringUtil.isEmpty(exchange)) exchange = "-";
        if (StringUtil.isEmpty(routingKey)) routingKey = "-";

        if (thr == null) {
            step.setMessage(DataProxy.sendHashedMessage(RABBIT_COMMAND_MSG), exchange, routingKey);
            step.setLevel(ParameterizedMessageLevel.INFO);
        } else {
            String msg = thr.toString();
            step.setMessage(DataProxy.sendHashedMessage(RABBIT_COMMAND_ERROR_MSG), exchange, routingKey, thr.getClass().getName(), msg);
            step.setLevel(ParameterizedMessageLevel.ERROR);
        }

        tctx.profile.pop(step);

        if (conf.counter_interaction_enabled) {
            String rabbitmq = (exchange.length() > 1) ? exchange : "rabbitmq";
            int rabbitmqHash = DataProxy.sendObjName(rabbitmq);
            MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getRabbitmqCallMeter(conf.getObjHash(), rabbitmqHash);
            if (meterInteraction != null) {
                meterInteraction.add(elapsed, thr != null);
            }
        }
    }

    static ILettuceTrace lettuceTracer;
    static IRedissonTrace redissonTrace;

    public static Object startRedissonCommand(Object redissonConn, byte[] key, Object redisCommand) {
        if (TraceContextManager.isForceDiscarded()) {
            return null;
        }
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }
        if (redissonTrace == null) {
            redissonTrace = RedissonTraceFactory.create(redissonConn.getClass().getClassLoader());
        }

        String command = redissonTrace.getCommand(redisCommand);
        if (command == null) return null;

        redissonTrace.startRedis(ctx, redissonConn);
        String args = redissonTrace.parseArgs(key);

        ParameterizedMessageStep step = new ParameterizedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        step.putTempMessage("command", command);
        step.putTempMessage("args", args);
        ctx.profile.push(step);

        return new LocalContext(ctx, step);
    }

    public static void endRedissonCommand(Object localContext, Throwable thr) {
        endLettuceCommand(localContext, thr);
    }

    public static Object startLettuceCommand(Object channel, Object redisCommand) {
        if (TraceContextManager.isForceDiscarded()) {
            return null;
        }
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }
        if(redisCommand instanceof Collection) {
            return null;
        }
        if (lettuceTracer == null) {
            lettuceTracer = LettuceTraceFactory.create(channel.getClass().getClassLoader());
        }
        String command = lettuceTracer.getCommand(redisCommand);
        if (command == null) return null;

        lettuceTracer.startRedis(ctx, channel);
        String args = lettuceTracer.parseArgs(redisCommand);

        ParameterizedMessageStep step = new ParameterizedMessageStep();
        step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
        step.putTempMessage("command", command);
        step.putTempMessage("args", args);
        ctx.profile.push(step);

        return new LocalContext(ctx, step);
    }

    public static void endLettuceCommand(Object localContext, Throwable thr) {
        if (localContext == null)
            return;

        LocalContext lctx = (LocalContext) localContext;

        ParameterizedMessageStep step = (ParameterizedMessageStep) lctx.stepSingle;
        if (step == null) return;

        TraceContext tctx = lctx.context;
        if (tctx == null) return;

        int elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
        step.setElapsed(elapsed);

        String command = step.getTempMessage("command");
        String args = step.getTempMessage("args");
        if (StringUtil.isEmpty(command)) command = "-";
        if (StringUtil.isEmpty(args)) args = "-";

        if (thr == null) {
            step.setMessage(DataProxy.sendHashedMessage(JEDIS_COMMAND_MSG), command, args);
            step.setLevel(ParameterizedMessageLevel.INFO);

        } else {
            String msg = thr.toString();
            step.setMessage(DataProxy.sendHashedMessage(JEDIS_COMMAND_ERROR_MSG), command, args, thr.getClass().getName(), msg);
            step.setLevel(ParameterizedMessageLevel.ERROR);

            if (tctx.error == 0 && conf.xlog_error_on_redis_exception_enabled) {
                if (conf.profile_fullstack_redis_error_enabled) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(msg).append("\n");
                    ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                    Throwable cause = thr.getCause();
                    while (cause != null) {
                        sb.append("\nCause...\n");
                        ThreadUtil.getStackTrace(sb, cause, conf.profile_fullstack_max_lines);
                        cause = cause.getCause();
                    }
                    msg = sb.toString();
                }

                int hash = DataProxy.sendError(msg);
                tctx.error = hash;
            }
        }

        tctx.profile.pop(step);

        if (conf.counter_interaction_enabled) {
            String redisName = "redis";
            if (StringUtil.isNotEmpty(tctx.lastRedisConnHost)) {
                redisName = tctx.lastRedisConnHost ;
            }
            int redisHash = DataProxy.sendObjName(redisName);
            MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getRedisCallMeter(conf.getObjHash(), redisHash);
            if (meterInteraction != null) {
                meterInteraction.add(elapsed, thr != null);
            }
        }
    }

}
