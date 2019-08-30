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

public class TraceSQL {
	public static final String CURRENT_TRACESQL_FIELD = "_current_trace_sql_";
	
	public Integer hashValue;
	public int runs = 0;
	
	public long startTime = -1L;
	public long endTime = 0L;
	public long totalTime = 0L;
	public long minTime = Long.MAX_VALUE;
	public long maxTime = 0L;
	public long processedRows = 0L;
	public boolean rowed = false;
	
	public long sqlStartTime;
	
	public void start(){
		if(startTime == -1L){
			startTime = System.currentTimeMillis();
		}
		sqlStartTime = System.nanoTime();
	}
	
	public void end(){
		long responseTime = System.nanoTime() - sqlStartTime;
		runs++;
		totalTime += responseTime;
		
		if(minTime > responseTime){
			minTime = responseTime;
		}
		if(maxTime < responseTime){
			maxTime = responseTime;
		}
		endTime = System.currentTimeMillis();
	}
	
	public void addRow(){
		if(!rowed) rowed = true;
		processedRows++;
	}
	
	public void addRow(int rows){
		if(!rowed) rowed = true;
		if(rows > 0){
			processedRows += rows;
		}
	}
	
	public void addRows(int [] rows){
		if(rows == null || rows.length < 1)
			return;
		
		if(!rowed) rowed = true;
		for(int i = 0; i < rows.length; i++){
			if(rows[i] > 0){
				processedRows += rows[i];
			}else if(rows[i] == -2){
				processedRows++;				
			}
		}
	}
	
	public long getTotalTimeByMillis(){
		return totalTime / 1000000L;		
	}

	public long getTotalTimeByMicro(){
		return totalTime / 1000L;
	}

	public long getMinTimeByMillis(){
		return minTime / 1000000L;		
	}

	public long getMinTimeByMicro(){
		return minTime / 1000L;
	}

	public long getMaxTimeByMillis(){
		return maxTime / 1000000L;		
	}

	public long getMaxTimeByMicro(){
		return maxTime / 1000L;
	}
	
}
