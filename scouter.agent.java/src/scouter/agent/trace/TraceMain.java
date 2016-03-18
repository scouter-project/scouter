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

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.meter.MeterService;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.error.REQUEST_REJECT;
import scouter.agent.error.USERTX_NOT_CLOSE;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.PluginAppServiceTrace;
import scouter.agent.plugin.PluginCaptureTrace;
import scouter.agent.plugin.PluginHttpServiceTrace;
import scouter.agent.proxy.HttpTraceFactory;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.summary.ServiceSummary;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodStep2;
import scouter.util.*;

import javax.sql.DataSource;

public class TraceMain {
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

    private static IHttpTrace http = null;
    private static Configure conf = Configure.getInstance();
    private static Error REJECT = new REQUEST_REJECT("service rejected");
    private static Error userTxNotClose = new USERTX_NOT_CLOSE("Missing Commit/Rollback Error");

    public static Object startHttpService(Object req, Object res) {
        try {
            TraceContext ctx = TraceContextManager.getContext();
            if (ctx != null) {
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
            TraceContext ctx = TraceContextManager.getContext();
            if (ctx != null) {
                return null;
            }
            return startHttp(req, res);
        } catch (Throwable t) {
            Logger.println("A144", "fail to deploy ", t);
        }
        return null;
    }

    public static Object reject(Object stat, Object req, Object res) {
        Configure conf = Configure.getInstance();
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

    private static void addSeviceName(TraceContext ctx, Object req) {
        try {
            Configure conf = Configure.getInstance();

            StringBuilder sb = new StringBuilder();
            if (conf.trace_service_name_post_key != null) {
                String v = http.getParameter(req, conf.trace_service_name_post_key);
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
                String v = http.getHeader(req, conf.trace_service_name_header_key);
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

    private static Object startHttp(Object req, Object res) {
        if (http == null) {
            initHttp(req);
        }
        Configure conf = Configure.getInstance();
        TraceContext ctx = new TraceContext(conf.profile_summary_mode_enabled);
        ctx.thread = Thread.currentThread();
        ctx.txid = KeyGen.next();
        ctx.startTime = System.currentTimeMillis();
        ctx.startCpu = SysJMX.getCurrentThreadCPU();
        ctx.threadId = TraceContextManager.start(ctx.thread, ctx);
        ctx.bytes = SysJMX.getCurrentThreadAllocBytes();
        ctx.profile_thread_cputime = conf.profile_thread_cputime_enabled;

        http.start(ctx, req, res);
        if (ctx.serviceName == null)
            ctx.serviceName = "Non-URI";
        Stat stat = new Stat(ctx, req, res);
        stat.isStaticContents = ctx.isStaticContents;

        if (stat.isStaticContents == false) {
            PluginHttpServiceTrace.start(ctx, req, res);
        }
        return stat;
    }

    private static void initHttp(Object req) {
        synchronized (lock) {
            if (http == null) {
                http = HttpTraceFactory.create(req.getClass().getClassLoader());
            }
        }
    }

    public static void endHttpService(Object stat, Throwable thr) {
        try {
            Stat stat0 = (Stat) stat;
            if (stat0 == null) {
                if (thr == null)
                    return;
                try {
                    TraceContext ctx = TraceContextManager.getContext();
                    if (ctx != null && ctx.error == 0) {
                        Configure conf = Configure.getInstance();
                        String emsg = thr.toString();
                        if (conf.profile_fullstack_service_error_enabled) {
                            StringBuffer sb = new StringBuffer();
                            sb.append(emsg).append("\n");
                            ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                            thr = thr.getCause();
                            while (thr != null) {
                                sb.append("\nCause...\n");
                                ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                                thr = thr.getCause();
                            }
                            emsg = sb.toString();
                        }
                        ctx.error = DataProxy.sendError(emsg);
                        ServiceSummary.getInstance().process(thr, ctx.error, ctx.serviceHash, ctx.txid, 0, 0);
                    }
                } catch (Throwable t) {
                }
                return;
            }
            TraceContext ctx = stat0.ctx;

            if (conf.getEndUserPerfEndpointHash() == ctx.serviceHash) {
                TraceContextManager.end(ctx.threadId);
                return;
            }
            //additional service name
            addSeviceName(ctx, stat0.req);
            // HTTP END
            http.end(ctx, stat0.req, stat0.res);
            // static-contents -> stop processing
            if (stat0.isStaticContents) {
                TraceContextManager.end(ctx.threadId);
                return;
            }
            // Plug-in end
            PluginHttpServiceTrace.end(ctx, stat0.req, stat0.res);
            // profile close
            TraceContextManager.end(ctx.threadId);
            Configure conf = Configure.getInstance();
            XLogPack pack = new XLogPack();
            // pack.endTime = System.currentTimeMillis();
            pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
            boolean sendOk = pack.elapsed >= conf.xlog_lower_bound_time_ms;
            ctx.profile.close(sendOk);
            ctx.serviceHash = DataProxy.sendServiceName(ctx.serviceName);
            pack.service = ctx.serviceHash;
            pack.xType = XLogTypes.WEB_SERVICE;
            pack.txid = ctx.txid;
            pack.gxid = ctx.gxid;
            pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
            pack.bytes = (int) (SysJMX.getCurrentThreadAllocBytes() - ctx.bytes);
            pack.status = ctx.status;
            pack.sqlCount = ctx.sqlCount;
            pack.sqlTime = ctx.sqlTime;
            pack.ipaddr = IPUtil.toBytes(ctx.remoteIp);
            pack.userid = ctx.userid;
            // ////////////////////////////////////////////////////////
            if (ctx.error != 0) {
                pack.error = ctx.error;
            } else if (thr != null) {
                if (thr == REJECT) {
                    Logger.println("A145", ctx.serviceName);
                    String emsg = conf.control_reject_text;
                    pack.error = DataProxy.sendError(emsg);
                    ServiceSummary.getInstance().process(thr, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
                } else {
                    String emsg = thr.toString();
                    if (conf.profile_fullstack_service_error_enabled) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(emsg).append("\n");
                        ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                        thr = thr.getCause();
                        while (thr != null) {
                            sb.append("\nCause...\n");
                            ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                            thr = thr.getCause();
                        }
                        emsg = sb.toString();
                    }
                    pack.error = DataProxy.sendError(emsg);
                    ServiceSummary.getInstance().process(thr, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
                }
            } else if (ctx.userTransaction  > 0 && conf.xlog_error_check_user_transaction_enabled) {
                pack.error = DataProxy.sendError("Missing Commit/Rollback Error");
                ServiceSummary.getInstance().process(userTxNotClose, pack.error, ctx.serviceHash, ctx.txid, 0, 0);
            }
            if (ctx.group != null) {
                pack.group = DataProxy.sendGroup(ctx.group);
            }
            pack.userAgent = ctx.userAgent;
            pack.referer = ctx.referer;
            // 2015.02.02
            pack.apicallCount = ctx.apicall_count;
            pack.apicallTime = ctx.apicall_time;
            pack.caller = ctx.caller;
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
            metering(pack);
            if (sendOk) {
                DataProxy.sendXLog(pack);
            }
        } catch (Throwable e) {
            Logger.println("A146", e);
        }
    }

    public static void metering(XLogPack pack) {
        switch (pack.xType) {
            case XLogTypes.WEB_SERVICE:
            case XLogTypes.APP_SERVICE:
                MeterService.getInstance().add(pack.elapsed, pack.sqlTime, pack.apicallTime, pack.error != 0);
                ServiceSummary.getInstance().process(pack);
                break;
            case XLogTypes.BACK_THREAD:
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

    public static Object startService(String name, String className, String methodName, String methodDesc, Object _this,
                                      Object[] arg, byte xType) {
        try {
            TraceContext ctx = TraceContextManager.getContext();
            if (ctx != null) {
                return null;
            }
            Configure conf = Configure.getInstance();
            ctx = new TraceContext(conf.profile_summary_mode_enabled);
            String service_name = name;
            ctx.thread = Thread.currentThread();
            ctx.serviceHash = HashUtil.hash(service_name);
            ctx.serviceName = service_name;
            ctx.startTime = System.currentTimeMillis();
            ctx.startCpu = SysJMX.getCurrentThreadCPU();
            ctx.txid = KeyGen.next();
            ctx.threadId = TraceContextManager.start(ctx.thread, ctx);
            ctx.bytes = SysJMX.getCurrentThreadAllocBytes();
            ctx.profile_thread_cputime = conf.profile_thread_cputime_enabled;
            ctx.xType = xType;
            PluginAppServiceTrace.start(ctx, new HookArgs(className, methodName, methodDesc, _this, arg));
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
                return;
            }
            TraceContext ctx = localCtx.context;
            if (ctx == null) {
                return;
            }
            if (ctx.xType == XLogTypes.BACK_THREAD) {
                MethodStep2 step = (MethodStep2) localCtx.stepSingle;
                step.elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - step.start_time;
                if (ctx.profile_thread_cputime) {
                    step.cputime = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu) - step.start_cpu;
                }
                step.error = errorCheck(ctx, thr);
                ctx.profile.pop(step);
                PluginAppServiceTrace.end(ctx);
                TraceContextManager.end(ctx.threadId);
                ctx.profile.close(true);
                return;
            }
            PluginAppServiceTrace.end(ctx);
            TraceContextManager.end(ctx.threadId);

            XLogPack pack = new XLogPack();
            pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
            // pack.endTime = System.currentTimeMillis();
            pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
            boolean sendOk = pack.elapsed >= Configure.getInstance().xlog_lower_bound_time_ms;
            ctx.profile.close(sendOk);
            DataProxy.sendServiceName(ctx.serviceHash, ctx.serviceName);
            pack.service = ctx.serviceHash;
            pack.xType = ctx.xType;
            pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
            pack.bytes = (int) (SysJMX.getCurrentThreadAllocBytes() - ctx.bytes);
            pack.status = ctx.status;
            pack.sqlCount = ctx.sqlCount;
            pack.sqlTime = ctx.sqlTime;
            pack.txid = ctx.txid;
            pack.gxid = ctx.gxid;
            pack.caller = ctx.caller;
            pack.ipaddr = IPUtil.toBytes(ctx.remoteIp);
            pack.userid = ctx.userid;
            pack.error = errorCheck(ctx, thr);
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
            metering(pack);
            if (sendOk) {
                DataProxy.sendXLog(pack);
            }
        } catch (Throwable t) {
            Logger.println("A148", "service end error", t);
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
                thr = thr.getCause();
                while (thr != null) {
                    sb.append("\nCause...\n");
                    ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_max_lines);
                    thr = thr.getCause();
                }
                emsg = sb.toString();
            }
            error = DataProxy.sendError(emsg);
            ServiceSummary.getInstance().process(thr, error, ctx.serviceHash, ctx.txid, 0, 0);
        } else if (ctx.userTransaction  > 0 && conf.xlog_error_check_user_transaction_enabled) {
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
        if (conf.profile_method_enabled == false)
            return null;
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            if (conf._trace_auto_service_enabled) {
                Object localContext = startService(classMethod, null, null, null, null, null, XLogTypes.APP_SERVICE);
                //startService내부에서 에러가 나는 경우(발생하면 안됨)
                //Null이 리턴될 수 있음(방어코드)
                //@skyworker
                if (localContext != null && conf._trace_auto_service_backstack_enabled) {
                    String stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2);
                    AutoServiceStartAnalyzer.put(classMethod, stack);
                    MessageStep m = new MessageStep();
                    m.message = "SERVICE BACKSTACK:\n" + stack;
                    ((LocalContext) localContext).context.profile.add(m);
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
            endService(lctx.option, null, thr);
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

    public static void setServiceName(String name) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null || name == null)
            return;
        ctx.serviceName = name;
        ctx.serviceHash = HashUtil.hash(name);
    }

    public static void setStatus(int httpStatus) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null)
            return;
        ctx.status = httpStatus;
    }

    public static XLogPack txperf(long endtime, long txid, int service_hash, String serviceName, int elapsed, int cpu,
                                  int sqlCount, int sqlTime, String remoteAddr, String error, long visitor) {
        XLogPack pack = new XLogPack();
        pack.cpu = cpu;
        pack.endTime = endtime;
        pack.elapsed = elapsed;
        DataProxy.sendServiceName(service_hash, serviceName);
        pack.service = service_hash;
        pack.bytes = 0;
        pack.status = 0;
        pack.sqlCount = sqlCount;
        pack.sqlTime = sqlTime;
        pack.txid = txid;
        pack.ipaddr = IPUtil.toBytes(remoteAddr);
        pack.userid = visitor;
        if (error != null) {
            pack.error = DataProxy.sendError(error);
        }
        MeterService.getInstance().add(pack.elapsed, pack.sqlTime, pack.apicallTime, error != null);
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

    public static void ctxLookup(Object this1, Object ctx) {
        if (ctx instanceof DataSource) {
            LoadedContext.put((DataSource) ctx);
        }
    }
}
