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
import scouter.agent.Logger;
import scouter.agent.plugin.PluginHttpCallTrace;
import scouter.agent.proxy.IHttpClient;
import scouter.agent.proxy.WebClientFactory;
import scouter.agent.trace.ApiCallTransferMap;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.LocalContext;
import scouter.agent.trace.TraceContext;
import scouter.lang.constants.B3Constant;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallStep2;
import scouter.util.Hexa32;
import scouter.util.IntKeyLinkedMap;
import scouter.util.KeyGen;

public class ForWebClient implements ApiCallTraceHelper.IHelper {

	private static IntKeyLinkedMap<IHttpClient> httpclients = new IntKeyLinkedMap<IHttpClient>().setMax(10);
	private static Configure conf = Configure.getInstance();

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {
		ApiCallStep2 step = new ApiCallStep2();
		ctx.apicall_name = hookPoint.class1;

		if (ok) {
			try {
				if (hookPoint.args != null && hookPoint.args.length >  0) {
					ApiCallTransferMap.put(System.identityHashCode(hookPoint.args[0]), ctx, step);
					ApiCallTransferMap.put(System.identityHashCode(hookPoint.this1), ctx, step);

				}
			} catch (Exception e) {
				this.ok = false;
			}
		}
		return step;
	}

	public void processInfo(Object bodyInserter, Object clientHttpRequest) {
		if (bodyInserter == null) {
			return;
		}
		int bodyInserterHash = System.identityHashCode(bodyInserter);
		ApiCallTransferMap.ID id = ApiCallTransferMap.get(bodyInserterHash);
		if (id == null) {
			return;
		}
		ApiCallTransferMap.remove(bodyInserterHash);

		TraceContext ctx = id.ctx;
		ApiCallStep2 step = id.step;
		if (ok) {
			try {
				IHttpClient httpclient = getProxy(clientHttpRequest);
				String host = httpclient.getHost(clientHttpRequest);

				step.opt = 1;
				step.address = host;
				if (host != null)
					ctx.apicall_target = host;
				ctx.apicall_name = httpclient.getURI(clientHttpRequest);

				step.txid = KeyGen.next();
				transfer(httpclient, ctx, clientHttpRequest, step.txid);
			} catch (Exception e) {
				this.ok = false;
			}
		}
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {
		//processEnd(rtn);
	}

	public LocalContext processEnd(Object exchangeFunction, Object clientHttpResponse) {
		int exchangeFunctionHash = System.identityHashCode(exchangeFunction);
		ApiCallTransferMap.ID id = ApiCallTransferMap.get(exchangeFunctionHash);
		if (id == null) {
			return null;
		}
		ApiCallTransferMap.remove(exchangeFunctionHash);

		TraceContext ctx = id.ctx;
		ApiCallStep2 step = id.step;

		IHttpClient httpclient = getProxy(exchangeFunction);
		String calleeObjHashStr = httpclient.getResponseHeader(clientHttpResponse, conf._trace_interservice_callee_obj_header_key);
		if (calleeObjHashStr != null) {
			try {
				ctx.lastCalleeObjHash = Integer.parseInt(calleeObjHashStr);
			} catch (NumberFormatException e) {
			}
		} else {
			ctx.lastCalleeObjHash = 0;
		}

		return new LocalContext(ctx, step, httpclient.getResponseStatusCode(clientHttpResponse));
	}

	private IHttpClient getProxy(Object _this) {
		int key = System.identityHashCode(_this.getClass());
		IHttpClient httpclient = httpclients.get(key);
		if (httpclient == null) {
			synchronized (this) {
				httpclient = WebClientFactory.create(_this.getClass().getClassLoader());
				httpclients.put(key, httpclient);
			}
		}
		return httpclient;
	}

	private boolean ok = true;

	private void transfer(IHttpClient httpclient, TraceContext ctx, Object req, long calleeTxid) {
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
				PluginHttpCallTrace.call(ctx, req);
			} catch (Exception e) {
				Logger.println("A178w", e);
				ok = false;
			}
		}
	}
}
