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

import scouter.agent.proxy.IHttpClient;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallStep2;
import scouter.util.IntKeyLinkedMap;

import java.net.URI;

public class ForSpringRestTemplate implements ApiCallTraceHelper.IHelper {

	private boolean ok = true;
	private static IntKeyLinkedMap<IHttpClient> httpclients = new IntKeyLinkedMap<IHttpClient>().setMax(5);

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {

		ApiCallStep2 step = new ApiCallStep2();

		step.opt = 1;
		step.address = null;

		if (ok) {
			try {
				URI uri = (URI) hookPoint.args[0];
				if (uri != null) {
					step.address = uri.getHost() + ":" + uri.getPort();
					ctx.apicall_target = step.address;
					ctx.apicall_name = uri.getPath();
				}
			} catch (Exception e) {
				ok = false;
			}
		}

		if (ctx.apicall_name == null)
			ctx.apicall_name = hookPoint.class1;

		ctx.lastApiCallStep = step;

		return step;
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {

		return;
	}
}
