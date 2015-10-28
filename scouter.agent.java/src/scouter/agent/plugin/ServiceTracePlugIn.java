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

package scouter.agent.plugin;

import java.util.HashMap;
import java.util.Map;

import scouter.agent.trace.ApiInfo;
import scouter.agent.trace.TraceContext;
import scouter.lang.pack.XLogPack;

public class ServiceTracePlugIn {

	static IServiceTrace dummy = new IServiceTrace() {
		public void start(TraceContext ctx, ApiInfo apiInfo) {
		}

		public void end(TraceContext ctx, XLogPack p) {
		}
	};
	static IServiceTrace plugIn = dummy;

	public static Map<String, IServiceTrace> handlers = new HashMap();

	public static void start(TraceContext ctx, ApiInfo apiInfo) {
		IServiceTrace handler = handlers.get(apiInfo.className);
		if (handler != null) {
			handler.start(ctx, apiInfo);
		} else {
			plugIn.start(ctx, apiInfo);
		}
	}

	public static void end(TraceContext ctx, XLogPack p) {
	}

}