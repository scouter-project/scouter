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

package scouter.agent.batch.task;

import java.io.File;
import java.io.FileWriter;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.agent.batch.netio.data.net.UdpLocalAgent;
import scouter.agent.batch.trace.TraceContext;

public class ResultSender extends Thread {
	public void run(){
		Configure config = null;
		TraceContext traceContext = null;
		long elapsedTime = 0L;

		try {
			config = Configure.getInstance();
			config.scouter_stop = true;
			
			traceContext = TraceContext.getInstance();
			traceContext.endTime = System.currentTimeMillis();
			elapsedTime = (traceContext.endTime - traceContext.startTime);
			
			if(config.batch_log_send_elapsed_ms <= elapsedTime){
				traceContext.caculateLast();
				String result = traceContext.toString();
				if(config.scouter_standalone || config.sbr_log_make){
					saveStandAloneResult(traceContext, result);
				}
				Logger.println(result);
			}
		}catch(Throwable ex){
			Logger.println("Scouter ResultSender(run) Exception: " + ex.getMessage());
		}finally{
			try {
				if(config != null && traceContext != null){
					if(!config.scouter_standalone ){
						if(config.batch_log_send_elapsed_ms <= elapsedTime){					
							if(config.sfa_dump_enabled && config.sfa_dump_send_elapsed_ms > elapsedTime){
								traceContext.isStackLogFile = false;
							}
							UdpLocalAgent.sendUdpPackToServer(traceContext.makePack());
						}
						if(config.sfa_dump_enabled && config.sfa_dump_send_elapsed_ms <= elapsedTime){
							UdpLocalAgent.sendDumpFileInfo(traceContext);
						}
					}
					if(config.sfa_dump_enabled && config.sfa_dump_send_elapsed_ms > elapsedTime){
						deleteFiles(traceContext);
					}
					UdpLocalAgent.sendEndInfo(traceContext);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	public void saveStandAloneResult(TraceContext traceContext, String result){
		File resultFile = new File(traceContext.getLogFullFilename() + ".sbr");
		if(resultFile.exists()){
			return;
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(resultFile);
			writer.write(result);
		}catch(Throwable ex){
			Logger.println("Scouter ResultSender(save) Exception: " + ex.getMessage());
		}finally{
			if(writer != null){
				try{ writer.close(); }catch(Exception ex){}
			}
		}
	}
	
	public void deleteFiles(TraceContext traceContext){
		String filename = traceContext.getLogFullFilename();
		try{
			File file = new File(filename + ".log");
			file.delete();
			file = new File(filename + ".inx");
			file.delete();
		}catch(Throwable ex){
			Logger.println("Scouter ResultSender(delete) Exception: " + ex.getMessage());
		}
	}
}
