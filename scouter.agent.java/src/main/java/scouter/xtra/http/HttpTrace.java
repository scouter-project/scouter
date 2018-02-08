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

import scouter.agent.Configure;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.summary.EndUserAjaxData;
import scouter.agent.summary.EndUserErrorData;
import scouter.agent.summary.EndUserNavigationData;
import scouter.agent.summary.EndUserSummary;
import scouter.agent.trace.*;
import scouter.lang.conf.ConfObserver;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.util.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import static scouter.agent.AgentCommonConstant.*;

public class HttpTrace implements IHttpTrace {
    boolean remote_by_header;
    boolean __ip_dummy_test;
    String http_remote_ip_header_key;

    public static String[] ipRandom = {"27.114.0.121", "58.3.128.121",
            "101.53.64.121", "125.7.128.121", "202.68.224.121", "62.241.64.121", "86.63.224.121", "78.110.176.121",
            "84.18.128.121", "95.142.176.121", "61.47.128.121", "110.76.32.121", "116.251.64.121", "123.150.0.121",
            "125.254.128.121", "5.134.32.0", "5.134.32.121", "52.119.0.121", "154.0.128.121", "190.46.0.121"};

    public HttpTrace() {
        Configure conf = Configure.getInstance();
        this.http_remote_ip_header_key = conf.trace_http_client_ip_header_key;
        this.remote_by_header = !StringUtil.isEmpty(this.http_remote_ip_header_key);
        this.__ip_dummy_test = conf.__ip_dummy_test;

        ConfObserver.add(HttpTrace.class.getName(), new Runnable() {
            public void run() {
                String x = Configure.getInstance().trace_http_client_ip_header_key;
                if (CompareUtil.equals(x, http_remote_ip_header_key) == false) {
                    remote_by_header = StringUtil.isEmpty(x) == false;
                    http_remote_ip_header_key = x;
                }
            }
        });
    }

    public String getParameter(Object req, String key) {
        HttpServletRequest request = (HttpServletRequest) req;

        String ctype = request.getContentType();
        if (ctype != null && ctype.startsWith("application/x-www-form-urlencoded"))
            return null;

        return request.getParameter(key);
    }

    public String getHeader(Object req, String key) {
        HttpServletRequest request = (HttpServletRequest) req;
        return request.getHeader(key);
    }

