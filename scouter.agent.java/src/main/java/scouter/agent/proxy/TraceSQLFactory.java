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
import scouter.agent.trace.SqlParameter;

public class TraceSQLFactory {
	private static final String TRACE_SQL = "scouter.xtra.jdbc.TraceSQL0";

	public static final ITraceSQL dummy = new ITraceSQL() {
		@Override
		public Exception getSlowSqlException() {
			return null;
		}
		@Override
		public Exception getTooManyRecordException() {
			return null;
		}
		@Override
		public Exception getConnectionOpenFailException() {
			return null;
		}
		@Override
		public Object start(Object o, String sql, byte methodType) {
			return null;
		}
		@Override
		public Object start(Object o, SqlParameter args, byte methodType) {
			return null;
		}
		@Override
		public Object driverConnect(Object conn, String url) {
			return null;
		}
		@Override
		public Object getConnection(Object conn) {
			return null;
		}
		@Override
		public Object dbcOpenEnd(Object conn, Object stat) {
			return null;
		}
		@Override
		public void ctxLookup(Object this1, Object ctx) { }
	};

	public static ITraceSQL create(ClassLoader parent) {
		try {
			ClassLoader loader = LoaderManager.getJdbcLoader(parent);
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
