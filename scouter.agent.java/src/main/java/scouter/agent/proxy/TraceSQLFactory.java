/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.agent.proxy;

import scouter.agent.Logger;

public class TraceSQLFactory {
	private static final String TRACE_SQL = "scouter.xtra.jdbc.TraceSQL0";

	public static final ITraceSQL dummy = new ITraceSQL() {
	};

	public static ITraceSQL create(ClassLoader parent) {
		try {
			ClassLoader loader = LoaderManager.getHttpLoader(parent);
			if (loader == null) {
				return dummy;
			}

			Class c = Class.forName(TRACE_SQL, true, loader);
			return (ITraceSQL) c.newInstance();

		} catch (Throwable e) {
			Logger.println("A134", "fail to create ITraceSQL", e);
			return dummy;
		}
	}

}
