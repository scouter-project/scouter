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

import scouter.agent.Logger;
import scouter.agent.trace.HookPoint;
import scouter.agent.trace.TraceContext;
import scouter.lang.pack.XLogPack;

abstract public class IServiceTrace {
	long lastModified;

	public void start(TraceContext ctx, HookPoint p) {
	}

	public void end(TraceContext ctx, XLogPack p) {
	}
	
	public void log(Object c) {
		Logger.println("PLUG-IN", c.toString());
	}

	public void println(Object c) {
		System.out.println(c);
	}
}