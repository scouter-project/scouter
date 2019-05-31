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
import scouter.agent.proxy.HttpClient43Factory;
import scouter.agent.proxy.IHttpClient;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;

public class ForJavaNetHttpClient implements ApiCallTraceHelper.IHelper {

	private static IHttpClient httpClient = null;
	private static Configure conf = Configure.getInstance();
	private boolean ok = true;

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {
		ApiCallStep step = new ApiCallStep();
		if (ok) {
			try {
				if (hookPoint.args != null && hookPoint.args.length > 0) {
					IHttpClient httpclient = getProxy();
					//step.txid = KeyGen.next();
					//transfer(httpclient, ctx, hookPoint.args[0], hookPoint.args[1], step.txid);
					String host = httpclient.getHost(hookPoint.args[0]);
					step.opt = 1;
					step.address = host;
					if (host != null)
						ctx.apicall_target = host;
					ctx.apicall_name = httpclient.getURI(hookPoint.args[0]);
				}
			} catch (Exception e) {
				this.ok = false;
			}
		}
		if (ctx.apicall_name == null)
			ctx.apicall_name = hookPoint.class1;
		return step;
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {

	}

	private IHttpClient getProxy() {
		if (httpClient == null) {
			synchronized (this) {
				if (httpClient == null) {
					httpClient = HttpClient43Factory.create(JavaAgent.getPlatformClassLoader());
				}
			}
		}
		return httpClient;
	}
}
