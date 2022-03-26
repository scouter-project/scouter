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

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.trace.IProfileCollector;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceMain;
import scouter.agent.trace.XLogSampler;
import scouter.lang.conf.ConfObserver;
import scouter.lang.constants.B3Constant;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.util.CompareUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouter.util.zipkin.HexCodec;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WebfluxHttpTrace implements IHttpTrace {
    boolean remote_by_header;
    String http_remote_ip_header_key;

    public WebfluxHttpTrace() {
        Configure conf = Configure.getInstance();
        this.http_remote_ip_header_key = conf.trace_http_client_ip_header_key;
        this.remote_by_header = !StringUtil.isEmpty(this.http_remote_ip_header_key);

        ConfObserver.add(WebfluxHttpTrace.class.getName(), new Runnable() {
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
        ServerHttpRequest request = (ServerHttpRequest) req;
        if (MediaType.APPLICATION_FORM_URLENCODED.equals(request.getHeaders().getContentType()))
            return null;

        return request.getQueryParams().getFirst(key);
    }

    private static String getMethod(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        return method != null ? method.toString() : null;
    }

    private static String getQuery(ServerHttpRequest request) {
        URI uri = request.getURI();
        return uri != null ? uri.getQuery() : null;
    }

    private static String getContentType(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null ? contentType.toString() : null;
    }

    private static String getRequestURI(ServerHttpRequest request) {
        URI uri = request.getURI();
        if (uri == null)
            return "no-url";

        String path = uri.getPath();
        if (path == null)
            return "no-url";

        return path;
    }

    private static String getHeader(ServerHttpRequest request, String key) {
        HttpHeaders headers = request.getHeaders();
        if (headers == null) {
            return null;
        }
        return headers.getFirst(key);
    }

    private String getRemoteAddr(ServerHttpRequest request) {
        try {
            if (remote_by_header) {
                String remoteIp = request.getHeaders().getFirst(http_remote_ip_header_key);
                if (remoteIp == null) {
                    return request.getRemoteAddress().getAddress().getHostAddress();
                }

                int commaPos = remoteIp.indexOf(',');
                if (commaPos > -1) {
                    remoteIp = remoteIp.substring(0, commaPos);
                }
                Logger.trace("remoteIp: " + remoteIp);
                return remoteIp;

            } else {
                return request.getRemoteAddress().getAddress().getHostAddress();
            }

        } catch (Throwable t) {
            remote_by_header = false;
            return "0.0.0.0";
        }
    }

    public String getHeader(Object req, String key) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        return getHeader(request, key);
    }

    @Override
    public String getCookie(Object req, String key) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        HttpCookie first = request.getCookies().getFirst(key);
        return first != null ? first.getValue() : null;
    }

    @Override
    public String getRequestURI(Object req) {
        return getRequestURI((ServerHttpRequest) req);
    }

    @Override
    public String getRequestId(Object req) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        return request.getId();
    }

    @Override
    public String getRemoteAddr(Object req) {
        return getRemoteAddr((ServerHttpRequest) req);
    }

    @Override
    public String getMethod(Object req) {
        return getMethod((ServerHttpRequest) req);
    }

    @Override
    public String getQueryString(Object req) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        URI uri = request.getURI();
        return uri != null ? uri.getQuery() : null;
    }

    @Override
    public Object getAttribute(Object req, String key) {
        return null;
    }

    @Override
    public Enumeration getParameterNames(Object req) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        Set<String> strings = request.getQueryParams().keySet();
        return Collections.enumeration(strings);
    }

    @Override
    public Enumeration getHeaderNames(Object req) {
        ServerHttpRequest request = (ServerHttpRequest) req;
        Set<String> strings = request.getHeaders().keySet();
        return Collections.enumeration(strings);
    }

    public void start(TraceContext ctx, Object req, Object res) {
        Configure conf = Configure.getInstance();
        ServerHttpRequest request = (ServerHttpRequest) req;
        ServerHttpResponse response = (ServerHttpResponse) res;

        ctx.serviceName = getRequestURI(request);
        ctx.serviceHash = HashUtil.hash(ctx.serviceName);

        if (ctx.serviceHash == conf.getEndUserPerfEndpointHash()) {
            ctx.isStaticContents = true;
            //TODO processEndUserData(request);
            return;
        }

        if (XLogSampler.getInstance().isFullyDiscardServicePattern(ctx.serviceName)) {
            ctx.isFullyDiscardService = true;
            return;
        }

        ctx.isStaticContents = TraceMain.isStaticContents(ctx.serviceName);

        ctx.http_method = getMethod(request);
        ctx.http_query = getQuery(request);
        ctx.http_content_type = getContentType(request);
        ctx.remoteIp = getRemoteAddr(request);

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
        String referer = getHeader(request, "Referer");
        if (referer != null) {
            ctx.referer = DataProxy.sendReferer(referer);
        }
        String userAgent = getHeader(request, "User-Agent");
        if (userAgent != null) {
            ctx.userAgent = DataProxy.sendUserAgent(userAgent);
            ctx.userAgentString = userAgent;
        }
        dump(ctx.profile, request, ctx);
        if (conf.trace_interservice_enabled) {
            try {
                boolean b3ModeValid = false;
                String b3TraceId = getHeader(request, B3Constant.B3_HEADER_TRACEID);
                String gxid = getHeader(request, conf._trace_interservice_gxid_header_key);

                if (gxid != null) {
                    ctx.gxid = Hexa32.toLong32(gxid);
                } else {
                    if (b3TraceId != null && !b3TraceId.equals("0")) {
                        b3ModeValid = true;
                    }
                }

                if (b3ModeValid && conf.trace_propagete_b3_header) {
                    ctx.gxid = HexCodec.lowerHexToUnsignedLong(b3TraceId);
                    ctx.txid = HexCodec.lowerHexToUnsignedLong(getHeader(request, B3Constant.B3_HEADER_SPANID));
                    String caller = getHeader(request, B3Constant.B3_HEADER_PARENTSPANID);
                    if (caller != null) {
                        ctx.caller = HexCodec.lowerHexToUnsignedLong(caller);
                        ctx.is_child_tx = true;
                    }
                    ctx.b3Mode = true;
                    ctx.b3Traceid = b3TraceId;

                } else {
                    String txid = getHeader(request, conf._trace_interservice_callee_header_key);
                    if (txid != null) {
                        ctx.txid = Hexa32.toLong32(txid);
                        ctx.is_child_tx = true;
                    }
                    String caller = getHeader(request, conf._trace_interservice_caller_header_key);
                    if (caller != null) {
                        ctx.caller = Hexa32.toLong32(caller);
                        ctx.is_child_tx = true;
                    }
                    String callerObjHashStr = getHeader(request, conf._trace_interservice_caller_obj_header_key);
                    if (callerObjHashStr != null) {
                        try {
                            ctx.callerObjHash = Integer.parseInt(callerObjHashStr);
                        } catch (NumberFormatException e) {
                        }
                        ctx.is_child_tx = true;
                    }
                }
            } catch (Throwable t) {
                Logger.println("Z101", "check propergation: " + t.getMessage());
            }

            if (ctx.is_child_tx) {
                response.getHeaders().add(conf._trace_interservice_callee_obj_header_key, String.valueOf(conf.getObjHash()));
            }
        }

        if (conf.trace_response_gxid_enabled && !ctx.isStaticContents) {
            try {
                if (ctx.gxid == 0)
                    ctx.gxid = ctx.txid;

                String resGxId = Hexa32.toString32(ctx.gxid) + ":" + ctx.startTime;
                response.getHeaders().add(conf._trace_interservice_gxid_header_key, resGxId);

                ResponseCookie c = ResponseCookie.from(conf._trace_interservice_gxid_header_key, resGxId).build();
                response.addCookie(c);

            } catch (Throwable t) {
            }
        }

        //check queuing from front proxy
        if (conf.trace_request_queuing_enabled) {
            try {
                ctx.queuingHost = getHeader(request, conf.trace_request_queuing_start_host_header);
                String startTime = getHeader(request, conf.trace_request_queuing_start_time_header);
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

                ctx.queuing2ndHost = getHeader(request, conf.trace_request_queuing_start_2nd_host_header);
                startTime = getHeader(request, conf.trace_request_queuing_start_2nd_time_header);
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

    public void end(TraceContext ctx, Object req, Object res) {
        Configure conf = Configure.getInstance();

        ServerHttpRequest request = (ServerHttpRequest) req;

        if (conf.profile_http_parameter_enabled) {
            if (conf.profile_http_parameter_url_prefix == null || ctx.serviceName.indexOf(conf.profile_http_parameter_url_prefix) >= 0) {
                String ctype = getContentType(request);
                if (ctype != null && ctype.indexOf("multipart") >= 0)
                    return;

                Enumeration en = getParameterNames(request);
                if (en != null) {
                    int start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                    while (en.hasMoreElements()) {
                        String key = (String) en.nextElement();
                        String value = new StringBuilder().append("parameter: ").append(key).append("=")
                                .append(StringUtil.limiting(getParameter(request, key), 1024)).toString();

                        MessageStep step = new MessageStep(value);
                        step.start_time = start_time;
                        ctx.profile.add(step);
                    }
                }
            }
        }
    }

    private static void dump(IProfileCollector p, ServerHttpRequest request, TraceContext ctx) {
        Configure conf = Configure.getInstance();
        if (conf.profile_http_querystring_enabled) {
            String msg = request.getMethod() + " ?" + StringUtil.trimToEmpty(getQuery(request));
            MessageStep step = new MessageStep(msg);
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            p.add(step);
        }
        if (conf.profile_http_header_enabled) {
            if (conf.profile_http_header_url_prefix == null || ctx.serviceName.indexOf(conf.profile_http_header_url_prefix) >= 0) {
                Set<Map.Entry<String, List<String>>> entries = request.getHeaders().entrySet();
                if (entries != null) {
                    int start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                    Iterator<Map.Entry<String, List<String>>> iterator = entries.iterator();
                    while (iterator.hasNext()) {
                        for (int i = 0; i < entries.size(); i++) {
                            Map.Entry<String, List<String>> entry = iterator.next();
                            if (conf._profile_http_header_keys != null
                                    && conf._profile_http_header_keys.size() > 0
                                    && !conf._profile_http_header_keys.contains(entry.getKey().toUpperCase())) {
                                continue;
                            }
                            if (entry.getValue() != null) {
                                for (int j = 0; j < entry.getValue().size(); j++) {
                                    String value = new StringBuilder().append("header: ").append(entry.getKey()).append("=")
                                            .append(StringUtil.limiting(entry.getValue().get(j), 1024)).toString();

                                    MessageStep step = new MessageStep(value);
                                    step.start_time = start_time;

                                    p.add(step);
                                }
                            }
                        }

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
        ServerHttpResponse response = (ServerHttpResponse) res;
        try {
            //TODO
            response.setRawStatusCode(400);
        } catch (Exception e) {
        }
    }

    public void rejectUrl(Object res, String url) {
        ServerHttpResponse response = (ServerHttpResponse) res;
        try {
            //TODO
            response.setRawStatusCode(400);
        } catch (Exception e) {
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