    public void start(TraceContext ctx, Object req, Object res) {
        Configure conf = Configure.getInstance();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        ctx.serviceName = getRequestURI(request);
        ctx.serviceHash = HashUtil.hash(ctx.serviceName);

        if (ctx.serviceHash == conf.getEndUserPerfEndpointHash()) {
            ctx.isStaticContents = true;
            processEndUserData(request);
            return;
        }

        ctx.isStaticContents = TraceMain.isStaticContents(ctx.serviceName);

        ctx.http_method = request.getMethod();
        ctx.http_query = request.getQueryString();
        ctx.http_content_type = request.getContentType();

        ctx.remoteIp = getRemoteAddr(request);

        TransferMap.ID transferId = (TransferMap.ID)request.getAttribute(REQUEST_ATTRIBUTE_CALLER_TRANSFER_MAP);
        request.setAttribute(REQUEST_ATTRIBUTE_CALLER_TRANSFER_MAP, null);
//        System.out.println("[scouter][http-start]transferId:thread: " + transferId + " ::: " + Thread.currentThread().getName());
//        System.out.println("[scouter][http-start]url: " + ctx.serviceName);

        if(transferId != null) {
            if(transferId.gxid !=0) ctx.gxid = transferId.gxid;
            if(transferId.callee !=0) ctx.txid = transferId.callee;
            if(transferId.caller !=0) ctx.caller = transferId.caller;
            ctx.xType = transferId.xType;
            if(ctx.xType == XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE) {
                TraceContext callerCtx = TraceContextManager.getDeferredContext(ctx.caller);
                StringBuilder sb = new StringBuilder(ctx.serviceName.length()*3);
                sb.append(ASYNC_SERVLET_DISPATCHED_PREFIX);
                if (Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTRIBUTE_SELF_DISPATCHED))) {
                    request.setAttribute(REQUEST_ATTRIBUTE_SELF_DISPATCHED, false);
                    sb.append("[self]");
                    if (callerCtx != null) sb.append(callerCtx.serviceName);
                    ctx.serviceName = sb.toString();
                } else {
                    if (callerCtx != null) sb.append(callerCtx.serviceName).append(":/");
                    ctx.serviceName = sb.append(ctx.serviceName).toString();
                }
            }
        }

        try {
            switch (conf.trace_user_mode) {
                case 3:
                    ctx.userid = UseridUtil.getUseridFromHeader(request, response, conf.trace_user_session_key);
                    if (ctx.userid == 0 && ctx.remoteIp != null) {
                        ctx.userid = HashUtil.hash(ctx.remoteIp);
                    }
                    break;
                case 2:
                    ctx.userid = UseridUtil.getUserid(request, response, conf.trace_user_cookie_path);
                    break;
                case 1:
                    ctx.userid = UseridUtil.getUseridCustom(request, response, conf.trace_user_session_key);
                    if (ctx.userid == 0 && ctx.remoteIp != null) {
                        ctx.userid = HashUtil.hash(ctx.remoteIp);
                    }
                    break;
                default:
                    if (ctx.remoteIp != null) {
                        ctx.userid = HashUtil.hash(ctx.remoteIp);
                    }
                    break;
            }
            MeterUsers.add(ctx.userid);
        } catch (Throwable e) {
            // ignore
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            ctx.referer = DataProxy.sendReferer(referer);
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            ctx.userAgent = DataProxy.sendUserAgent(userAgent);
            ctx.userAgentString = userAgent;
        }
        dump(ctx.profile, request, ctx);
        if (conf.trace_interservice_enabled && transferId == null) {
            try {
                String gxid = request.getHeader(conf._trace_interservice_gxid_header_key);
                if (gxid != null) {
                    ctx.gxid = Hexa32.toLong32(gxid);
                }
                String txid = request.getHeader(conf._trace_interservice_callee_header_key);
                if (txid != null) {
                    ctx.txid = Hexa32.toLong32(txid);
                    ctx.is_child_tx = true;
                }
                String caller = request.getHeader(conf._trace_interservice_caller_header_key);
                if (caller != null) {
                    ctx.caller = Hexa32.toLong32(caller);
                    ctx.is_child_tx = true;
                }
            } catch (Throwable t) {
            }
        }

        if (conf.trace_response_gxid_enabled && !ctx.isStaticContents) {
            try {
                if (ctx.gxid == 0)
                    ctx.gxid = ctx.txid;

                String resGxId = Hexa32.toString32(ctx.gxid) + ":" + ctx.startTime;
                response.setHeader(conf._trace_interservice_gxid_header_key, resGxId);

                Cookie c = new Cookie(conf._trace_interservice_gxid_header_key, resGxId);
                response.addCookie(c);

            } catch (Throwable t) {
            }
        }

        //deprecated !
        if (conf.trace_webserver_enabled) {
            try {
                ctx.web_name = request.getHeader(conf.trace_webserver_name_header_key);
                String web_time = request.getHeader(conf.trace_webserver_time_header_key);
                if (web_time != null) {
                    int x = web_time.indexOf("t=");
                    if (x >= 0) {
                        web_time = web_time.substring(x + 2);
                        x = web_time.indexOf(' ');
                        if (x > 0) {
                            web_time = web_time.substring(0, x);
                        }
                        ctx.web_time = (int) (System.currentTimeMillis() - (Long.parseLong(web_time) / 1000));
                    }
                }
            } catch (Throwable t) {
            }
        }

        //check queuing from front proxy
        if (conf.trace_request_queuing_enabled) {
            try {
                ctx.queuingHost = request.getHeader(conf.trace_request_queuing_start_host_header);
                String startTime = request.getHeader(conf.trace_request_queuing_start_time_header);
                if (startTime != null) {
                    int t = startTime.indexOf("t=");
                    int ts = startTime.indexOf("ts=");
                    long startMillis = 0l;
                    if (t >= 0) {
                        startMillis = Long.parseLong(startTime.substring(t + 2).trim())/1000;
                    } else if (ts >= 0) {
                        startMillis = Long.parseLong(startTime.substring(ts + 3).replace(".", ""));
                    }

                    if (startMillis > 0) {
                        ctx.queuingTime = (int) (System.currentTimeMillis() - startMillis);
                    }
                }

                ctx.queuing2ndHost = request.getHeader(conf.trace_request_queuing_start_2nd_host_header);
                startTime = request.getHeader(conf.trace_request_queuing_start_2nd_time_header);
                if (startTime != null) {
                    int t = startTime.indexOf("t=");
                    int ts = startTime.indexOf("ts=");
                    long startMillis = 0l;
                    if (t >= 0) {
                        startMillis = Long.parseLong(startTime.substring(t + 2).trim())/1000;
                    } else if (ts >= 0) {
                        startMillis = Long.parseLong(startTime.substring(ts + 3).replace(".", ""));
                    }

                    if (startMillis > 0) {
                        ctx.queuing2ndTime = (int) (System.currentTimeMillis() - startMillis);
                    }
                }
            } catch (Throwable t) {
            }
        }
    }

    private void processEndUserData(HttpServletRequest request) {
        EndUserNavigationData nav;
        EndUserErrorData err;
        EndUserAjaxData ajax;

        if ("err".equals(request.getParameter("p"))) {
            EndUserErrorData data = new EndUserErrorData();

            data.count = 1;
            data.stacktrace = DataProxy.sendError(StringUtil.nullToEmpty(request.getParameter("stacktrace")));
            data.userAgent = DataProxy.sendUserAgent(StringUtil.nullToEmpty(request.getParameter("userAgent")));
            data.host = DataProxy.sendServiceName(StringUtil.nullToEmpty(request.getParameter("host")));
            data.uri = DataProxy.sendServiceName(StringUtil.nullToEmpty(request.getParameter("uri")));
            data.message = DataProxy.sendError(StringUtil.nullToEmpty(request.getParameter("message")));
            data.name = DataProxy.sendError(StringUtil.nullToEmpty(request.getParameter("name")));
            data.file = DataProxy.sendServiceName(StringUtil.nullToEmpty(request.getParameter("file")));
            data.lineNumber = CastUtil.cint(request.getParameter("lineNumber"));
            data.columnNumber = CastUtil.cint(request.getParameter("columnNumber"));

            //Logger.println("@ input error data -> print");
            //Logger.println(data);

            EndUserSummary.getInstance().process(data);

        } else if ("nav".equals(request.getParameter("p"))) {

        } else if ("ax".equals(request.getParameter("p"))) {

        }

        //EndUserSummary.getInstance().process(p);

    }

    private String getRequestURI(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null)
            return "no-url";
        int x = uri.indexOf(';');
        if (x > 0)
            return uri.substring(0, x);
        else
            return uri;
    }

    private String getRemoteAddr(HttpServletRequest request) {
        try {
            //For Testing
            if (__ip_dummy_test) {
                return getRandomIp();
            }
            
            if (remote_by_header) {
            	String remoteIp = request.getHeader(http_remote_ip_header_key);

                int commaPos = remoteIp.indexOf(',');
                if (remoteIp != null && commaPos > -1) {
                	remoteIp = remoteIp.substring(0, commaPos);
                }
            	
                return remoteIp;
            } else {
                return request.getRemoteAddr();
            }

        } catch (Throwable t) {
            remote_by_header = false;
            return "0.0.0.0";
        }
    }

    private String getRandomIp() {
        int len = ipRandom.length;
        int randomNum = (int) (Math.random() * (len-1));
        return ipRandom[randomNum];
    }

    public void end(TraceContext ctx, Object req, Object res) {
        Configure conf = Configure.getInstance();

        HttpServletRequest request = (HttpServletRequest) req;
        if (conf.profile_http_parameter_enabled) {
            if (conf.profile_http_parameter_url_prefix == null || ctx.serviceName.indexOf(conf.profile_http_parameter_url_prefix) >= 0) {
                String ctype = request.getContentType();
                if (ctype != null && ctype.indexOf("multipart") >= 0)
                    return;

                Enumeration en = request.getParameterNames();
                if (en != null) {
                    int start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                    while (en.hasMoreElements()) {
                        String key = (String) en.nextElement();
                        String value = new StringBuilder().append("parameter: ").append(key).append("=")
                                .append(StringUtil.limiting(request.getParameter(key), 1024)).toString();

                        MessageStep step = new MessageStep(value);
                        step.start_time = start_time;
                        // step.start_cpu = (int) (SysJMX.getCurrentThreadCPU()
                        // - ctx.startCpu);

                        ctx.profile.add(step);
                    }
                }
            }
        }
        // HttpServletRequest request = (HttpServletRequest)req;
        // HttpServletResponse response = (HttpServletResponse)res;
    }

    private static void dump(IProfileCollector p, HttpServletRequest request, TraceContext ctx) {
        Configure conf = Configure.getInstance();
        if (conf.profile_http_querystring_enabled) {
            String msg = request.getMethod() + " ?" + StringUtil.trimToEmpty(request.getQueryString());
            MessageStep step = new MessageStep(msg);
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            p.add(step);
        }
        if (conf.profile_http_header_enabled) {
            if (conf.profile_http_header_url_prefix == null || ctx.serviceName.indexOf(conf.profile_http_header_url_prefix) >= 0) {
                Enumeration en = request.getHeaderNames();
                if (en != null) {
                    int start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                    while (en.hasMoreElements()) {
                        String key = (String) en.nextElement();
                        if (conf._profile_http_header_keys != null
                                && conf._profile_http_header_keys.size() > 0
                                && !conf._profile_http_header_keys.contains(key.toUpperCase())) {
                            continue;
                        }
                        String value = new StringBuilder().append("header: ").append(key).append("=")
                                .append(StringUtil.limiting(request.getHeader(key), 1024)).toString();

                        MessageStep step = new MessageStep(value);
                        step.start_time = start_time;

                        p.add(step);
                    }
                }
            }
        }

        if (conf.profile_http_parameter_enabled) {
            HashedMessageStep step = new HashedMessageStep();
            step.hash = DataProxy.sendHashedMessage("[HTTP parameters] will be shown in the last of this profile if available.(profile_http_parameter_enabled : true)");
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            step.time = -1;
            ctx.profile.add(step);
        }
    }

    public void rejectText(Object res, String text) {
        HttpServletResponse response = (HttpServletResponse) res;
        try {
            PrintWriter pw = response.getWriter();
            pw.println(text);
        } catch (IOException e) {
        }
    }

    public void rejectUrl(Object res, String url) {
        HttpServletResponse response = (HttpServletResponse) res;
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
        }
    }

    public void addAsyncContextListener(Object ac) {
        return;
    }

    public TraceContext getTraceContextFromAsyncContext(Object oAsyncContext) {
        return null;
    }

    public void setDispatchTransferMap(Object oAsyncContext, long gxid, long caller, long callee, byte xType) {
        return;
    }

    public void setSelfDispatch(Object oAsyncContext, boolean self) {
        return;
    }

    public boolean isSelfDispatch(Object oAsyncContext) {
        return false;
    }
}
