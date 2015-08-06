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

package scouter.agent.plugin;

import java.util.HashMap;
import java.util.Map;

import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.api.ForDefault;
import scouter.agent.trace.api.ForHttpClient;
import scouter.agent.trace.api.ForHttpClient40;
import scouter.agent.trace.api.ForHttpClient43;
import scouter.agent.trace.api.ForHttpURLConnection;
import scouter.agent.trace.api.ForJCOClient;
import scouter.agent.trace.api.ForNettyHttpRequest;
import scouter.agent.trace.api.ForRibbonLB;
import scouter.agent.trace.api.ForSAPTemp;
import scouter.agent.trace.api.ForSunHttpClient;
import scouter.lang.step.ApiCallStep;

public class ApiCallTracePlugin {

	public static Map<String, IApiCallTrace> handlers = new HashMap();

	public static void put(IApiCallTrace o) {
		if (o.targetName() == null)
			return;
		String name = o.targetName().replace('.', '/');
		handlers.put(name, o);
	}

	public static void put(String name, IApiCallTrace o) {
		name = name.replace('.', '/');
		handlers.put(name, o);
	}

	public static IApiCallTrace get(String name) {
		return handlers.get(name);
	}

	static {
		defaultObjects();
	}

	private static void defaultObjects() {
		put(new ForHttpURLConnection());
		put(new ForSunHttpClient());
		put(new ForHttpClient());
		put(new ForHttpClient43());
		put(new ForHttpClient40());
		put(new ForJCOClient());
		put(new ForRibbonLB());
		put(new ForNettyHttpRequest());

		// TODO:LF를 위해 임시로 추가해둠 나중에 Plugin으로 분리해야함
		put("lgfs/frontoffice/sap/service/SapServiceImpl", new ForSAPTemp());
		put("lgfs/woutlet/sap/service/SapServiceImpl", new ForSAPTemp());
		put("lgfs/mfront/sap/service/SapServiceImpl", new ForSAPTemp());
		put("lgfs/moutlet/sap/service/SapServiceImpl", new ForSAPTemp());
	}

	public static void reinit() {
		handlers.clear();
		defaultObjects();
	}

	private static IApiCallTrace defaultObj = new ForDefault();

	public static ApiCallStep start(TraceContext ctx, ApiInfo apiInfo) {
		IApiCallTrace plug = handlers.get(apiInfo.className);
		if (plug == null)
			return defaultObj.apiCall(ctx,apiInfo);
		return plug.apiCall(ctx, apiInfo);
	}

	
}