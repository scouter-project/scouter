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

public class LocalSQL extends java.util.HashMap<Integer, TraceSQL>{
	private static final long serialVersionUID = 1L;
	private String currentThreadName;
	private TraceSQL currentTraceSql;
	private Thread currentThread;
	
	public LocalSQL() {
		super(100);
		this.currentThread = Thread.currentThread();
		this.currentThreadName = this.currentThread.getName();
	}
	
	public TraceSQL get(String sqlText){
		int hashValue = sqlText.hashCode();
		TraceSQL traceSql = super.get(hashValue);
		if(traceSql == null){
			hashValue = TraceContext.getInstance().getSQLHash(sqlText, hashValue);
			
			traceSql = new TraceSQL();
			traceSql.hashValue = hashValue;
			super.put(hashValue, traceSql);
		}
		currentTraceSql = traceSql;
		return traceSql;
	}
	
	public TraceSQL getCurrentTraceSQL(){
		return currentTraceSql;
	}
	
	public Thread getThread(){
		return currentThread;
	}
	
	public String toString(){
		return currentThreadName + ": " + super.toString();
	}
}
