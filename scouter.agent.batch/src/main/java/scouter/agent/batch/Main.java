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
package scouter.agent.batch;

import java.io.File;
import java.util.Hashtable;

import scouter.Version;
import scouter.agent.batch.netio.data.net.UdpLocalAgent;
import scouter.agent.batch.netio.data.net.UdpLocalServer;
import scouter.agent.batch.netio.request.ReqestHandlingProxy;
import scouter.agent.batch.netio.service.net.TcpRequestMgr;
import scouter.agent.batch.task.LogMonitor;
import scouter.agent.batch.task.StatusSender;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;

public class Main {
	static public Hashtable<String, MapPack> batchMap = new Hashtable<String, MapPack>();
	
	public static void main(String[] args) {		
		Logo.print(true);
		System.out.println("Scouter Batch Agent Version " + Version.getServerFullVersion());
		Logger.println("A01", "Scouter Batch Agent Version " + Version.getServerFullVersion());
		try {
			ReqestHandlingProxy.load(ReqestHandlingProxy.class);
			UdpLocalServer.getInstance();
			TcpRequestMgr.getInstance();
			
			File exit = new File(SysJMX.getProcessPID() + ".scouter");
			try {
				exit.createNewFile();
			} catch (Exception e) {
				String tmp = System.getProperty("user.home", "/tmp");
				exit = new File(tmp, SysJMX.getProcessPID() + ".scouter.run");
				try {
					exit.createNewFile();
				} catch (Exception k) {
					System.exit(1);
				}
			}
			exit.deleteOnExit();
			long startTime = System.currentTimeMillis();
			long currentTime;
	
			LogMonitor.getInstance();
				
			StatusSender statusSender = new StatusSender();
			while (true) {
				currentTime = System.currentTimeMillis();
				if((currentTime - startTime) >= 10000){
					UdpLocalAgent.sendUdpPackToServer(getObjectPack());
					startTime = currentTime;
				}
				if (exit.exists() == false) {
					System.exit(0);
				}
				statusSender.sendBatchService(currentTime);
				ThreadUtil.sleep(1000);
				
			}
		}catch(Throwable th){
			Logger.println("E001", "Abnormal stop:" + th.getMessage(), th);
		}
	}
	
	static public ObjectPack getObjectPack(){
		Configure conf = Configure.getInstance();
		ObjectPack pack = new ObjectPack();
		pack.alive = true;
		pack.objHash = conf.getObjHash();
		pack.objName = conf.getObjName();
		pack.objType = conf.obj_type;
		pack.version = Version.getAgentFullVersion();
		return pack;
	}
}
