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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.util.SysJMX;

public class TraceContext {
	private static final String SQL_OTHERS = "Others";
	private static final int SQL_OTHERS_HASH = SQL_OTHERS.hashCode();
	private static TraceContext instance = null;
	
	static {
		instance = new TraceContext();
	}
	
	final public static TraceContext getInstance(){
		return instance;
	}
	
	private TraceContext() {
		startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		
		readBatchId();
		sqlMaxCount = Configure.getInstance().sql_max_count;
		pID = SysJMX.getProcessPID();
		startCpu = SysJMX.getProcessCPU();
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
		String lineSeparator = System.getProperty("line.separator");
		
		buffer.append(lineSeparator);
		buffer.append("-[Result]----------------------------------------------").append(lineSeparator);
		buffer.append("Batch     ID: ").append(this.batchJobId).append(lineSeparator);
		buffer.append("Run  Command: ").append(this.args).append(lineSeparator);
		if(this.stackLogFile != null){
			buffer.append("Stack   Dump: ").append(this.stackLogFile.getAbsolutePath()).append(lineSeparator);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		buffer.append("Start   Time: ").append(sdf.format(new Date(this.startTime))).append(lineSeparator);
		buffer.append("Stop    Time: ").append(sdf.format(new Date(this.endTime))).append(lineSeparator);
		buffer.append("Elapsed Time: ").append((this.endTime - this.startTime)).append("ms").append(lineSeparator);
		if(this.getCPUTimeByMillis() > 0){
			buffer.append("CPU     Time: ").append(this.getCPUTimeByMillis()).append("ms").append(lineSeparator);
		}
		if(sqlMap.size() > 0){
			long SqlTime = 0L;
			int sqlRunCount = 0;
		
			for(TraceSQL traceSql : sqlMap.values()){
				SqlTime += traceSql.totalTime;
				sqlRunCount += traceSql.count;
			}
			buffer.append("SQL     Time: ").append((SqlTime/1000000L)).append("ms").append(lineSeparator);
			buffer.append("SQL     Type: ").append(sqlMap.size()).append(lineSeparator);
			buffer.append("SQL RunCount: ").append(sqlRunCount).append(lineSeparator);
		}
		if(threadCnt > 0){
			buffer.append("Thread Count: ").append(this.threadCnt).append(lineSeparator);
		}
		
		if(sqlMap.size() > 0){
			buffer.append(lineSeparator).append("<SQLs>").append(lineSeparator);
			int index = 0;
			
			for(TraceSQL traceSql : sortTraceSQLList()){
				index++;
				buffer.append("-----------").append(lineSeparator);
				buffer.append(index).append(':').append(uniqueSqls.get(traceSql.hashValue)).append(lineSeparator);
				buffer.append("Start Time:").append(sdf.format(new Date(traceSql.startTime))).append(lineSeparator);
				buffer.append("End   Time:").append(sdf.format(new Date(traceSql.endTime))).append(lineSeparator);
				buffer.append("Count     :").append(traceSql.count).append(lineSeparator);
				buffer.append("Total Time:").append(traceSql.getTotalTimeByMillis()).append(lineSeparator);
				buffer.append("Min   Time:").append(traceSql.getMinTimeByMillis()).append(lineSeparator);
				buffer.append("Max   Time:").append(traceSql.getMaxTimeByMillis()).append(lineSeparator);
				buffer.append("Rows      :").append(traceSql.processedRows).append('(').append(traceSql.rowed).append(')').append(lineSeparator);	
			}
		}
		buffer.append("-------------------------------------------------------").append(lineSeparator);
		return buffer.toString();
	}
	
	public void addSQLStats(LocalSQL localSql){	
		if(localSql == null || localSql.size() == 0){
			return;
		}
		
		Integer hashValue;
		TraceSQL statsSql;
		for(TraceSQL sql : localSql.values()){
			hashValue = sql.hashValue;
			synchronized(sqlMap){
				statsSql = sqlMap.get(hashValue);
				if(statsSql == null){
					statsSql = new TraceSQL();
					statsSql.hashValue = hashValue;
					sqlMap.put(hashValue, statsSql);
				}
			}
			synchronized(statsSql){
				statsSql.count += sql.count;
				statsSql.totalTime += sql.totalTime;
				statsSql.processedRows += sql.processedRows;
				if( statsSql.startTime > sql.startTime || statsSql.startTime == -1L){
					statsSql.startTime = sql.startTime;
				}
				if(statsSql.endTime < sql.endTime){
					statsSql.endTime = sql.endTime;
				}
				if(statsSql.minTime > sql.minTime){
					statsSql.minTime = sql.minTime;
				}
				if(statsSql.maxTime < sql.maxTime){
					statsSql.maxTime = sql.maxTime;
				}
				if(sql.rowed){
					statsSql.rowed = true;
				}
			}
		}
	}
	
	public void caculateLast(){
		synchronized(localSQLList){
			for(LocalSQL localSql : localSQLList){
				addSQLStats(localSql);
			}
			localSQLList.clear();
		}
		this.endCpu = SysJMX.getProcessCPU();
	}
	
	public void checkThread(){
		Thread thread;
		LocalSQL localSql;
		int inx;
		
		synchronized(localSQLList){
			for(inx = localSQLList.size() - 1; inx >=0; inx--){
				localSql = localSQLList.get(inx);
				thread = localSql.getThread();
				if(!thread.isAlive()){
					addSQLStats(localSql);
					localSQLList.remove(inx);
				}
			}
		}
	}	
	
	public void addLocalSQL(LocalSQL localSql){
		synchronized(localSQLList){
			localSQLList.add(localSql);
			threadCnt++;
		}
	}

	public void removeLocalSQL(LocalSQL localSql){
		synchronized(localSQLList){
			localSQLList.remove(localSql);
		}
		addSQLStats(localSql);
	}
	
	public List<LocalSQL> getLocalSQLList(){
		return localSQLList;
	}
	
	public long getCPUTimeByMicro(){
		return ((endCpu - startCpu)/1000L);
	}
	
	public long getCPUTimeByMillis(){
		return ((endCpu - startCpu)/1000000L);
	}
	
	public String getLogFilename(){
		Date dt = new Date(startTime);
		String fileSeparator = System.getProperty("file.separator");
		String date = new SimpleDateFormat("yyyyMMdd").format(dt);
		
		File dir = new File(new StringBuilder(100).append(Configure.getInstance().sfa_dump_dir.getAbsolutePath()).append(fileSeparator).append(date).toString());
		if(!dir.exists()){
			dir.mkdirs();
		}
		return new StringBuilder(100).append(dir.getAbsolutePath()).append(fileSeparator).append(batchJobId).append('_').append(date).append('_').append(new SimpleDateFormat("HHmmss.SSS").format(dt)).append('_').append(pID).toString();	
	}
	
	public List<TraceSQL> sortTraceSQLList(){
		List<TraceSQL> inList = new ArrayList<TraceSQL>(sqlMap.size() + 1);
		synchronized(sqlMap){
			for(TraceSQL traceSql : sqlMap.values()){
				inList.add(traceSql);
			}
		}
		Collections.sort(inList,
				new Comparator<TraceSQL>(){
					@Override
					public int compare(TraceSQL o1, TraceSQL o2) {
						if(o1.totalTime < o2.totalTime){
							return 1;
						}else if(o1.totalTime > o2.totalTime){
							return -1;
						}
						return 0;
					}
					
				});
		return inList;
	}
	
	public String batchJobId;
	public String args;
	public Integer pID;
	
	public long startTime;
	public long endTime;
	
	public int threadCnt = 0;
	public long startCpu;
	public long endCpu;

	public File stackLogFile = null;
	public File standAloneFile = null;

	private HashMap<Integer, String> uniqueSqls = new HashMap<Integer, String>(100);
	private HashMap<Integer, TraceSQL> sqlMap = new HashMap<Integer, TraceSQL>(100);
	private List<LocalSQL> localSQLList = new ArrayList<LocalSQL>();
	
	private int sqlMaxCount;
	
	public String lastStack;
}