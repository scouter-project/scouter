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

import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;

import java.lang.reflect.Field;
import java.net.URL;

public class ForSunHttpClient implements ApiCallTraceHelper.IHelper {
	static Class sunHttpClass = null;
	static Field url = null;

	static {

		try {
			sunHttpClass = sun.net.www.http.HttpClient.class;
			url = sunHttpClass.getDeclaredField("url");
			url.setAccessible(true);
		} catch (Throwable e) {
			url = null;
			sunHttpClass = null;
		}

	}

	private boolean ok = true;

	public ApiCallStep process(TraceContext ctx, HookArgs hookPoint) {

		ApiCallStep step = new ApiCallStep();

		try {
			if (ok && (hookPoint.this1 instanceof sun.net.www.http.HttpClient)) {
				URL u = (URL) url.get(hookPoint.this1);
				if (u != null) {
					ctx.apicall_name = u.getPath();
				}
			}
		} catch (Exception e) {
			ok = false;
		}
		if (ctx.apicall_name == null)
			ctx.apicall_name = hookPoint.class1;
		return step;
	}

	public void processEnd(TraceContext ctx, ApiCallStep step, Object rtn, HookArgs hookPoint) {
		return;
	}

}
