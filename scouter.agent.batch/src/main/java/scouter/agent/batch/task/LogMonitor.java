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

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.util.ThreadUtil;

public class LogMonitor extends Thread {
	private static LogMonitor instance = null;
	
	static public LogMonitor getInstance(){
		if(instance == null){
			instance = new LogMonitor();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));            
            instance.start();
 		}
		return instance;
	}
	
	public void run() {
		long logKeepTime = 0;
		long currentTime = 0;
		
		try {		
			Configure config = Configure.getInstance();
			
			while(!config.scouter_stop){
				currentTime = System.currentTimeMillis();
				logKeepTime = (config.log_keep_days * 86400000L); 
				deleteLogFiles(config.sfa_dump_dir, currentTime, logKeepTime, new String [] {".log", ".inx", ".sbr"}, true, 0);
				deleteLogFiles(new File(config.log_dir), currentTime, logKeepTime, new String [] {".log"}, false, 0);
				Thread.sleep(3600000L);
			}
		}catch(Throwable ex){
			Logger.println("ERROR: " + ex.getMessage());
		}
	}
	
	private boolean deleteLogFiles(File dir, long currentTime, long logKeeptime, String [] fileExtension, boolean isDeleteEmpty, int index){
		long lastTime;
		String fileName;
		int listSize = 0;
		int deleteSize = 0;
		try{
			File [] list = 	dir.listFiles();
			if(list == null){
				return false;
			}
			listSize = list.length;
			for(File file :list){
				fileName = file.getName();
				if(fileName.equals(".") || fileName.equals("..")){
					deleteSize++;
					continue;
				}
				
				if(file.isDirectory()){
					if(deleteLogFiles(file, currentTime, logKeeptime, fileExtension, isDeleteEmpty, (index + 1))){
						file.delete();
						deleteSize++;
						Logger.println("LOG: delete directory - " + file.getAbsolutePath());			
					}
					continue;
				} else if(!checkExtenstion(fileName, fileExtension)){
					continue;
				}
				lastTime = file.lastModified();
				if((currentTime - lastTime) >= logKeeptime){
					file.delete();
					deleteSize++;
					Logger.println("LOG: delete file - " + file.getAbsolutePath());			
				}
			}
		}catch(Exception ex){
			Logger.println("ERROR: LogDelete - " + ex.getMessage());
		}
		if(listSize == deleteSize){
			return true;
		}
		return false;
	}
	
	private boolean checkExtenstion(String fileName, String [] fileExtenstion){
		if(fileExtenstion == null){
			return false;
		}
		for(String extension : fileExtenstion){
			if(fileName.endsWith(extension)){
				return true;
			}
		}
		return false;
	}
}
