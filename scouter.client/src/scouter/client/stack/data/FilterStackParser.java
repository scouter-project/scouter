/*
 *  Copyright 2015 the original author or authors. 
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
 *
 */
package scouter.client.stack.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import scouter.client.stack.config.ParserConfig;
import scouter.client.stack.utils.StringUtils;


public class FilterStackParser extends StackParser {
		
	public FilterStackParser(){
		
	}
	
	public FilterStackParser(ParserConfig config){
		super(config);
	}
	
	public void process(){
	    BufferedReader reader = null;
	    try {
	    	reader = new BufferedReader(new FileReader(new File(getStackFileInfo().getFilename())));
	    	
	    	StringBuilder  timeBuffer = null;
	    	String line = null;
	    	boolean isWorking = false;
	    	ArrayList<String> workingList = new ArrayList<String>(300);
	    	
	    	ArrayList<String> workerThread = getWorkerThread();	    	
	    	String filter = getFilter();
	    	ThreadStatusInfo tsinfo = new ThreadStatusInfo();
	    	
	    	int lineCount = 0;
	    	int workerCount = 0;
	    	int workingCount = 0;
	    	int totalWorkerCount = 0;
	    	int dumpCount = 0;
	    	
            ParserConfig config = getStackFileInfo().getParserConfig();
            int stackStartLine = config.getStackStartLine();
            int timeSize = config.getTimeSize();
            
	    	while((line = reader.readLine()) != null){
	    		progressBar();
	    		if(line.trim().length() == 0){
	    			if(isWorking && lineCount > stackStartLine){ 
	    				processStack(workingList, tsinfo);
	    			}
	    			isWorking = false;
	    			workingList = new ArrayList<String>(300);
	    			lineCount = 0;
	    			continue;
	    		}
	    		
	    		// Dump time
	    		if(lineCount == 0){
	    			if(line.length() == timeSize){
	    					if(timeBuffer != null  && timeBuffer.length() > 10){
	    						timeBuffer.append('\t').append(workerCount).append('\t').append(workingCount);
	    						for(int tsIndex = 0; tsIndex < tsinfo.geSize(); tsIndex++){
	        						timeBuffer.append('\t').append(tsinfo.getValue(tsIndex));    							
	    						}
	    						addTime(timeBuffer.toString());
	    					}
	    					timeBuffer = new StringBuilder(50);
	    					timeBuffer.append(line);
	    					tsinfo = new ThreadStatusInfo();
	    					writeTime(line);
	    					workerCount = 0;
	    					workingCount = 0;
	    					dumpCount++;
	    			}else{
	    				if(StringUtils.checkExist(line, workerThread)){
	    					workerCount++;
	    					totalWorkerCount++;
	    				}
	    			}	
	    		}
	    		
	    		// Working Thread
	    		if(!isWorking && line.indexOf(filter) >= 0){
	    			isWorking = true;
	    			workingCount++;
	    		}

	    		workingList.add(line);
	    		lineCount++;
	    	}
	    	
	    	// last stack
	    	if(isWorking && lineCount > stackStartLine){
				processStack(workingList, tsinfo);	    		
	    	}
	    	
	    	// last time
			if(timeBuffer != null && timeBuffer.length() > 10){
				timeBuffer.append('\t').append(workerCount).append('\t').append(workingCount);
				for(int tsIndex = 0; tsIndex < tsinfo.geSize(); tsIndex++){
					timeBuffer.append('\t').append(tsinfo.getValue(tsIndex));    							
				}				
				addTime(timeBuffer.toString());
			}
			
			setTotalWorkerCount(totalWorkerCount);
			setDumpCount(dumpCount);
	    }catch(Exception ex){
	    	throw new RuntimeException(ex);
	    }finally{
	    	if(reader != null){
	    		try { reader.close();}catch(Exception e){}
	    	}
	    }
	}
}
