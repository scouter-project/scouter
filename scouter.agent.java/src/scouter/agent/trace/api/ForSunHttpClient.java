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
import java.net.URL;

import scouter.agent.plugin.IApiCallTrace;
import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.step.ApiCallStep;

public class ForSunHttpClient implements IApiCallTrace {
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

	public ApiCallStep apiCall(TraceContext ctx, ApiInfo apiInfo) {

		ApiCallStep step = new ApiCallStep();

		try {
			if (ok && (apiInfo._this instanceof sun.net.www.http.HttpClient)) {
				URL u = (URL) url.get(apiInfo._this);
				if (u != null) {
					ctx.apicall_name = u.getPath();
				}
			}
		} catch (Exception e) {
			ok = false;
		}
		if (ctx.apicall_name == null)
			ctx.apicall_name = apiInfo.className;
		return step;
	}

	public void apiEnd(TraceContext ctx, ApiInfo apiInfo, Object returnValue, Throwable thr) {
	}

	public void checkTarget(ApiInfo apiInfo) {
	}

	public String targetName() {
		return "sun/net/www/http/HttpClient";
	}
}
