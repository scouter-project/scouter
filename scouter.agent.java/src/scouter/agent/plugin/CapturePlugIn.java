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

public class CapturePlugIn {

	static ICapture plugIn;

	static {
		PlugInLoader.getInstance();
	}

	public static void capArgs(TraceContext ctx, String className, String methodName, String methodDesc, Object[] arg) {
		if (plugIn != null) {
			try {
				plugIn.capArgs(new ContextWrapper(ctx), className, methodName, methodDesc, arg);
			} catch (Throwable t) {
			}
		}
	}

	public static void capReturn(TraceContext ctx, String className, String methodName, String methodDesc, Object data) {
		if (plugIn != null) {
			try {
				plugIn.capReturn(new ContextWrapper(ctx), className, methodName, methodDesc, data);
			} catch (Throwable t) {
			}
		}
	}

	public static void capThis(TraceContext ctx, String className, String methodDesc, Object data) {
		if (plugIn != null) {
			try {
				plugIn.capThis(new ContextWrapper(ctx), className, methodDesc, data);
			} catch (Throwable t) {
			}
		}
	}

}
