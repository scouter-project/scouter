/*
 *  Copyright 2015 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scouter.agent.Configure;
import scouter.agent.counter.meter.MeterUsers;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.IHttpTrace;
import scouter.agent.trace.IProfileCollector;
import scouter.agent.trace.TraceContext;
import scouter.io.DataInputX;
import scouter.lang.conf.ConfObserver;
import scouter.lang.step.MessageStep;
import scouter.util.CompareUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.StringUtil;

public class HttpTrace implements IHttpTrace {
	public HttpTrace() {
		Configure conf = Configure.getInstance();
		this.remote_by_header = StringUtil.isEmpty(conf.http_remote_ip_header_key) == false;
		this.http_remote_ip_header_key = conf.http_remote_ip_header_key;

		ConfObserver.add(HttpTrace.class.getName(), new Runnable() {
			public void run() {
				String x = Configure.getInstance().http_remote_ip_header_key;
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

		if (conf.service_header_key != null) {
			String v = request.getHeader(conf.service_header_key);
			ctx.serviceName = new StringBuilder(ctx.serviceName.length() + v.length() + 5).append(ctx.serviceName)
					.append('-').append(v).toString();
		}
		ctx.serviceHash = HashUtil.hash(ctx.serviceName);
		ctx.http_method = request.getMethod();
		ctx.http_query = request.getQueryString();
		ctx.http_content_type = request.getContentType();

		ctx.remoteAddr = IPUtil.toBytes(getRemoteAddr(request));
		try {
			switch (conf.mode_userid) {
			case 2:
				ctx.userid = UseridUtil.getUserid(request, response);
				break;
			case 1:
				ctx.userid = UseridUtil.getUseridCustom(request, response, conf.userid_jsessionid);
				if (ctx.userid == 0) {
					ctx.userid = DataInputX.toInt(ctx.remoteAddr, 0);
				}
				break;
			default:
				ctx.userid = DataInputX.toInt(ctx.remoteAddr, 0);
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
		}
		dump(ctx.profile, request, ctx);
		if (conf.enable_trace_e2e) {
			try {
				String gxid = request.getHeader(conf.gxid);
				if (gxid != null) {
					ctx.gxid = Hexa32.toLong32(gxid);
				}
				String txid = request.getHeader(conf.this_txid);
				if (txid != null) {
					ctx.txid = Hexa32.toLong32(txid);
					ctx.is_child_tx = true;
				}
				String caller = request.getHeader(conf.caller_txid);
				if (caller != null) {
					ctx.caller = Hexa32.toLong32(caller);
					ctx.is_child_tx = true;
				}
			} catch (Throwable t) {
			}
		}

		if (conf.enable_response_gxid) {
			try {
				if (ctx.gxid == 0)
					ctx.gxid = ctx.txid;
				response.setHeader(conf.gxid, Hexa32.toString32(ctx.gxid) + ":" + ctx.startTime);
			} catch (Throwable t) {
			}
		}
		if (conf.enable_trace_web) {
			try {
				ctx.web_name = request.getHeader(conf.key_web_name);
				String web_time = request.getHeader(conf.key_web_time);
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
		if (conf.http_debug_querystring) {
			String msg = request.getMethod() + " ?" + StringUtil.trimToEmpty(request.getQueryString());
			MessageStep step = new MessageStep(msg);
			step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			p.add(step);
		}
		if (conf.http_debug_header) {
			if (conf.http_debug_header_url == null || ctx.serviceName.indexOf(conf.http_debug_header_url) >= 0) {
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
		if (conf.http_debug_parameter) {
			if (conf.http_debug_parameter_url == null || ctx.serviceName.indexOf(conf.http_debug_parameter_url) >= 0) {
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