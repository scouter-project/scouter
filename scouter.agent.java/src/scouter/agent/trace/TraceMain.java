/*
 *  Copyright 2015 LG CNS.
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
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.HttpServiceTracePlugIn;
import scouter.agent.plugin.ServiceTracePlugIn;
import scouter.agent.proxy.HttpTraceFactory;
import scouter.agent.proxy.IHttpTrace;
import scouter.lang.AlertLevel;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.util.ArrayUtil;
import scouter.util.HashUtil;
import scouter.util.IPUtil;
import scouter.util.KeyGen;
import scouter.util.ObjectUtil;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

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

	public static Object startHttpService(Object req, Object res) {
		try {
			TraceContext ctx = TraceContextManager.getLocalContext();
			if (ctx != null) {
				if (ctx.done_http_service == false) {
					ctx.done_http_service = true;
					addSeviceName(ctx, req);
				}
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
			TraceContext ctx = TraceContextManager.getLocalContext();
			if (ctx != null) {
				return null;
			}
			return startHttp(req, res);
		} catch (Throwable t) {
			Logger.println("A144", "fail to deploy ", t);
		}
		return null;
	}

	private static Error REJECT = new Error();

	public static Object reject(Object stat, Object req, Object res) {
		Configure conf = Configure.getInstance();
		if (conf.enable_reject_service) {
			if (stat == null || req == null || res == null)
				return null;
			if (http == null) {
				initHttp(req);
			}

			Stat stat0 = (Stat) stat;
			if (stat0.isStaticContents)
				return null;

			if (HttpServiceTracePlugIn.reject(stat0.ctx, req, res) // reject by
																	// customized
																	// plugin
					|| TraceContextManager.size() > conf.max_active_service) {// reject
																				// by
																				// max_active_service
				// howto reject
				if (conf.enable_reject_url) {
					http.rejectUrl(res, conf.reject_url); // by url
				} else {
					http.rejectText(res, conf.reject_text);// just message
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
			if (conf.service_post_key != null) {
				String v = http.getParameter(req, conf.service_post_key);
				if (v != null) {
					if (sb.length() == 0) {
						sb.append(ctx.serviceName);
						sb.append('?').append(conf.service_post_key).append("=").append(v);
					} else {
						sb.append('&').append(conf.service_post_key).append("=").append(v);
					}
				}
			}
			if (conf.service_get_key != null && ctx.http_query != null) {
				int x = ctx.http_query.indexOf(conf.service_get_key);
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
		TraceContext ctx = new TraceContext(conf.enable_profile_summary);
		ctx.thread = Thread.currentThread();
		ctx.txid = KeyGen.next();
		ctx.startTime = System.currentTimeMillis();
		ctx.startCpu = SysJMX.getCurrentThreadCPU();
		ctx.threadId = TraceContextManager.start(ctx.thread, ctx);
		ctx.bytes = SysJMX.getCurrentThreadAllocBytes();
		ctx.profile_thread_cputime = conf.profile_thread_cputime;
		http.start(ctx, req, res);
		if (ctx.serviceName == null)
			ctx.serviceName = "Non-URI";
		HttpServiceTracePlugIn.start(ctx, req, res);
		Stat stat = new Stat(ctx, req, res);
		stat.isStaticContents = isStaticContents(ctx.serviceName);
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
					TraceContext ctx = TraceContextManager.getLocalContext();
					if (ctx != null && ctx.error == 0) {
						Configure conf = Configure.getInstance();
						String emsg = thr.toString();
						AlertProxy.sendAlert(AlertLevel.ERROR, "SERVICE_ERROR", emsg);
						if (conf.profile_fullstack_service_error) {
							StringBuffer sb = new StringBuffer();
							sb.append(emsg).append("\n");
							ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
							thr = thr.getCause();
							while (thr != null) {
								sb.append("\nCause...\n");
								ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
								thr = thr.getCause();
							}
							emsg = sb.toString();
						}
						ctx.error = DataProxy.sendError(emsg);
					}
				} catch (Throwable t) {
				}
				return;
			}
			TraceContext ctx = stat0.ctx;
			http.end(ctx, stat0.req, stat0.res);
			TraceContextManager.end(ctx.threadId);
			Configure conf = Configure.getInstance();
			if (stat0.isStaticContents) {
				return;
			}
			XLogPack pack = new XLogPack();
			// pack.endTime = System.currentTimeMillis();
			pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
			boolean sendOk = pack.elapsed >= conf.xlog_time_limit;
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
			pack.ipaddr = ctx.remoteAddr;
			pack.userid = ctx.userid;
			// ////////////////////////////////////////////////////////
			if (ctx.error != 0) {
				pack.error = ctx.error;
			} else if (thr != null) {
				if (thr == REJECT) {
					Logger.println("A145", ctx.serviceName);
					AlertProxy.sendAlert(AlertLevel.ERROR, "SERVICE_REJECTED", ctx.serviceName);
					String emsg = conf.reject_text;
					pack.error = DataProxy.sendError(emsg);
				} else {
					String emsg = thr.toString();
					AlertProxy.sendAlert(AlertLevel.ERROR, "SERVICE_ERROR", emsg);
					if (conf.profile_fullstack_service_error) {
						StringBuffer sb = new StringBuffer();
						sb.append(emsg).append("\n");
						ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
						thr = thr.getCause();
						while (thr != null) {
							sb.append("\nCause...\n");
							ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
							thr = thr.getCause();
						}
						emsg = sb.toString();
					}
					pack.error = DataProxy.sendError(emsg);
				}
			} else if (conf.isErrorStatus(ctx.status)) {
				String emsg = "HttpStatus " + ctx.status;
				pack.error = DataProxy.sendError(emsg);
				AlertProxy.sendAlert(AlertLevel.ERROR, "HTTP_ERROR", emsg);
			}
			// pack.divPerf = ctx.divPerf;
			pack.userAgent = ctx.userAgent;
			pack.referer = ctx.referer;
			// 2015.02.02
			pack.apicallCount = ctx.apicall_count;
			pack.apicallTime = ctx.apicall_time;
			pack.caller = ctx.caller;

			if (ctx.login != null) {
				pack.login = DataProxy.sendLogin(ctx.login);
			}
			if (ctx.bizcode != null) {
				pack.biz = DataProxy.sendBizCode(ctx.bizcode);
			}
			metering(pack);
			HttpServiceTracePlugIn.end(ctx, pack);
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
			MeterService.getInstance().add(pack.elapsed, pack.error != 0);
			break;
		case XLogTypes.BACK_THREAD:
		}
	}

	private static boolean isStaticContents(String serviceName) {
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

	public static Object startService(String name, String className, String methodName, String methodDesc,
			Object _this, Object[] arg, byte xType) {
		try {
			TraceContext ctx = TraceContextManager.getLocalContext();
			if (ctx != null) {
				return null;
			}
			Configure conf = Configure.getInstance();
			ctx = new TraceContext(conf.enable_profile_summary);
			String service_name = name;
			ctx.thread = Thread.currentThread();
			ctx.serviceHash = HashUtil.hash(service_name);
			ctx.serviceName = service_name;
			ctx.startTime = System.currentTimeMillis();
			ctx.startCpu = SysJMX.getCurrentThreadCPU();
			ctx.txid = KeyGen.next();
			ctx.threadId = TraceContextManager.start(ctx.thread, ctx);
			ctx.bytes = SysJMX.getCurrentThreadAllocBytes();
			ctx.profile_thread_cputime = conf.profile_thread_cputime;
			ctx.xType = xType;
			ServiceTracePlugIn.start(ctx, new ApiInfo(className, methodName, methodDesc, _this, arg));
			return new Stat(ctx);
		} catch (Throwable t) {
			Logger.println("A147", t);
		}
		return null;
	}

	public static void endService(Object stat, Object returnValue, Throwable thr) {
		try {
			Stat stat0 = (Stat) stat;
			if (stat0 == null)
				return;
			TraceContext ctx = stat0.ctx;
			if (ctx == null) {
				return;
			}
			TraceContextManager.end(ctx.threadId);
			XLogPack pack = new XLogPack();
			pack.cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
			// pack.endTime = System.currentTimeMillis();
			pack.elapsed = (int) (System.currentTimeMillis() - ctx.startTime);
			boolean sendOk = pack.elapsed >= Configure.getInstance().xlog_time_limit;
			ctx.profile.close(sendOk);
			DataProxy.sendServiceName(ctx.serviceHash, ctx.serviceName);
			pack.service = ctx.serviceHash;
			pack.xType = ctx.xType;
			pack.bytes = (int) (SysJMX.getCurrentThreadAllocBytes() - ctx.bytes);
			pack.status = ctx.status;
			pack.sqlCount = ctx.sqlCount;
			pack.sqlTime = ctx.sqlTime;
			pack.txid = ctx.txid;
			pack.ipaddr = ctx.remoteAddr;
			pack.userid = ctx.userid;
			if (ctx.error != 0) {
				pack.error = ctx.error;
			} else if (thr != null) {
				Configure conf = Configure.getInstance();
				String emsg = thr.toString();
				if (conf.profile_fullstack_service_error) {
					StringBuffer sb = new StringBuffer();
					sb.append(emsg).append("\n");
					ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
					thr = thr.getCause();
					while (thr != null) {
						sb.append("\nCause...\n");
						ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
						thr = thr.getCause();
					}
					emsg = sb.toString();
				}
				pack.error = DataProxy.sendError(emsg);
			}
			// 2015.02.02
			pack.apicallCount = ctx.apicall_count;
			pack.apicallTime = ctx.apicall_time;

			if (ctx.login != null) {
				pack.login = DataProxy.sendLogin(ctx.login);
			}
			if (ctx.bizcode != null) {
				pack.biz = DataProxy.sendBizCode(ctx.bizcode);
			}

			ServiceTracePlugIn.end(ctx, pack);
			metering(pack);
			if (sendOk) {
				DataProxy.sendXLog(pack);
			}
		} catch (Throwable t) {
			Logger.println("A148", t);
		}
	}

	public static void capArgs(String className, String methodName, String methodDesc, Object[] arg) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return;
		MessageStep step = new MessageStep();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.message = toString("CAP-ARG", className, methodName, methodDesc, arg);
		ctx.profile.add(step);
	}

	public static void jspServlet(Object[] arg) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null || arg.length < 3)
			return;
		MessageStep step = new MessageStep();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.message = "JSP " + arg[2];
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
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return;
		MessageStep step = new MessageStep();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.message = toStringTHIS("CAP-THIS", className, methodDesc, this0);
		ctx.profile.add(step);
	}

	public static void capReturn(String className, String methodName, String methodDesc, Object rtn) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return;
		MessageStep step = new MessageStep();
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.message = toStringRTN("CAP-RTN", className, methodName, methodDesc, rtn);
		ctx.profile.add(step);
	}

	private static Configure conf = Configure.getInstance();

	public static Object startMethod(int hash, String classMethod) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			if (conf.enable_auto_service_trace) {
				Object stat = startService(classMethod, null, null, null, null, null, XLogTypes.BACK_THREAD);
				if (conf.enable_auto_service_backstack) {
					String stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 2);
					AutoServiceStartAnalizer.put(classMethod, stack);
					MessageStep m = new MessageStep();
					m.message = "SERVICE BACKSTACK:\n" + stack;
					((Stat) stat).ctx.profile.add(m);
				}
				return new LocalContext(stat);
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

	public static void endMethod(Object stat, Throwable thr) {
		if (stat == null)
			return;
		LocalContext lctx = (LocalContext) stat;
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

	// public static Object startDivPerf(int divIdx, String classMethod) {
	// TraceContext ctx = TraceContextManager.getLocalContext();
	// if (ctx == null)
	// return null;
	//
	// Configure conf = Configure.getInstance();
	// if (divIdx >= conf.divperf_size)
	// return null;
	//
	// // if (ctx.divPerf == null)
	// // ctx.divPerf = new int[conf.divperf_size];
	//
	// return new DivContext(ctx, System.currentTimeMillis(), divIdx);
	// }
	// public static void endDivPerf(Object stat, Throwable thr) {
	// if (stat == null)
	// return;
	// DivContext dctx = (DivContext) stat;
	// int time = (int) (System.currentTimeMillis() - dctx.stime);
	// dctx.context.divPerf[dctx.divIdx] += time;
	// }
	// /////////////////////
	public static void setStatus(int httpStatus) {
		TraceContext ctx = TraceContextManager.getLocalContext();
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
			AlertProxy.sendAlert(AlertLevel.ERROR, "SERVICE_EXCEPTION", error);
		}
		MeterService.getInstance().add(pack.elapsed, error != null);
		DataProxy.sendXLog(pack);
		MeterUsers.add(pack.userid);
		return pack;
	}

	public static void addMessage(String msg) {
		TraceContext ctx = TraceContextManager.getLocalContext();
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
}
