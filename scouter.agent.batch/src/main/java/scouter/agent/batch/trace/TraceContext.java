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
import scouter.lang.pack.BatchPack;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.MapValue;
import scouter.util.SysJMX;
import scouter.util.TimeFormatUtil;

public class TraceContext {
	private static final String SQL_OTHERS = "Others";
	private static final int SQL_OTHERS_HASH = SQL_OTHERS.hashCode();
	private static TraceContext instance = null;
	
	public String batchJobId;
	public String args;
	public Integer pID;
	
	public long startTime;
	public long endTime;
	
	public int threadCnt = 0;
	public long startCpu;
	public long endCpu;

	public long gcTime= 0L;
	public long gcCount = 0L;
	

	public int sqlTotalCnt = 0;
	public long sqlTotalTime = 0L;
	public long sqlTotalRows = 0L;
	public long sqlTotalRuns = 0L;

	public boolean isStackLogFile = false;
	public String standAloneFile = null;

	private HashMap<Integer, String> uniqueSqls = new HashMap<Integer, String>(100);
	private HashMap<Integer, TraceSQL> sqlMap = new HashMap<Integer, TraceSQL>(100);
	private List<LocalSQL> localSQLList = new ArrayList<LocalSQL>();
	
	private int sqlMaxCount;
	
	public String lastStack = null;	
	
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
		
		buffer.append("-[").append(this.batchJobId).append("]----------------------------------------------").append(lineSeparator);
		buffer.append("Run  Command: ").append(this.args).append(lineSeparator);
		if(this.isStackLogFile){
			buffer.append("Stack   Dump: ").append(this.getLogFullFilename()).append(lineSeparator);
		}
		long elapsedTime = this.endTime - this.startTime;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		buffer.append("Start   Time: ").append(sdf.format(new Date(this.startTime))).append(lineSeparator);
		buffer.append("Stop    Time: ").append(sdf.format(new Date(this.endTime))).append(lineSeparator);
		buffer.append("Elapsed Time: ").append(String.format("%,13d",elapsedTime)).append(" ms ");
		buffer.append(TimeFormatUtil.elapsedTime(elapsedTime));
		buffer.append(lineSeparator);
		if(this.getCPUTimeByMillis() > 0){
			buffer.append("CPU     Time: ").append(String.format("%,13d",this.getCPUTimeByMillis())).append(" ms ");
			if(elapsedTime > 0){
				buffer.append(String.format("%.2f", ((float)(this.getCPUTimeByMillis() * 100F)/elapsedTime))).append(" %");
			}
			buffer.append(lineSeparator);
		}
		if(this.gcCount > 0){
			buffer.append("GC     Count: ").append(String.format("%,13d",this.gcCount)).append(lineSeparator);
			buffer.append("GC      Time: ").append(String.format("%,13d",this.gcTime)).append(" ms ");
			if(elapsedTime > 0){
				buffer.append(String.format("%.2f", ((float)(this.gcTime * 100F)/elapsedTime))).append(" %");
			}
			buffer.append(lineSeparator);
		}
		
		if(sqlMap.size() > 0){
			buffer.append("SQL     Time: ").append(String.format("%,13d",(sqlTotalTime/1000000L))).append(" ms ");
			if(elapsedTime > 0){
				buffer.append(String.format("%.2f", ((float)((sqlTotalTime / 1000000F) * 100F)/elapsedTime))).append(" %");
			}
			buffer.append(lineSeparator);
			buffer.append("SQL     Type: ").append(String.format("%,13d",sqlMap.size())).append(lineSeparator);
			buffer.append("SQL     Runs: ").append(String.format("%,13d",sqlTotalRuns)).append(lineSeparator);
		}
		if(threadCnt > 0){
			buffer.append("Thread Count: ").append(String.format("%,13d",this.threadCnt)).append(lineSeparator);
		}
		
