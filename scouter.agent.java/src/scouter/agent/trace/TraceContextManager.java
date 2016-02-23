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

package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.util.LongEnumer;
import scouter.util.LongKeyMap;

import java.util.Enumeration;

public class TraceContextManager {

	private static LongKeyMap<TraceContext> entry = new LongKeyMap<TraceContext>();
	private static ThreadLocal<TraceContext> local = new ThreadLocal<TraceContext>();

	public static LongEnumer keys() {
		return entry.keys();
	}

	public static int size() {
		return entry.size();
	}

	public static int[] getActiveCount() {
		int[] act = new int[3];
		try {
			Configure conf = Configure.getInstance();
			long now = System.currentTimeMillis();
			Enumeration<TraceContext> en = entry.values();
			while (en.hasMoreElements()) {
				TraceContext ctx = en.nextElement();
				long tm = now - ctx.startTime;
				if (tm < conf.trace_activeserivce_yellow_time) {
					act[0]++;
				} else if (tm < conf.trace_activeservice_red_time) {
					act[1]++;
				} else {
					act[2]++;
				}
			}
		} catch (Throwable t) {
		}
		return act;
	}

	public static Enumeration<TraceContext> getContextEnumeration() {
		return entry.values();
	}

	public static TraceContext getContext(long key) {
		return entry.get(key);
	}

	public static TraceContext getContext() {
		return  local.get();
	}

	public static long start(Thread thread, TraceContext o) {
		long key = thread.getId();
		local.set(o);
		entry.put(key, o);
		return key;
	}

	public static void end(long key) {
		local.set(null);
		entry.remove(key);
	}
}