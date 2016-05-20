/*
 *  Copyright 2015 Scouter Project.
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

import scouter.agent.batch.trace.TraceContext;
import scouter.util.ThreadUtil;

public class BatchMonitor extends Thread {
	private static BatchMonitor instance = null;
	
	static public BatchMonitor getInstance(){
		if(instance == null){
			instance = new BatchMonitor();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            
            // start Job Info
            TraceContext traceContext = TraceContext.getInstance();

            instance.start();		
		}
		return instance;
	}
	public void run() {
	}
}
