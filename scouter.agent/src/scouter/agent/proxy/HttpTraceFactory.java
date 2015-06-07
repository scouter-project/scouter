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
package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;

public class HttpTraceFactory {
	private static final String HTTP_TRACE = "scouter.xtra.http.HttpTrace";

	public static final IHttpTrace dummy = new IHttpTrace() {
		public String getParameter(Object req, String key) {
			return null;
		}

		public void start(TraceContext ctx, Object req, Object res) {
		}

		public void end(TraceContext ctx, Object req, Object res) {
		}

		public void rejectText(Object res, String text) {
		}

		public void rejectUrl(Object res, String url) {
		}
	};;

	public static IHttpTrace create(ClassLoader parent) {
		try {
			ClassLoader loader = LoaderManager.getHttpLoader(parent);
			if (loader == null) {
				return dummy;
			}
			Class c = Class.forName(HTTP_TRACE, true, loader);
			return (IHttpTrace) c.newInstance();
		} catch (Throwable e) {
			Logger.println("A135", "fail to create", e);
			return dummy;
		}
	}

}
