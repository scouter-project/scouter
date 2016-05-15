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
package scouter.client.threads;

import java.util.Set;

import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.util.ThreadUtil;

public class SessionObserver extends Thread {
	
	private static SessionObserver observer;
	
	public synchronized static void load() {
		if (observer == null) {
			observer = new SessionObserver();
			observer.setDaemon(true);
			observer.setName("SessionObserverThread");
			observer.start();
		}
	}
	
	private static final long CHECK_INTERVAL = 5000;

	public void run() {
		while (true) {
			try {
				Set<Integer> idSet = ServerManager.getInstance().getOpenServerList();
				for (int serverId : idSet) {
					Server server = ServerManager.getInstance().getServer(serverId);
					if (server == null) {
						continue;
					}
					if (server.isConnected() == false && server.getConnectionPool().size() < 1) {
						server.setSession(0); // reset session
					}
					if (/*server.isConnected() &&*/ server.getSession() == 0) {
						LoginResult result = LoginMgr.silentLogin(server, server.getUserId(), server.getPassword());
						if (result.success) {
							ConsoleProxy.infoSafe("Success re-login to " + server.getName());
						} else {
							ConsoleProxy.errorSafe("Failed re-login to " + server.getName() + " : " + result.getErrorMessage());
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			ThreadUtil.sleep(CHECK_INTERVAL);
		}
	}
}
