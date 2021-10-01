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
import scouter.agent.counter.meter.MeterAPI;
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.plugin.PluginHttpCallTrace;
import scouter.agent.plugin.PluginHttpServiceTrace;
import scouter.agent.proxy.IHttpClient;
import scouter.agent.proxy.SpringRestTemplateHttpRequestFactory;
import scouter.agent.summary.ServiceSummary;
import scouter.agent.trace.api.ApiCallTraceHelper;
import scouter.lang.constants.B3Constant;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallStep2;
import scouter.lang.step.MessageStep;
import scouter.lang.step.SocketStep;
import scouter.util.*;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public class TraceApiCall {
	private static Configure conf = Configure.getInstance();
	private static Object lock = new Object();
	private static IntKeyLinkedMap<IHttpClient> restTemplateHttpRequests = new IntKeyLinkedMap<IHttpClient>().setMax(5);
	private static IntKeyLinkedMap<TransferMap.ID> map = new IntKeyLinkedMap<TransferMap.ID>().setMax(2001);

	static {
		try {
			PluginHttpServiceTrace.class.getClass();
		} catch (Throwable t) {
		}
	}

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

	public static void apiInfo(String className, String methodName, String methodDesc, Object _this, Object[] arg) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null && arg.length >= 2) {
			ctx.apicall_target = arg[0] + ":" + arg[1];
		}
	}
	public static Object startApicall(String className, String methodName, String methodDesc, Object _this, Object[] arg) {
		try {
			TraceContext ctx = TraceContextManager.getContext();
			if (ctx == null) {
				return null;
			}
			if (ctx.apicall_name != null) {
				return null;
			}
			HookArgs hookPoint = new HookArgs(className, methodName, methodDesc, _this, arg);
			ApiCallStep step = ApiCallTraceHelper.start(ctx, hookPoint);
			if (step == null)
				return null;
			step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			if (ctx.profile_thread_cputime) {
				step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
			}
			ctx.profile.push(step);

			return new LocalContext(ctx, step, hookPoint);
		} catch (Throwable sss) {
			sss.printStackTrace();
		}
		return null;
	}

	public static Object startApicall(String name, long apiTxid) {
		TraceContext ctx = TraceContextManager.getContext();
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
			TraceContext tctx = lctx.context;
			ApiCallStep step = (ApiCallStep) lctx.stepSingle;

			if (step.address == null) {
				step.address = tctx.apicall_target;
			}
			step.hash = DataProxy.sendApicall(tctx.apicall_name);
			tctx.apicall_name = null;
			tctx.apicall_target = null;
			tctx.lastApiCallStep = null;

			step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
			if (tctx.profile_thread_cputime) {
				step.cputime = (int) (SysJMX.getCurrentThreadCPU() - tctx.startCpu) - step.start_cpu;
			}
			tctx.apicall_count++;
			tctx.apicall_time += step.elapsed;

			if (thr != null) {
				String msg = thr.getMessage();
				if(msg == null){
					msg = thr.toString();
				}
				Configure conf = Configure.getInstance();
				if (conf.profile_fullstack_apicall_error_enabled) {
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
				step.error = DataProxy.sendError(msg);
				if (tctx.error == 0 && conf.xlog_error_on_apicall_exception_enabled) {
					tctx.error = step.error;
				}
				tctx.offerErrorEntity(ErrorEntity.of(thr, step.error, 0, step.hash));
			}

			Object hookArgs = lctx.option;
			if (hookArgs instanceof HookArgs) {
				ApiCallTraceHelper.end(tctx, step, returnValue, (HookArgs) hookArgs);
			}

			if(step instanceof ApiCallStep2 && ((ApiCallStep2) step).async == 1) {
				//skip api metering
			} else {
				MeterAPI.getInstance().add(step.elapsed, step.error != 0);
				if (conf.counter_interaction_enabled) {
					int toHash = tctx.lastCalleeObjHash;
					tctx.lastCalleeObjHash = 0;
					if (toHash == 0) {
						if (StringUtil.isEmpty(step.address)) {
							step.address = "unknown";
						}
						toHash = DataProxy.sendObjName(step.address);
						MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
								.getNormalOutgoingMeter(conf.getObjHash(), toHash);
						if (meterInteraction != null) {
							meterInteraction.add(step.elapsed, step.error != 0);
						}
					} else {
						MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
								.getApiOutgoingMeter(conf.getObjHash(), toHash);
						if (meterInteraction != null) {
							meterInteraction.add(step.elapsed, step.error != 0);
						}
					}
				}
			}

			ServiceSummary.getInstance().process(step);
			tctx.profile.pop(step);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	public static Object startSocket(Object socketOrSocketChannel, SocketAddress addr) {
		if (!(addr instanceof InetSocketAddress))
			return null;
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			if (Configure.getInstance().trace_background_socket_enabled) {
				InetSocketAddress inet = (InetSocketAddress) addr;
				InetAddress host = inet.getAddress();
				int port = inet.getPort();
				byte[] ipaddr = host == null ? null : host.getAddress();
				SocketTable.add(ipaddr, port, 0, 0);
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
			return new LocalContext(ctx, step, socketOrSocketChannel);
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
			TraceContext tctx = lctx.context;
			SocketStep step = (SocketStep) lctx.stepSingle;
			step.elapsed = (int) (System.currentTimeMillis() - tctx.startTime) - step.start_time;
			if (thr != null) {
				String msg = thr.toString();
				step.error = DataProxy.sendError(msg);
				if (tctx.error == 0) {
					tctx.error = step.error;
				}
				tctx.offerErrorEntity(ErrorEntity.of(thr, step.error, 0, 0));
			}
			tctx.profile.add(step);
			SocketTable.add(step.ipaddr, step.port, tctx.serviceHash, tctx.txid);
			Configure conf = Configure.getInstance();
			if (conf.profile_socket_open_fullstack_enabled) {
				if (conf.profile_socket_open_fullstack_port == 0 || conf.profile_socket_open_fullstack_port == step.port) {
					tctx.profile.add(new MessageStep(step.start_time, ThreadUtil.getThreadStack()));
				}
			}
		} catch (Throwable t) {
			Logger.println("A142", "socket trace close error", t);
		}
	}
	public static void open(File file) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			MessageStep m = new MessageStep();
			m.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			m.message = "FILE " + file.getName();
			ctx.profile.add(m);
		}
	}

	public static final IHttpClient dummyHttpClient = new IHttpClient() {
		public String getURI(Object o) {
			return null;
		}
		public String getHost(Object o) {
			return null;
		}
		public String getHeader(Object o, String key) {
			return null;
		}
		public String getResponseHeader(Object o, String key) {
			return null;
		}
		public int getResponseStatusCode(Object o) {
			return 200;
		}
		public void addHeader(Object o, String key, String value) {
		}
	};

	public static void endCreateSpringRestTemplateRequest(Object _this, Object oRtn) {
		if (!conf.trace_interservice_enabled) {
			return;
		}

		TraceContext ctx = TraceContextManager.getContext();
		if(ctx == null) return;
		if(ctx.lastApiCallStep == null) return;

		Configure conf = Configure.getInstance();

		int key = System.identityHashCode(_this.getClass());
		IHttpClient httpclient = restTemplateHttpRequests.get(key);
		if (httpclient == null) {
			synchronized (lock) {
				if (httpclient == null) {
					if (_this.getClass().getClassLoader() == null) {
						httpclient = dummyHttpClient;
					} else {
						Set<String> allSuperSet = getAllExtendedOrImplementedTypesRecursively(oRtn.getClass());
						if (allSuperSet.contains("org.springframework.http.HttpRequest")) { //Spring 3.0 doesn't have the interface. HttpRequest is since 3.1
							httpclient = SpringRestTemplateHttpRequestFactory.create(_this.getClass().getClassLoader());
						} else {
							httpclient = dummyHttpClient;
						}
					}
					restTemplateHttpRequests.put(key, httpclient);
				}
			}
		}

		try {
			if (ctx.gxid == 0) {
				ctx.gxid = ctx.txid;
			}
			ctx.lastApiCallStep.txid = KeyGen.next();

			httpclient.addHeader(oRtn, conf._trace_interservice_gxid_header_key, Hexa32.toString32(ctx.gxid));
			httpclient.addHeader(oRtn, conf._trace_interservice_caller_header_key, Hexa32.toString32(ctx.txid));
			httpclient.addHeader(oRtn, conf._trace_interservice_callee_header_key, Hexa32.toString32(ctx.lastApiCallStep.txid));
			httpclient.addHeader(oRtn, conf._trace_interservice_caller_obj_header_key, String.valueOf(conf.getObjHash()));

			if (conf.trace_propagete_b3_header) {
				httpclient.addHeader(oRtn, B3Constant.B3_HEADER_TRACEID, Hexa32.toUnsignedLongHex(ctx.gxid));
				httpclient.addHeader(oRtn, B3Constant.B3_HEADER_PARENTSPANID, Hexa32.toUnsignedLongHex(ctx.txid));
				httpclient.addHeader(oRtn, B3Constant.B3_HEADER_SPANID, Hexa32.toUnsignedLongHex(ctx.lastApiCallStep.txid));
				//httpclient.addHeader(oRtn, B3Constant.B3_HEADER_SAMPLED, "1"); omit means defer
			}

			PluginHttpCallTrace.call(ctx, httpclient, oRtn);

		} catch (Exception e) {

		}
	}

	public static void webClientInfo(Object bodyInserter, Object clientHttpRequest) {
		if (!conf.trace_interservice_enabled) {
			return;
		}
		ApiCallTraceHelper.webClientInfo(bodyInserter, clientHttpRequest);
	}

	public static void endWebClientApicall(Object exchangeFunction, Object clientResponse) {
		if (!conf.trace_interservice_enabled) {
			return;
		}
		LocalContext localContext = ApiCallTraceHelper.webClientProcessEnd(exchangeFunction, clientResponse);
		if (localContext == null) {
			return;
		}
		Object option = localContext.option;
		localContext.option = null;
		Throwable throwable = null;
		if (option instanceof Integer) {
			int statusCode = (Integer) option;
			if (statusCode >= 400) {
				throwable = new RuntimeException("WebClient response code: " + statusCode);
			}
		}
		endApicall(localContext, clientResponse, throwable);
	}

	public static void setCalleeToCtxInHttpClientResponse(Object _this, Object res) {
		if (!conf.trace_interservice_enabled) {
			return;
		}

		TraceContext ctx = TraceContextManager.getContext();
		if(ctx == null) return;

		try {
			ApiCallTraceHelper.setCalleeToCtxInHttpClientResponse(ctx, _this, res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setCalleeToCtxInSpringClientHttpResponse(Object _this, Object res) {
		if (!conf.trace_interservice_enabled) {
			return;
		}

		TraceContext ctx = TraceContextManager.getContext();
		ApiCallStep2 step = null;

		if (ctx == null) {
			int thisHash = System.identityHashCode(_this);
			ApiCallTransferMap.ID transferID = ApiCallTransferMap.get(thisHash);
			if (transferID != null) {
				ApiCallTransferMap.remove(thisHash);
				ctx = transferID.ctx;
				step = transferID.step;
			} else {
				return;
			}
		}

		try {
			ApiCallTraceHelper.setCalleeToCtxInSpringClientHttpResponse(ctx, _this, res);

			if (step != null && step.async == 1) {
				ctx.apicall_time -= step.elapsed;
				step.elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - step.start_time;
				ctx.apicall_time += step.elapsed;

				MeterAPI.getInstance().add(step.elapsed, step.error != 0);

				if (conf.counter_interaction_enabled) {
					int toHash = ctx.lastCalleeObjHash;
					ctx.lastCalleeObjHash = 0;
					if (toHash == 0) {
						if (StringUtil.isEmpty(step.address)) {
							step.address = "unknown";
						}
						toHash = DataProxy.sendObjName(step.address);
						MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
								.getNormalOutgoingMeter(conf.getObjHash(), toHash);
						if (meterInteraction != null) {
							meterInteraction.add(step.elapsed, step.error != 0);
						}
					} else {
						MeterInteraction meterInteraction = MeterInteractionManager.getInstance()
								.getApiOutgoingMeter(conf.getObjHash(), toHash);
						if (meterInteraction != null) {
							meterInteraction.add(step.elapsed, step.error != 0);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initImmutableJavaHttpRequest(Object requestBuilder) {
		if (!conf.trace_interservice_enabled) {
			return;
		}

		TraceContext ctx = TraceContextManager.getContext();
		if(ctx == null) return;

		try {
			ApiCallTraceHelper.setTransferToCtxJavaHttpRequest(ctx, requestBuilder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static Set<String> getAllExtendedOrImplementedTypesRecursively(Class clazz) {
		List<String> res = new ArrayList<String>();

		do {
			res.add(clazz.getName());

			// First, add all the interfaces implemented by this class
			Class[] interfaces = clazz.getInterfaces();
			if (interfaces.length > 0) {
				for(int i=0; i<interfaces.length; i++) {
					res.add(interfaces[i].getName());
				}

				for (Class interfaze : interfaces) {
					res.addAll(getAllExtendedOrImplementedTypesRecursively(interfaze));
				}
			}

			// Add the super class
			Class superClass = clazz.getSuperclass();

			// Interfaces does not have java,lang.Object as superclass, they have null, so break the cycle and return
			if (superClass == null) {
				break;
			}

			// Now inspect the superclass
			clazz = superClass;
		} while (!"java.lang.Object".equals(clazz.getCanonicalName()));

		return new HashSet<String>(res);
	}
}
