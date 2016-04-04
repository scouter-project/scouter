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
package scouter.client.server;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import scouter.client.net.TcpProxy;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.HashUtil;
import scouter.util.LinkedMap;
import scouter.util.ThreadUtil;

public class ServerManager extends Thread {
	
	private static volatile ServerManager instance;
	private LinkedMap<Integer, Server> serverMap = new LinkedMap<Integer, Server>();
	
	public static ServerManager getInstance() {
		if (instance == null) {
			synchronized (ServerManager.class) {
				if (instance == null) {
					instance = new ServerManager();
					instance.setName(ThreadUtil.getName(instance));
					instance.setDaemon(true);
					instance.start();
				}
			}
		}
		return instance;
	}
	
	
	private ServerManager() {}
	
	public Server addServer(Server server) {
		return serverMap.put(server.getId(), server);
	}
	
	public Server getDefaultServer() {
		return serverMap.getFirstValue();
	}
	
	public boolean setDefaultServer(Server server) {
		   serverMap.putFirst(server.getId(), server);
		   return true;
	}
	
	public void run() {
		while (true) {
			syncServerTime();
			ThreadUtil.sleep(2000);
		}
	}
	
	private void syncServerTime() {
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			if (server.isConnected() && server.isOpen()) {
				TcpProxy tcp = TcpProxy.getTcpProxy(server.getId());
				try {
					MapPack p = (MapPack) tcp.getSingle(RequestCmd.SERVER_STATUS, null);
					if (p != null) {
						long time = p.getLong("time");
						if (time > 0) {
							server.setDelta(time);
						}
						long usedMemory = p.getLong("used");
						long totalMemory = p.getLong("total");
						server.setUsedMemory(usedMemory);
						server.setTotalMemory(totalMemory);
					}
				} catch (Throwable th) {
					th.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
		}
	}


	public Server getServer(int id) {
		return serverMap.get(id);
	}
	
	public Server getServer(String addr) {
		String[] iport = addr.split(":");
		if (iport == null || iport.length < 2) {
			return null;
		}
		return getServer(HashUtil.hash(iport[0] + iport[1]));
	}
	
	public void removeServer(int serverId) {
		Server remove = serverMap.remove(serverId);
		if (remove != null) {
			remove.close();
			System.out.println("Remove server : " + remove.getName());
		}
	}
	
	public Set<Integer> getOpenServerList() {
		Set<Integer> keySet = new HashSet<Integer>();
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			if (server.isOpen()) {
				keySet.add(server.getId());
			}
		}
		return keySet;
	}
	
	public Enumeration<Integer> getAllServerList() {
		return serverMap.keys();
	}
	
	public boolean isRunningServer(String ip, String port) {
		int hash = HashUtil.hash(ip + port);
		Server server = serverMap.get(hash);
		if (server == null) {
			return false;
		}
		return server.isOpen();
	}
	
	public void printAll() {
		System.out.println("******************************");
		System.out.println("SERVER LIST");
		System.out.println("******************************");
		Enumeration<Integer> itr = serverMap.keys();
		while (itr.hasMoreElements()) {
			Integer key = itr.nextElement();
			Server server = serverMap.get(key);
			
			System.out.println("ID : " + key);
			System.out.println("IP : " + server.getIp());
			System.out.println("Port : " + server.getPort());
			System.out.println("Connected : " + server.isConnected());
			System.out.println("Session : " + server.getSession());
			System.out.println("------------------------------------------");
		}
	}


	public void shutdown() {
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			server.close();
		}
		serverMap.clear();
	}
}
