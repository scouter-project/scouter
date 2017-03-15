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

import java.util.HashMap;
import java.util.Map;

public class ApiCallTraceHelper {
	static interface IHelper {
		public ApiCallStep process(TraceContext ctx, HookArgs hookPoint);
	}

	static Map<String, IHelper> handlers = new HashMap<String, IHelper>();

	static void put(String name, IHelper o) {
		name = name.replace('.', '/');
		handlers.put(name, o);
	}

	public static IHelper get(String name) {
		return handlers.get(name);
	}

	static {
		put("sun/net/www/protocol/http/HttpURLConnection", new ForHttpURLConnection());
		put("sun/net/www/http/HttpClient", new ForSunHttpClient());
		put("org/apache/commons/httpclient/HttpClient", new ForHttpClient());
		put("org/apache/http/impl/client/InternalHttpClient", new ForHttpClient43());
		put("org/apache/http/impl/client/AbstractHttpClient", new ForHttpClient40());
		put("com/sap/mw/jco/JCO$Client", new ForJCOClient());
		put("com/netflix/ribbon/transport/netty/http/LoadBalancingHttpClient", new ForRibbonLB());
		put("io/reactivex/netty/protocol/http/client/HttpClientImpl", new ForNettyHttpRequest());
		put("org/springframework/web/client/RestTemplate", new ForSpringRestTemplate());
		put("org/springframework/web/client/AsyncRestTemplate", new ForSpringAsyncRestTemplate());
	}

	private static IHelper defaultObj = new ForDefault();

	public static ApiCallStep start(TraceContext ctx, HookArgs hookPoint) {
		IHelper plug = handlers.get(hookPoint.class1);
		if (plug == null)
			return defaultObj.process(ctx, hookPoint);
		return plug.process(ctx, hookPoint);
	}

}
