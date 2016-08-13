/*
 *  Copyright 2016 Scouter Project.
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
package scouter.agent.batch;

import java.io.File;
import java.io.FileWriter;

import scouter.agent.batch.dump.ThreadDumpHandler;
import scouter.agent.batch.trace.TraceContext;
import scouter.util.ThreadUtil;

public class BatchMonitor extends Thread {
	private static BatchMonitor instance = null;
	public Configure config = null;
	
	static public BatchMonitor getInstance(){
		if(instance == null){
			instance = new BatchMonitor();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            
            TraceContext.getInstance();
            Runtime.getRuntime().addShutdownHook(new ResultSender());
            instance.start();
 		}
		return instance;
	}
	
	public void run() {
		FileWriter stackWriter = null;
		FileWriter indexWriter = null;
		try {		
			File stackFile = null;
			config = Configure.getInstance();			
			TraceContext traceContext = TraceContext.getInstance();
			if(config.sfa_dump_enabled){
				stackFile = new File(traceContext.getLogFullFilename() + ".log");
				if(stackFile.exists()){
					stackFile = null;
				}else{
					traceContext.stackLogFile = stackFile.getAbsolutePath();
					
					stackWriter = new FileWriter(stackFile);
					indexWriter = new FileWriter(new File(traceContext.getLogFullFilename() + ".inx"));
				}
			}
			if(stackWriter != null){
				while(!config.scouter_stop){
					ThreadDumpHandler.processDump(stackFile, stackWriter, indexWriter, config.sfa_dump_filter, config.sfa_dump_header_exists);
					traceContext.checkThread();
					Thread.sleep(config.sfa_dump_interval_ms);
				}
			}
		}catch(Throwable ex){
			Logger.println("ERROR: " + ex.getMessage());
		}finally{
			if(stackWriter != null){
				try{ stackWriter.close(); }catch(Exception ex){}	
			}
			if(indexWriter != null){
				try{ indexWriter.close(); }catch(Exception ex){}	
			}
		}
	}
}
