/*
 *  Copyright 2015 the original author or authors.
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
package scouter.client.daemon;
/*
 * 
 *   java -cp scouter.common.jar:scouter.client.jar 
 *           -Dscouter_server=127.0.0.1:6100:admin:admin  
 *           scouter.client.daemon.Main
 * 
 *   java -cp scouter.common.jar:scouter.client.jar 
 *          -Dscouter_config=scouter.conf 
 *           scouter.client.daemon.Main
 * 
 *  ##scouter.conf
 *       scouter_server=127.0.0.1:6100:admin:admin 
 *       
 */
import java.io.File;

import scouter.client.daemon.test.WasTest;
import scouter.client.server.ServerManager;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

public class Main {
	public static void main(String[] args) {
		Configure.getInstance();
		SessionDaemon.load();

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
		System.out.println("System JRE version : " + System.getProperty("java.version"));
		/////////////////////
		WasTest.test();
		////////////////////
		while (true) {
			if (exit.exists() == false) {
				ServerManager.getInstance().shutdown();
				System.exit(0);
			}
			ThreadUtil.sleep(1000);
		}

	}
}
