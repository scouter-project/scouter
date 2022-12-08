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
package scouter.agent.trace.api;

import scouter.agent.Configure;
import scouter.agent.JavaAgent;
import scouter.agent.plugin.PluginHttpCallTrace;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.agent.util.ModuleUtil;
import scouter.lang.constants.B3Constant;
import scouter.lang.step.ApiCallStep;
import scouter.util.Hexa32;
import scouter.util.KeyGen;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

public class ForHttpURLConnection implements ApiCallTraceHelper.IHelper {

	static Class httpclass = null;
	static Field inputStream = null;

	static {
		try {
			if (JavaAgent.isJava9plus()) {
				ModuleUtil.grantAccess(JavaAgent.getInstrumentation(),
                    ForHttpURLConnection.class.getName(),
					"sun.net.www.protocol.http.HttpURLConnection");
			}
			httpclass = sun.net.www.protocol.http.HttpURLConnection.class;
			inputStream = httpclass.getDeclaredField("inputStream");
			inputStream.setAccessible(true);
		} catch (Throwable e) {
			inputStream = null;
			httpclass = null;
		}
	}

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {

		ApiCallStep step = new ApiCallStep();

		try {
			if (hookPoint.this1 instanceof sun.net.www.protocol.http.HttpURLConnection) {
				if (inputStream.get(hookPoint.this1) != null) {
					// Null  추적이 종료된다.
					return null;
				}
			}
			HttpURLConnection urlCon = ((HttpURLConnection) hookPoint.this1);
			if ("connect".equals(hookPoint.method)) {
				step.txid = KeyGen.next();
				transfer(ctx, urlCon, step.txid);
				ctx.callee = step.txid;
				return null; // apicall을 무시함...
			} else {
				if (ctx.callee == 0) { // connect가 호출되지 않음
					step.txid = KeyGen.next();
					transfer(ctx, urlCon, step.txid);
				} else {
					step.txid = ctx.callee;
					ctx.callee = 0;
				}
				URL url = urlCon.getURL();
				ctx.apicall_name = url.getPath();

				step.opt = 1;
				step.address = url.getHost() + ":" + url.getPort();

			}
		} catch (Exception e) {
		}

		if (ctx.apicall_name == null)
			ctx.apicall_name = hookPoint.class1;
		return step;
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {
		return;
	}

	private void transfer(TraceContext ctx, HttpURLConnection urlCon, long calleeTxid) {

		Configure conf = Configure.getInstance();
		if (conf.trace_interservice_enabled) {

			if (ctx.gxid == 0) {
				ctx.gxid = ctx.txid;
			}
			try {
				urlCon.setRequestProperty(conf._trace_interservice_gxid_header_key, Hexa32.toString32(ctx.gxid));
				urlCon.setRequestProperty(conf._trace_interservice_callee_header_key, Hexa32.toString32(calleeTxid));
				urlCon.setRequestProperty(conf._trace_interservice_caller_header_key, Hexa32.toString32(ctx.txid));
				urlCon.setRequestProperty(conf._trace_interservice_caller_obj_header_key, String.valueOf(conf.getObjHash()));

				if (conf.trace_propagete_b3_header) {
					urlCon.setRequestProperty(B3Constant.B3_HEADER_TRACEID, Hexa32.toUnsignedLongHex(ctx.gxid));
					urlCon.setRequestProperty(B3Constant.B3_HEADER_PARENTSPANID, Hexa32.toUnsignedLongHex(ctx.txid));
					urlCon.setRequestProperty(B3Constant.B3_HEADER_SPANID, Hexa32.toUnsignedLongHex(calleeTxid));
				}
				PluginHttpCallTrace.call(ctx, urlCon);
			} catch (Throwable t) {
			}
		}
	}

	public void checkTarget(HookArgs hookPoint) {
	}
}
