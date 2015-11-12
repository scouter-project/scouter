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

import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;

public class PluginAppServiceTrace {

	static AbstractAppService plugIn;

	static {
		PluginLoader.getInstance();
	}

	public static void start(TraceContext ctx, HookArgs hook) {
		if (plugIn != null) {
			try {
				plugIn.start(new WrContext(ctx), hook);
			} catch (Throwable t) {
			}
		}
	}

	public static void end(TraceContext ctx) {
		if (plugIn != null) {
			try {
				plugIn.end(new WrContext(ctx));
			} catch (Throwable t) {
			}
		}
	}

}