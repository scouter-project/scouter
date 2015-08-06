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

import scouter.agent.plugin.IApiCallTrace;
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;

public class ForJCOClient implements IApiCallTrace {
	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {

		ApiCallStep step = new ApiCallStep();

		if (apiInfo.arg != null && apiInfo.arg.length > 0) {
			ctx.apicall_name = apiInfo.arg[0] + "(JCO)";
		}

		if (ctx.apicall_name == null)
			ctx.apicall_name = apiInfo.className;
		return step;
	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	public String targetName() {
		return "com/sap/mw/jco/JCO$Client";
	}
	public void checkTarget(ApiInfo apiInfo) {
	}
}
