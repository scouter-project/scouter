/*
 *  Copyright 2016 the original author or authors. 
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

package scouter.agent.batch.trace;

public class TraceContextManager {

	private static ThreadLocal<LocalSQL> local = new ThreadLocal<LocalSQL>();

	public static LocalSQL getLocalSQL(){
		LocalSQL localSql = local.get();
		if(localSql == null){
			localSql = new LocalSQL();
			local.set(localSql);
			TraceContext.getInstance().addLocalSQL(localSql);
		}
		return localSql;
	}
	
	public static void endThread(){
		LocalSQL localSql = local.get();
		local.set(null);
		TraceContext.getInstance().removeLocalSQL(localSql);
	}
	
	public static TraceSQL getTraceSQL(String sqlText){
		TraceSQL traceSql = getLocalSQL().get(sqlText);
		return traceSql;
	}
	
	public static TraceSQL getCurrentTraceSQL(){
		return getLocalSQL().getCurrentTraceSQL();
	}
	
	public static TraceSQL startTraceSQL(String sqlText){
		TraceSQL traceSql = getTraceSQL(sqlText);
		traceSql.start();
		return traceSql;
	}
}