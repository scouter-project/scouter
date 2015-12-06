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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scouter.agent.Configure;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.summary.EndUserAjaxData;
import scouter.agent.summary.EndUserErrorData;
import scouter.agent.summary.EndUserNavigationData;
import scouter.agent.trace.IProfileCollector;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceMain;
import scouter.lang.conf.ConfObserver;
import scouter.lang.step.MessageStep;
import scouter.util.CompareUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

public class HttpTrace implements IHttpTrace {
	public HttpTrace() {
		Configure conf = Configure.getInstance();
		this.remote_by_header = StringUtil.isEmpty(conf.trace_http_client_ip_header_key) == false;
		this.http_remote_ip_header_key = conf.trace_http_client_ip_header_key;

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
		
		//
		if(ctx.serviceHash == conf.getEndUserPerfEndpointHash()){
			ctx.isStaticContents=true;
			enduser(request);
			return;
		}
		
		ctx.isStaticContents = TraceMain.isStaticContents(ctx.serviceName);

		
		ctx.http_method = request.getMethod();
		ctx.http_query = request.getQueryString();
		ctx.http_content_type = request.getContentType();

		ctx.remoteIp = getRemoteAddr(request);

		try {
			switch (conf.trace_user_mode) {
			case 2:
				ctx.userid = UseridUtil.getUserid(request, response);
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
		if (conf.trace_interservice_enabled) {
			try {
				String gxid = request.getHeader(conf.trace_interservice_gxid_header_key);
				if (gxid != null) {
					ctx.gxid = Hexa32.toLong32(gxid);
				}
				String txid = request.getHeader(conf.trace_interservice_callee_header_key);
				if (txid != null) {
					ctx.txid = Hexa32.toLong32(txid);
					ctx.is_child_tx = true;
				}
				String caller = request.getHeader(conf.trace_interservice_caller_header_key);
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
				response.setHeader(conf.trace_interservice_gxid_header_key, resGxId);

				Cookie c = new Cookie(conf.trace_interservice_gxid_header_key, resGxId);
				response.addCookie(c);

			} catch (Throwable t) {
			}
		}

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
	}

	private void enduser(HttpServletRequest request) {
		EndUserNavigationData nav;
		EndUserErrorData err;
		EndUserAjaxData ajax;
		
		
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

	private boolean remote_by_header;
	private String http_remote_ip_header_key;

	private String getRemoteAddr(HttpServletRequest request) {
		try {
			if (remote_by_header) {
				return request.getHeader(http_remote_ip_header_key);
			} else {
				return request.getRemoteAddr();
			}
		} catch (Throwable t) {
			remote_by_header = false;
			return "0.0.0.0";
		}
	}

	public void end(TraceContext ctx, Object req, Object res) {
		// HttpServletRequest request = (HttpServletRequest)req;
		// HttpServletResponse response = (HttpServletResponse)res;
		//
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
						String value = new StringBuilder().append("header: ").append(key).append("=")
								.append(StringUtil.limiting(request.getHeader(key), 1024)).toString();

						MessageStep step = new MessageStep(value);
						step.start_time = start_time;
						// step.start_cpu = (int) (SysJMX.getCurrentThreadCPU()
						// -
						// ctx.startCpu);

						p.add(step);
					}
				}
			}
		}
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

						p.add(step);
					}
				}
			}
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

	
	public static void main(String[] args) {
		System.out.println("http trace".indexOf(null));
	}

}