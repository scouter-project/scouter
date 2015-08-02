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
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.lang.step.ApiCallStep;
import scouter.util.Hexa32;
import scouter.util.IntKeyLinkedMap;
import scouter.util.KeyGen;

public class ForHttpClient43 implements IApiCallTrace {

	public String targetName() {
		return "org/apache/http/impl/client/InternalHttpClient";
	}

	private static IntKeyLinkedMap<IHttpClient> httpclients = new IntKeyLinkedMap<IHttpClient>().setMax(5);

	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {

		ApiCallStep step = new ApiCallStep();

		if (ok) {
			try {
				if (apiInfo.arg != null && apiInfo.arg.length >= 2) {
					IHttpClient httpclient = getProxy(apiInfo);

					step.txid = KeyGen.next();
					transfer(httpclient, ctx, apiInfo.arg[0], apiInfo.arg[1], step.txid);
					String host = httpclient.getHost(apiInfo.arg[0]);

					step.opt = 1;
					step.address = host;
					if(host!=null)
						ctx.apicall_target=host;
					
					ctx.apicall_name = httpclient.getURI(apiInfo.arg[1]);
				}
			} catch (Exception e) {
				this.ok = false;
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
				httpclient = HttpClient43Factory.create(apiInfo._this.getClass().getClassLoader());
				httpclients.put(key, httpclient);
			}
		}
		return httpclient;
	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	private boolean ok = true;

	private void transfer(IHttpClient httpclient, TraceContext ctx, Object host, Object req, long calleeTxid) {
		Configure conf = Configure.getInstance();
		if (conf.enable_trace_e2e) {
			try {

				if (ctx.gxid == 0) {
					ctx.gxid = ctx.txid;
				}
				httpclient.addHeader(req, conf.gxid, Hexa32.toString32(ctx.gxid));
				httpclient.addHeader(req, conf.caller_txid, Hexa32.toString32(ctx.txid));
				httpclient.addHeader(req, conf.this_txid, Hexa32.toString32(calleeTxid));
			} catch (Exception e) {
				System.err.println("HttpClinet4.3 " + e);
				ok = false;
			}
		}
	}

	public void checkTarget(ApiInfo apiInfo) {
		Configure conf = Configure.getInstance();
		if (conf.enable_trace_e2e==false) {
			return;
		}
		int key = System.identityHashCode(apiInfo._this.getClass());
		IHttpClient httpclient = httpclients.get(key);
		if (httpclient == null) {
			synchronized (this) {
				httpclient = HttpClient43Factory.create(apiInfo._this.getClass().getClassLoader());
				httpclients.put(key, httpclient);
			}
		}
		String thread_id = httpclient.getHeader( apiInfo.arg[1], "scouter_thread_id" );
		if(thread_id!=null){
		    TraceContext ctx= TraceContextManager.getContext(Long.parseLong(thread_id));
  		      if(ctx!=null){
  		    	  ctx.apicall_target=apiInfo.arg[0].toString();
  		    	  System.out.println("HttpClient43 target: " +ctx.apicall_target);
  		      }
		}
	}
}
