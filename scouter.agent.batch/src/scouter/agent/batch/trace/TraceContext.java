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
import java.util.List;

import scouter.agent.batch.Configure;

public class TraceContext {
	private static TraceContext instance = null;
	
	final public static TraceContext getInstance(){
		if(instance == null){
			instance = new TraceContext();
		}
		return instance;
	}
	
	private TraceContext() {
		startTime = System.currentTimeMillis();
		
		Configure config = Configure.getInstance();
		if("props".equals(config.batch_id_type)){
			batchJobId = config.getValue(config.batch_id);
		}else if("args".equals(config.batch_id_type)){	
			List<String> list = ManagementFactory.getRuntimeMXBean().getInputArguments();
			int index = Integer.parseInt(config.batch_id);
			if(list != null && list.size() > index){
				batchJobId = list.get(index);
			}
		}else if("class".equals(config.batch_id_type)){
			 ManagementFactory.getRuntimeMXBean();
		}
		
		if(batchJobId == null || batchJobId.length() == 0){
			batchJobId ="NoId[Scouter]";
		}
		System.out.println("JobID="+  batchJobId);
		try{
			throw new Exception("test");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		System.getProperties().list(System.out);
	}

	public String batchJobId;
	public String args;
	
	public long startTime;
	public long endTime;
	
	public int maxThreadCnt = 0;
	public long startCpu;
	public long endCpu;

	public long bytes;
	public int status;

	public int error;

	// sql
	public int sqlCount;
	public int sqlTime;
	public String sqltext;

	// apicall
	public String apicall_name;
	public int apicall_count;
	public int apicall_time;
	public String apicall_target;
}