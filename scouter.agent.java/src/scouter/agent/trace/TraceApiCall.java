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
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.ApiCallTracePlugin;
import scouter.agent.plugin.HttpServiceTracePlugIn;
import scouter.lang.AlertLevel;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.SocketStep;
import scouter.util.HashUtil;
import scouter.util.IPUtil;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
public class TraceApiCall {
	public static class Stat {
		public TraceContext ctx;
		public Object req;
		public Object res;
		public Stat(TraceContext ctx, Object req, Object res) {
			this.ctx = ctx;
			this.req = req;
			this.res = res;
		}
		public Stat(TraceContext ctx) {
			this.ctx = ctx;
		}
	}
	static {
		try {
			HttpServiceTracePlugIn.class.getClass();
		} catch (Throwable t) {
		}
	}
	public static void apiInfo(String className, String methodName, String methodDesc, Object _this, Object[] arg) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null && arg.length >= 2) {
			ctx.apicall_target = arg[0] + ":" + arg[1];
			// System.out.println("apiinfo : " +ctx.apicall_name +
			// " target="+ctx.apicall_target);
		}
	}
	public static Object startApicall(String className, String methodName, String methodDesc, Object _this, Object[] arg) {
		try {
			TraceContext ctx = TraceContextManager.getLocalContext();
			if (ctx == null) {
				return null;
			}
			if (ctx.apicall_name != null) {
				return null;
			}
			// System.out.println("apicall start: " +ctx.apicall_name +
			// " target="+ctx.apicall_target);
			ApiInfo apiInfo = new ApiInfo(className, methodName, methodDesc, _this, arg);
			ApiCallStep step = ApiCallTracePlugin.start(ctx, apiInfo);
			if (step == null)
				return null;
			step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			if (ctx.profile_thread_cputime) {
				step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
			}
			ctx.profile.push(step);
			return new LocalContext(ctx, step, apiInfo);
		} catch (Throwable sss) {
			sss.printStackTrace();
		}
		return null;
	}
	public static Object startApicall(String name, long apiTxid) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null)
			return null;
		if (ctx.apicall_name != null) {
			return null;
		}
		ApiCallStep step = new ApiCallStep();
		step.txid = apiTxid;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		ctx.profile.push(step);
		ctx.apicall_name = name;
		return new LocalContext(ctx, step);
	}
	public static void endApicall(Object stat, Object returnValue, Throwable thr) {
		if (stat == null)
			return;
		try {
			LocalContext lctx = (LocalContext) stat;
			TraceContext tctx = (TraceContext) lctx.context;
			ApiCallStep step = (ApiCallStep) lctx.stepSingle;
			// System.out.println("apicall end: " +tctx.apicall_name +
			// " target="+tctx.apicall_target);
			if (step.address == null) {
				step.address = tctx.apicall_target;
			}
			step.hash = HashUtil.hash(tctx.apicall_name);
			DataProxy.sendApicall(step.hash, tctx.apicall_name);
			tctx.apicall_name = null;
			tctx.apicall_target = null;
			step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
			if (tctx.profile_thread_cputime) {
				step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
			}
			tctx.apicall_count++;
			tctx.apicall_time += step.elapsed;
			if (thr != null) {
				String msg = thr.getMessage();
				// AlertProxy.sendAlert(AlertLevel.ERROR, "SUBCALL_EXCEPTION",
				// msg);
				Configure conf = Configure.getInstance();
				if (conf.profile_fullstack_apicall_error) {
					
						StringBuffer sb = new StringBuffer();
						sb.append(msg).append("\n");
						ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
						thr = thr.getCause();
						while (thr != null) {
							sb.append("\nCause...\n");
							ThreadUtil.getStackTrace(sb, thr, conf.profile_fullstack_lines);
							thr = thr.getCause();
						}
						msg = sb.toString();
					
				}
				int hash = HashUtil.hash(msg);
				if (tctx.error == 0) {
					tctx.error = hash;
				}
				step.error = hash;
				DataProxy.sendError(hash, msg);
			}
			tctx.profile.pop(step);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	public static Object startSocket(Socket socket, SocketAddress addr, int timeout) {
		if (!(addr instanceof InetSocketAddress))
			return null;
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx == null) {
			Configure conf = Configure.getInstance();
			if (conf.debug_socket_openstack) {
				try {
					InetSocketAddress inet = (InetSocketAddress) addr;
					if (conf.debug_socket_openstack_port == 0 || conf.debug_socket_openstack_port == inet.getPort()) {
						Logger.info("SOCKET " + inet.getHostName() + ":" + inet.getPort() + " not profiled!!");
						Logger.info(ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace()));
					}
				} catch (Throwable t) {
				}
			}
			return null;
		}
		try {
			SocketStep step = new SocketStep();
			step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			if (ctx.profile_thread_cputime) {
				step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
			}
			InetSocketAddress inet = (InetSocketAddress) addr;
			InetAddress host = inet.getAddress();
			int port = inet.getPort();
			step.ipaddr = host == null ? null : host.getAddress();
			step.port = port;
			return new LocalContext(ctx, step, socket);
		} catch (Throwable t) {
			Logger.println("A141", "socket trace error", t);
			return null;
		}
	}
	public static void endSocket(Object stat, Throwable thr) {
		if (stat == null) {
			return;
		}
		try {
			LocalContext lctx = (LocalContext) stat;
			TraceContext tctx = (TraceContext) lctx.context;
			SocketStep step = (SocketStep) lctx.stepSingle;
			step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
			if (thr != null) {
				String msg = thr.toString();
				int hash = HashUtil.hash(msg);
				if (tctx.error == 0) {
					tctx.error = hash;
				}
				step.error = hash;
				DataProxy.sendError(hash, msg);
				AlertProxy.sendAlert(AlertLevel.WARN, "SOCKET_EXCEPTION", msg);
			}
			tctx.profile.add(step);
			Configure conf = Configure.getInstance();
			if (conf.profile_socket_openstack) {
				if (conf.profile_socket_openstack_port == 0 || conf.profile_socket_openstack_port == step.port) {
					MessageStep m = new MessageStep();
					m.start_time = step.start_time;
					m.message = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace());
					tctx.profile.add(m);
				}
			}
			if (conf.debug_socket_openstack) {
				if (conf.debug_socket_openstack_port == 0 || conf.debug_socket_openstack_port == step.port) {
					Logger.info("SOCKET " + IPUtil.toString(step.ipaddr) + ":" + step.port);
					Logger.info(ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace()));
				}
			}
		} catch (Throwable t) {
			Logger.println("A142", "socket trace close error", t);
		}
	}
	public static void open(File file) {
		TraceContext ctx = TraceContextManager.getLocalContext();
		if (ctx != null) {
			MessageStep m = new MessageStep();
			m.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			m.message = "FILE " + file.getName();
			ctx.profile.add(m);
		}
	}
}
