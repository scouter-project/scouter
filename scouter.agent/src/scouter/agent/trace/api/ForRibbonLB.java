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

import scouter.agent.Configure;
import scouter.agent.plugin.IApiCallTrace;
import scouter.agent.proxy.HttpClient43Factory;
import scouter.agent.proxy.IHttpClient;
import scouter.agent.proxy.NettyHttpClientFactory;
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;
import scouter.util.Hexa32;
import scouter.util.IntKeyLinkedMap;
import scouter.util.KeyGen;

public class ForRibbonLB implements IApiCallTrace {

	private int fail = 0;
	private static IntKeyLinkedMap<IHttpClient> httpclients = new IntKeyLinkedMap<IHttpClient>().setMax(5);

	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {

		ApiCallStep step = new ApiCallStep();

		if (fail < 100 && apiInfo.arg != null && apiInfo.arg.length > 2) {
			try {
				IHttpClient httpclient = getProxy(apiInfo);
				step.txid = KeyGen.next();
				transfer(httpclient, ctx, apiInfo.arg[1], step.txid);
				String host = httpclient.getHost(apiInfo.arg[0]);

				step.opt = 1;
				step.address = host;
				ctx.apicall_name = httpclient.getURI(apiInfo.arg[1]);
				ctx.apicall_name = fw_stripes(ctx.apicall_name);

			} catch (Throwable e) {
				ctx.apicall_name = e.toString();
				e.printStackTrace();
				fail++;
			}
		}
		if (ctx.apicall_name == null)
			ctx.apicall_name = apiInfo.className;
		return step;
	}

	private IHttpClient getProxy(ApiInfo apiInfo) {
		int key = System.identityHashCode(apiInfo._this.getClass());
		IHttpClient httpclient = httpclients.get(key);
		if (httpclient == null) {
			synchronized (this) {
				httpclient = NettyHttpClientFactory.create(apiInfo._this.getClass().getClassLoader());
				httpclients.put(key, httpclient);
			}
		}
		return httpclient;
	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	private void transfer(IHttpClient httpclient, TraceContext ctx, Object req, long calleeTxid) {
		Configure conf = Configure.getInstance();
		if (conf.enable_trace_e2e) {
			try {

				if (ctx.gxid == 0) {
					ctx.gxid = ctx.txid;
				}
				httpclient.addHeader(req, conf.gxid_key, Hexa32.toString32(ctx.gxid));
				httpclient.addHeader(req, conf.scouter_caller_txid, Hexa32.toString32(ctx.txid));
				httpclient.addHeader(req, conf.scouter_this_txid, Hexa32.toString32(calleeTxid));
				httpclient.addHeader(req, "scouter_caller_url", ctx.serviceName);
				httpclient.addHeader(req, "scouter_caller_name", conf.objName);
				httpclient.addHeader(req, "scouter_thread_id", Long.toString(ctx.threadId));
				
			} catch (Exception e) {
				
			}
		}
	}

	private String fw_stripes(String url) {
		if (url == null)
			return null;
		int y = url.indexOf('?');
		if (y > 0) {
			return url.substring(0, y);
		}
		return url;
	}

	public void checkTarget(ApiInfo apiInfo) {
	}

	public String targetName() {
		return "com/netflix/ribbon/transport/netty/http/LoadBalancingHttpClient";
	}
}
