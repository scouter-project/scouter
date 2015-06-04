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
package scouter.agent.trace.api;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import scouter.agent.Configure;
import scouter.agent.plugin.IApiCallTrace;
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;
import scouter.util.Hexa32;
import scouter.util.KeyGen;

public class ForHttpURLConnection implements IApiCallTrace {

	static Class httpclass = null;
	static Field inputStream = null;

	static {
		try {
			httpclass = sun.net.www.protocol.http.HttpURLConnection.class;
			inputStream = httpclass.getDeclaredField("inputStream");
			inputStream.setAccessible(true);
		} catch (Throwable e) {
			inputStream = null;
			httpclass = null;
		}
	}

	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {

		ApiCallStep step = new ApiCallStep();

		try {
			if (apiInfo._this instanceof sun.net.www.protocol.http.HttpURLConnection) {
				if (inputStream.get(apiInfo._this) != null) {
					// Null  추적이 종료된다.
					return null;
				}
			}
			HttpURLConnection urlCon = ((HttpURLConnection) apiInfo._this);
			if ("connect".equals(apiInfo.methodName)) {
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
			ctx.apicall_name = apiInfo.className;
		return step;

	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	public String targetName() {
		return "sun/net/www/protocol/http/HttpURLConnection";
	}

	private void transfer(TraceContext ctx, HttpURLConnection urlCon, long calleeTxid) {

		Configure conf = Configure.getInstance();
		if (conf.enable_trace_e2e) {

			if (ctx.gxid == 0) {
				ctx.gxid = ctx.txid;
			}
			try {
				urlCon.setRequestProperty(conf.gxid_key, Hexa32.toString32(ctx.gxid));
				urlCon.setRequestProperty(conf.scouter_this_txid, Hexa32.toString32(calleeTxid));
				urlCon.setRequestProperty(conf.scouter_caller_txid, Hexa32.toString32(ctx.txid));
			} catch (Throwable t) {
			}
		}
	}
	public void checkTarget(ApiInfo apiInfo) {
	}
}
