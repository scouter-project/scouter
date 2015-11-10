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

import scouter.agent.trace.TraceContext;
import scouter.lang.pack.XLogPack;

public class HttpServiceTracePlugIn {

	static IHttpService plugIn;

	static {
		PlugInLoader.getInstance();
	}

	public static void start(TraceContext ctx, Object req, Object res) {
		if (plugIn != null) {
			try {
				plugIn.start(new ContextWrapper(ctx), new RequestWrapper(req), new ResponseWrapper(res));
			} catch (Throwable t) {
			}
		}
	}

	public static void end(TraceContext ctx, XLogPack p) {
		if (plugIn != null) {
			try {
				plugIn.end(new ContextWrapper(ctx), p);
			} catch (Throwable t) {
			}
		}
	}

	public static boolean reject(TraceContext ctx, Object req, Object res) {
		if (plugIn != null) {
			try {
				return plugIn.reject(new ContextWrapper(ctx), new RequestWrapper(req), new ResponseWrapper(res));
			} catch (Throwable t) {
			}
		}
		return false;
	}
}
