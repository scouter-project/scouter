/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouterx.webapp.framework.client.server;

import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.HashUtil;
import scouter.util.LinkedMap;
import scouter.util.ThreadUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.exception.ErrorStateBizException;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		Server firstValue = serverMap.getFirstValue();
		if (firstValue == null) {
			throw new ErrorStateBizException(ErrorState.COLLECTOR_NOT_CONNECTED);
		}
		return firstValue;
	}
	
	public boolean setDefaultServer(Server server) {
		   serverMap.putFirst(server.getId(), server);
		   return true;
	}

	@Override
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
			if (server.isOpen() && server.getSession() != 0) {
				TcpProxy tcp = TcpProxy.getTcpProxy(server);
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
					TcpProxy.close(tcp);
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
	
	public Set<Integer> getOpenServerIdList() {
		Set<Integer> serverSet = new HashSet<Integer>();
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			if (server.isOpen()) {
				serverSet.add(server.getId());
			}
		}
		return serverSet;
	}

	public List<Server> getAllServerList() {
		List<Server> serverSet = new ArrayList<>();
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			serverSet.add(server);
		}
		return serverSet;
	}

	public Set<Integer> getClosedServerIdList() {
		Set<Integer> serverSet = new HashSet<Integer>();
		Enumeration<Server> servers = serverMap.values();
		while (servers.hasMoreElements()) {
			Server server = servers.nextElement();
			if (!server.isOpen()) {
				serverSet.add(server.getId());
			}
		}
		return serverSet;
	}

	public int getServerCount() {
		return serverMap.size();
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

	/**
	 * Get a server, if over one server, throw exception.
	 * @return Server
	 */
	public Server getServerIfNullDefault(Server server) {
		if (server == null) {
			if (getServerCount() != 1) {
				throw new ValidationException("Multiple server connected web-app requires the server to request for.");
			}
			return getDefaultServer();
		} else {
			return server;
		}
	}

	/**
	 * Get a server, if over one server, throw exception.
	 * @return Server
	 */
	public Server getServerIfNullDefault(int serverId) {
		return getServerIfNullDefault(getServer(serverId));
	}
}
