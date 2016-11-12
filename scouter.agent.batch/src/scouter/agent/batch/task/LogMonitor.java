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
				deleteLogFiles(config.sfa_dump_dir, currentTime, logKeepTime, new String [] {".log", ".inx", ".sbr"});
				deleteLogFiles(new File(config.log_dir), currentTime, logKeepTime, new String [] {".log"});
				Thread.sleep(3600000L);
			}
		}catch(Throwable ex){
			Logger.println("ERROR: " + ex.getMessage());
		}
	}
	
	private void deleteLogFiles(File dir, long currentTime, long logKeeptime, String [] fileExtension){
		long lastTime;
		String fileName;
		try{
			File [] list = 	dir.listFiles();
			if(list == null){
				return;
			}
			for(File file :list){
				fileName = file.getName();
				if(fileName.equals(".") || fileName.equals("..")){
					continue;
				}
				
				if(file.isDirectory()){
					deleteLogFiles(file, currentTime, logKeeptime, fileExtension);
					continue;
				} else if(!checkExtenstion(fileName, fileExtension)){
					continue;
				}
				lastTime = file.lastModified();
				if((currentTime - lastTime) >= logKeeptime){
					file.delete();
					Logger.println("LOG: delete file - " + file.getAbsolutePath());			
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			Logger.println("ERROR: LogDelete - " + ex.getMessage());			
		}
		
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
