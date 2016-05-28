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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
		try {			
			config = Configure.getInstance();			
			if(!config.dump_enabled){
				return;
			}
			
			TraceContext traceContext = TraceContext.getInstance();
			Date dt = new Date(traceContext.startTime);
			String fileSeparator = System.getProperty("file.separator");
			String date = new SimpleDateFormat("yyyyMMdd").format(dt);
			
			File dir = new File(new StringBuilder(100).append(config.dump_dir.getAbsolutePath()).append(fileSeparator).append(date).toString());
			if(!dir.exists()){
				dir.mkdirs();
			}
			File stackFile = new File(new StringBuilder(100).append(dir.getAbsolutePath()).append(fileSeparator).append(traceContext.batchJobId).append('_').append(date).append('_').append(new SimpleDateFormat("HHmmss.SSS").format(dt)).append(".log").toString());
			if(stackFile.exists()){
				return;
			}
			traceContext.stackLogFile = stackFile;
			while(!config.scouter_stop){
				ThreadDumpHandler.processDump(stackFile, config.dump_filter, config.dump_header_exists);
				Thread.sleep(config.dump_interval_ms);
			}
		}catch(Throwable ex){
			Logger.println("ERROR: " + ex.getMessage());
		}
	}
}
