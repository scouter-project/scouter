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

import java.util.Enumeration;

import scouter.client.daemon.api.ServerApi;
import scouter.client.net.LoginMgr;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.util.StringLinkedSet;
import scouter.util.ThreadUtil;

public class SessionDaemon extends Thread {

	private static SessionDaemon observer;
	Configure conf = Configure.getInstance();

	public synchronized static void load() {
		if (observer == null) {
			observer = new SessionDaemon();
			observer.setDaemon(true);
			observer.setName("SessionDaemon");
			observer.start();
		}
	}

	private static final long CHECK_INTERVAL = 2000;
	private long lastCheckServerTime;

	public void run() {
		while (true) {
			try {
				healthPing();
				long now = System.currentTimeMillis();
				if (now < (lastCheckServerTime + 5000)) {
					continue;
				}
				lastCheckServerTime = now;
				checkServerConf();
				Enumeration<Integer> idSet = ServerManager.getInstance().getAllServerList();
				while (idSet.hasMoreElements()) {
					int serverId = idSet.nextElement();
					Server server = ServerManager.getInstance().getServer(serverId);
					if (server == null) {
						continue;
					}
					if (server.isConnected() == false && server.getConnectionPool().size() < 1) {
						server.setSession(0); // reset session
					}
					if (server.isConnected() && server.getSession() == 0) {
						boolean success = LoginMgr.silentLogin(server, server.getUserId(), server.getPassword());
						System.out.println("Relogin... " + server.getId() + ":" + server.getPort() + "......."
								+ success);
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			ThreadUtil.sleep(CHECK_INTERVAL);
		}
	}

	private void checkServerConf() {
		StringLinkedSet serverSet = conf.scouter_server;
		ServerManager serverManager = ServerManager.getInstance();
		for (String unit : serverSet.getArray()) {
			try {
				String[] args = unit.split(":");
				String ip = args[0];
				String port = args[1];
				if (serverManager.isRunningServer(ip, port) == false) {
					Server server = new Server(ip, port);
					serverManager.addServer(server);
					System.out.println("Try to Login  Server = " + ip + ":" + port);
					boolean success = LoginMgr.login(server.getId(), args[2], args[3]);
					if (success == false) {
						System.out.println("Failed to login... " + ip + ":" + port);
						serverManager.removeServer(server.getId());
					} else {
						System.out.println("Success to login... " + ip + ":" + port);
					}
				}
			} catch (Exception e) {
			}
		}
	}

	private void healthPing() {
		Enumeration<Integer> idSet = ServerManager.getInstance().getAllServerList();
		while (idSet.hasMoreElements()) {
			ServerApi.getObjectList(idSet.nextElement());
		}
	}
}
