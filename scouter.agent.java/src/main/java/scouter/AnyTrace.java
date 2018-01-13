/*
 *  Copyright 2015 Scouter Project.
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
 *  
 *  @author skyworker
 */
package scouter;

import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.TraceApiCall;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.lang.pack.XLogTypes;
import scouter.util.KeyGen;

public class AnyTrace {

	public static void message(String name) {
		TraceMain.addMessage(name);
	}

	public static Object startService(String name) {
		return TraceMain.startService(name, null, null, null, null, null, XLogTypes.APP_SERVICE);
	}

	public static void setServiceName(String name) {
		TraceMain.setSpringControllerName(name);
	}

	public static void serviceError(String emsg) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null && ctx.error != 0) { // already started
			ctx.error = DataProxy.sendError(emsg);
		}
	}

	public static void endService(Object stat, Throwable thr) {
		TraceMain.endService(stat, null, thr);
	}

	public static Object startMethod(int hash, String classMethodName) {
		return TraceMain.startMethod(hash, classMethodName);
	}

	public static void endMethod(Object stat, Throwable thr) {
		TraceMain.endMethod(stat, thr);
	}

	public static long createTxid() {
		return KeyGen.next();
	}

	public static Object startApicall(String name, long apiTxid) {
		return TraceApiCall.startApicall(name, apiTxid);
	}

	public static void setApicallName(String name) {
		try {
			TraceContext ctx = TraceContextManager.getContext();
			if (ctx != null) {
				if (ctx.apicall_name != null) { // already started subcall only
					ctx.apicall_name = name;
				}
			}
		} catch (Throwable t) {// ignore
		}
	}

	public static void endApicall(Object stat, Throwable thr) {
		TraceApiCall.endApicall(stat, null, thr);
	}

	public static void desc(String desc) {
		try {
			TraceContext ctx = TraceContextManager.getContext();
			if (ctx != null) {
				ctx.desc = desc;
			}
		} catch (Throwable t) {// ignore
		}
	}

	public static void login(String login) {
		try {
			TraceContext ctx = TraceContextManager.getContext();
			if (ctx != null) {
				ctx.login = login;
			}
		} catch (Throwable t) {// ignore
		}
	}
}