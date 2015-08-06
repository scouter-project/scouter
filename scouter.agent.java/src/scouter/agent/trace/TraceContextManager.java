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

package scouter.agent.trace;

import java.util.Enumeration;
import java.util.Hashtable;

import scouter.agent.Configure;

public class TraceContextManager {

	private static Hashtable<Long, TraceContext> entry = new Hashtable<Long, TraceContext>(107);
	private static ThreadLocal<TraceContext> local = new ThreadLocal<TraceContext>();

	public static Enumeration<Long> keys() {
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
			Enumeration<TraceContext> en = entry.elements();
			while (en.hasMoreElements()) {
				TraceContext ctx = en.nextElement();
				long tm = now - ctx.startTime;
				if (tm < conf.yellow_line_time) {
					act[0]++;
				} else if (tm < conf.red_line_time) {
					act[1]++;
				} else {
					act[2]++;
				}
			}
		} catch (Throwable t) {
		}
		return act;
	}
	
	public static Enumeration<TraceContext> getContextEnumeration(){
		return entry.elements();
	}

	public static TraceContext getContext(long key) {
		return (TraceContext) entry.get(key);
	}

	public static TraceContext getLocalContext() {
		return (TraceContext) local.get();
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