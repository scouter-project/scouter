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
package scouter.agent.batch.netio.data.net;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import scouter.agent.batch.Configure;
import scouter.util.ThreadUtil;

public class TcpAgentReqMgr extends Thread{
	private static ConcurrentLinkedQueue<byte []> jobQueue = new ConcurrentLinkedQueue<byte []>();
	private static List<File> fileList = new ArrayList<File>();
	
	private static TcpAgentReqMgr instance;

	public static synchronized TcpAgentReqMgr getInstance() {
		if (instance == null) {
			instance = new TcpAgentReqMgr();
			instance.setName("SCOUTER-TCP-REQ-MGR");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}

	protected Executor pool = ThreadUtil.createExecutor("SCOUTER-REQ-STACK", Configure.getInstance().net_tcp_stack_session_count, 10000, true);

	@Override
	public void run() {
		while (true) {			
			int sessionCount = Configure.getInstance().net_tcp_stack_session_count;
			ThreadUtil.sleep(1000);
			try {
				for (int i = 0; i < sessionCount && TCPStackZipWorker.LIVE.size() < sessionCount; i++) {
					TCPStackZipWorker w = new TCPStackZipWorker(this);
					if (w.prepare(false)) {
						pool.execute(w);
					} else {
						ThreadUtil.sleep(3000);
					}
				}
				while (TCPStackZipWorker.LIVE.size() > sessionCount) {
					TCPStackZipWorker w = TCPStackZipWorker.LIVE.removeFirst();
					w.close(true);
				}
				deleteFiles();
			} catch (Throwable t) {
			}
		}
	}
	
	private void deleteFiles(){
		int size = fileList.size();
		if(size == 0)
			return;
		
		synchronized(fileList){
			for(int i = size - 1; i >=0; i++){
				if(fileList.get(i).delete()){
					fileList.remove(i);
				}
			}
		}
		
	}
	
	public void addJob(byte [] job){
		jobQueue.add(job);
		//System.out.println("AddJob");		
		synchronized(jobQueue){
			jobQueue.notify();
		}
	}
	
	public void addFile(File file){
		synchronized(fileList){
			fileList.add(file);
		}
	}	

	public byte [] getJob(){
		byte [] job = null;
		while((job = jobQueue.poll()) == null){
			try{ 
				synchronized(jobQueue){
					jobQueue.wait(3000); 
				}
			}catch(Exception ex){};
		}
		//System.out.println("GetJob->" + job.length);	
		return job;
	}
}