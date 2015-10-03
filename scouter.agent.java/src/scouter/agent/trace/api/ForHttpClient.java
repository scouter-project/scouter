/*
 *  Copyright 2015 the original author or authors.
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

import java.lang.reflect.Method;

import scouter.agent.plugin.IApiCallTrace;
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;

public class ForHttpClient implements IApiCallTrace {

	private boolean ok = true;

	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {
		if (ok && apiInfo.arg != null && apiInfo.arg.length == 3) {
			try {
				Method method = apiInfo.arg[1].getClass().getMethod("getURI");
				Object o = method.invoke(apiInfo.arg[1]);
				if (o != null) {
					ctx.apicall_name = o.toString();
				}
			} catch (Throwable e) {
				ok = false;
			}
		}

		ApiCallStep step = new ApiCallStep();
		if (ctx.apicall_name == null)
			ctx.apicall_name = apiInfo.className;
		return step;
	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	public String targetName() {
		return "org/apache/commons/httpclient/HttpClient";
	}
	public void checkTarget(ApiInfo apiInfo) {
	}
}
