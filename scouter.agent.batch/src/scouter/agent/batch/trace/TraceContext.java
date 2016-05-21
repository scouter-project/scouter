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

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;

public class TraceContext {
	private static final String SQL_OTHERS = "Others";
	private static final int SQL_OTHERS_HASH = "Others".hashCode();
	private static TraceContext instance = null;
	
	final public static TraceContext getInstance(){
		if(instance == null){
			instance = new TraceContext();
		}
		return instance;
	}
	
	private TraceContext() {
		startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		
		readBatchId();
		sqlMaxCount = Configure.getInstance().sql_max_count;
	}
	
	private void readBatchId(){
		Configure config = Configure.getInstance();
		if("props".equals(config.batch_id_type)){
			batchJobId = config.getValue(config.batch_id);
		}else{
			args = System.getProperty("sun.java.command");
			StringTokenizer token = new StringTokenizer(args, " ");
			if("args".equals(config.batch_id_type)){
				int index = Integer.parseInt(config.batch_id);
				int currentIndex = -1;
				while(token.hasMoreTokens()){
					if(currentIndex == index){
						batchJobId = token.nextToken();
						break;
					}else{
						token.nextToken();
					}
					currentIndex++;
				}
			}else if("class".equals(config.batch_id_type)){
				if(token.hasMoreTokens()){
					batchJobId = token.nextToken();
				}
			}
		}
		
		if(batchJobId == null || batchJobId.length() == 0){
			batchJobId ="NoId[Scouter]";
		}
		Logger.println("Batch ID="+  batchJobId);		
	}
	
	public int getSQLHash(String sqltext, int hashValue){
		synchronized(uniqueSqls){
			if(uniqueSqls.get(hashValue) != null){
				return hashValue;
			}
	
			if(uniqueSqls.size() < sqlMaxCount){
				uniqueSqls.put(hashValue, sqltext);
				return hashValue;
			}else if(uniqueSqls.size() == sqlMaxCount){
				uniqueSqls.put(SQL_OTHERS_HASH, SQL_OTHERS);
			}
			return SQL_OTHERS_HASH;
		}
	}
	
	public HashMap<Integer, String> getUniqueSQLs(){
		return uniqueSqls;
	}
	
	public String toString(){
		StringBuilder buffer = new StringBuilder(100);
		
		buffer.append("\r\n");
		buffer.append("-[Result]----------------------------------------------\r\n");
		buffer.append("Batch     ID: ").append(this.batchJobId).append("\r\n");
		buffer.append("Run  Command: ").append(this.args).append("\r\n");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		buffer.append("Start   Time: ").append(sdf.format(new Date(this.startTime))).append("\r\n");
		buffer.append("Stop    Time: ").append(sdf.format(new Date(this.endTime))).append("\r\n");
		buffer.append("Elapsed Time: ").append((this.endTime - this.startTime)).append("ms\r\n");
		buffer.append("-------------------------------------------------------\r\n");
		return buffer.toString();
	}
	
	public String batchJobId;
	public String args;
	
	public long startTime;
	public long endTime;
	
	public int threadCnt = 0;
	public long startCpu;
	public long endCpu;

	private HashMap<Integer, String> uniqueSqls = new HashMap<Integer, String>(100);
	private int sqlMaxCount;
}