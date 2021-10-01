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
import scouter.agent.plugin.PluginHttpCallTrace;
import scouter.agent.proxy.IHttpClient;
import scouter.agent.proxy.NettyHttpClientFactory;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.lang.constants.B3Constant;
import scouter.lang.step.ApiCallStep;
import scouter.util.Hexa32;
import scouter.util.IntKeyLinkedMap;
import scouter.util.KeyGen;

public class ForRibbonLB implements ApiCallTraceHelper.IHelper {

	private int fail = 0;
	private static IntKeyLinkedMap<IHttpClient> httpclients = new IntKeyLinkedMap<IHttpClient>().setMax(5);

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {

		ApiCallStep step = new ApiCallStep();

		if (fail < 100 && hookPoint.args != null && hookPoint.args.length > 2) {
			try {
				IHttpClient httpclient = getProxy(hookPoint);
				step.txid = KeyGen.next();
				transfer(httpclient, ctx, hookPoint.args[1], step.txid);
				String host = httpclient.getHost(hookPoint.args[0]);

				step.opt = 1;
				step.address = host;
				ctx.apicall_name = httpclient.getURI(hookPoint.args[1]);
				ctx.apicall_name = fw_stripes(ctx.apicall_name);

			} catch (Throwable e) {
				ctx.apicall_name = e.toString();
				e.printStackTrace();
				fail++;
			}
		}
		if (ctx.apicall_name == null)
			ctx.apicall_name = hookPoint.class1;
		return step;
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {
		return;
	}

	private IHttpClient getProxy(HookArgs hookPoint) {
		int key = System.identityHashCode(hookPoint.this1.getClass());
		IHttpClient httpclient = httpclients.get(key);
		if (httpclient == null) {
			synchronized (this) {
				httpclient = NettyHttpClientFactory.create(hookPoint.this1.getClass().getClassLoader());
				httpclients.put(key, httpclient);
			}
		}
		return httpclient;
	}

	private void transfer(IHttpClient httpclient, TraceContext ctx, Object req, long calleeTxid) {
		Configure conf = Configure.getInstance();
		if (conf.trace_interservice_enabled) {
			try {

				if (ctx.gxid == 0) {
					ctx.gxid = ctx.txid;
				}
				httpclient.addHeader(req, conf._trace_interservice_gxid_header_key, Hexa32.toString32(ctx.gxid));
				httpclient.addHeader(req, conf._trace_interservice_caller_header_key, Hexa32.toString32(ctx.txid));
				httpclient.addHeader(req, conf._trace_interservice_callee_header_key, Hexa32.toString32(calleeTxid));
				httpclient.addHeader(req, conf._trace_interservice_caller_obj_header_key, String.valueOf(conf.getObjHash()));

				if (conf.trace_propagete_b3_header) {
					httpclient.addHeader(req, B3Constant.B3_HEADER_TRACEID, Hexa32.toUnsignedLongHex(ctx.gxid));
					httpclient.addHeader(req, B3Constant.B3_HEADER_PARENTSPANID, Hexa32.toUnsignedLongHex(ctx.txid));
					httpclient.addHeader(req, B3Constant.B3_HEADER_SPANID, Hexa32.toUnsignedLongHex(calleeTxid));
				}

				PluginHttpCallTrace.call(ctx, httpclient, req);

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
}