		if(sqlMap.size() > 0){
			buffer.append(lineSeparator).append("<SQLs>").append(lineSeparator);
			int index = 0;
			buffer.append("Index          Runs     TotalTime        Rate       MinTime       AvgTime       MaxTime          Rows (Measured) StartTime               EndTime").append(lineSeparator);
			buffer.append("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
			List<TraceSQL> list = sortTraceSQLList();
			for(TraceSQL traceSql : list){
				index++;
				buffer.append(lineSeparator);
				buffer.append(String.format("%5s", index)).append(' ');
				buffer.append(String.format("%,13d", traceSql.runs)).append(' ');
				buffer.append(String.format("%,13d", traceSql.getTotalTimeByMillis())).append(' ');
				if(elapsedTime == 0){
					buffer.append(String.format("%,10.2f", 0F)).append("% ");					
				}else{
					buffer.append(String.format("%,10.2f", ((100F * traceSql.getTotalTimeByMillis())/elapsedTime))).append("% ");					
				}
				if(traceSql.runs == 0 && traceSql.minTime == Long.MAX_VALUE){
					buffer.append(String.format("%,13d", 0)).append(' ');
				}else{
					buffer.append(String.format("%,13d", traceSql.getMinTimeByMillis())).append(' ');
				}
				if(traceSql.runs == 0){
					buffer.append(String.format("%,13d", 0)).append(' ');					
				}else{
					buffer.append(String.format("%,13d", (traceSql.getTotalTimeByMillis()/traceSql.runs))).append(' ');					
				}
				buffer.append(String.format("%,13d", traceSql.getMaxTimeByMillis())).append(' ');
				buffer.append(String.format("%,13d", traceSql.processedRows)).append(' ').append(String.format("%10s", traceSql.rowed)).append(' ');
				buffer.append(sdf.format(new Date(traceSql.startTime))).append(' ');
				buffer.append(sdf.format(new Date(traceSql.endTime)));
			}
			buffer.append(lineSeparator).append("----------------------------------------------------------------------------------------------------------------------------------------------------------------").append(lineSeparator);

			buffer.append(lineSeparator).append("<SQL Texts>").append(lineSeparator);
			index = 0;
			for(TraceSQL traceSql : list){
				index++;
				buffer.append("-----------------").append(lineSeparator);
				buffer.append("#SQLINX-").append(index).append(lineSeparator);
				buffer.append(uniqueSqls.get(traceSql.hashValue)).append(lineSeparator);
			}			
		}
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
					this.sqlTotalCnt++;
				}
			}
			synchronized(statsSql){
				statsSql.runs += sql.runs;
				this.sqlTotalRuns += sql.runs;
				statsSql.totalTime += sql.totalTime;
				this.sqlTotalTime += sql.totalTime;
				statsSql.processedRows += sql.processedRows;
				this.sqlTotalRows += sql.processedRows;
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
		caculateResource();
	}
	
	public void caculateResource(){
		this.endCpu = SysJMX.getProcessCPU();
		long [] gcInfo = SysJMX.getCurrentProcGcInfo();
		this.gcCount = gcInfo[0];
		this.gcTime = gcInfo[1];
	}
	
	public MapPack caculateTemp(){
		MapPack map = new MapPack();
		map.put("batchJobId", this.batchJobId);
		map.put("args", this.args);
		map.put("pID", (long)this.pID);
		map.put("startTime", this.startTime);
		map.put("elapsedTime", (System.currentTimeMillis() - this.startTime));
		map.put("cPUTime", (this.endCpu - startCpu));
		map.put("gcCount", this.gcCount);
		map.put("gcTime", this.gcTime);

		long tempSqlTotalTime = this.sqlTotalTime;
		long tempSqlTotalRows = this.sqlTotalRows;
		long tempSqlTotalRuns = this.sqlTotalRuns;
		
		synchronized(localSQLList){
			for(LocalSQL localSql : localSQLList){
				for(TraceSQL sql : localSql.values()){
					tempSqlTotalTime += sql.totalTime;
					tempSqlTotalRows += sql.processedRows;
					tempSqlTotalRuns += sql.runs;
				}
			}
		}
		

		map.put("sqlTotalTime", tempSqlTotalTime);
		map.put("sqlTotalRows", tempSqlTotalRows);
		map.put("sqlTotalRuns", tempSqlTotalRuns);		
		
		if(this.lastStack == null){
			map.put("lastStack", "None");			
		}else{
			map.put("lastStack", this.lastStack);			
		}

		return map;
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
	
	public String getLogFullFilename(){
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
	
	public BatchPack makePack(){
		BatchPack pack = new BatchPack();
		Configure config = Configure.getInstance();
		
		pack.objHash = config.getObjHash();	
		pack.objName = config.getObjName();
		pack.objType = config.obj_type;
		pack.batchJobId = this.batchJobId;
		pack.batchJobId =  this.batchJobId;
		pack.args =  this.args;
		pack.pID =  this.pID;
		pack.startTime =  this.startTime;
		pack.elapsedTime =  (this.endTime - this.startTime);
		pack.threadCnt =  this.threadCnt;
		pack.cpuTime =  (this.endCpu - this.startCpu);
		pack.gcCount = this.gcCount;
		pack.gcTime = this.gcTime;
		
		pack.sqlTotalCnt =  this.sqlTotalCnt;
		pack.sqlTotalTime =  this.sqlTotalTime;
		pack.sqlTotalRows =  this.sqlTotalRows;
		pack.sqlTotalRuns =  this.sqlTotalRuns;

		pack.isStack = isStackLogFile;
		
		if(this.sqlTotalCnt > 0){
			pack.uniqueSqls = this.uniqueSqls;		
			pack.sqlStats = new ArrayList<MapValue>((int)this.sqlTotalCnt);
			MapValue value;
			for(TraceSQL traceSql : sortTraceSQLList()){
				value = new MapValue();
				pack.sqlStats.add(value);
	
				value.put("hashValue", (long)traceSql.hashValue);
				value.put("runs", (long)traceSql.runs);
				value.put("startTime", traceSql.startTime);
				value.put("endTime", traceSql.endTime);
				value.put("totalTime", traceSql.totalTime);
				if(traceSql.runs == 0 && traceSql.minTime == Long.MAX_VALUE){
					value.put("minTime", 0);					
				}else{
					value.put("minTime", traceSql.minTime);
				}
				
				value.put("maxTime", traceSql.maxTime);
				value.put("processedRows", traceSql.processedRows);
				value.put("rowed", new BooleanValue(traceSql.rowed));
			}
		}
		return pack;
	}
}