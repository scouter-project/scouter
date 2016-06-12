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

import scouter.client.net.ConnectionPool;
import scouter.lang.counters.CounterEngine;
import scouter.lang.value.MapValue;
import scouter.util.HashUtil;

public class Server {
	final private int id;
	private String name;
	private String ip;
	private int port;
	private ConnectionPool connPool = new ConnectionPool();
	private boolean connected = false;
	private long session;
	private long delta;
	private CounterEngine counterEngine = new CounterEngine();
	
	private String timezone;
	private String userId;
	private String encryptedPass;
	private String group;
	private String version;
	private String email;
	private boolean secureMode = true;
	
	private boolean open = false;
	
	private long usedMemory;
	private long totalMemory;
	private boolean dirty = false;
	
	private int soTimeOut = 8000;
	
	private MapValue groupPolicyMap = new MapValue();
	private MapValue menuEnableMap = new MapValue();
	
	public Server(String ip, String port) {
		this(ip, port, null);
	}
	
	public Server(String ip, String port, String name) {
		this.id = HashUtil.hash(ip + port);
		this.ip = ip;
		this.port = Integer.valueOf(port);
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	
	public String getIp() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public ConnectionPool getConnectionPool() {
		return this.connPool;
	}
	
	public void close() {
		this.connPool.closeAll();
	}
	
	public long getSession() {
		return session;
	}
	
	public void setSession(long session) {
		this.session = session;
	}
	
	public long getDelta() {
		return delta;
	}
	
	public void setDelta(long serverTime) {
		if (serverTime < 1) {
			this.delta = 0;
		} else {
			this.delta = serverTime - System.currentTimeMillis();
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void setConnected(boolean isConnected) {
		this.connected = isConnected;
	}
	
	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		if (this.name == null) {
			return ip + ":" + port;
		}
		return this.name;
	}
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getPassword() {
		return encryptedPass;
	}

	public void setPassword(String password) {
		this.encryptedPass = password;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setGroupPolicy(MapValue mv) {
		this.groupPolicyMap = mv;
	}
	
	public boolean isAllowAction(String name) {
		return groupPolicyMap.getBoolean(name);
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public CounterEngine getCounterEngine() {
		return counterEngine;
	}
	
	public long getUsedMemory() {
		return usedMemory;
	}

	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}
	
	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}
	
	public void setMenuEnableMap(MapValue mv) {
		this.menuEnableMap = mv;
	}
	
	public boolean isEnableMenu(String key) {
		return menuEnableMap.getBoolean(key);
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public boolean isDirty() {
		return this.dirty;
	}
	
	public long getCurrentTime() {
		return System.currentTimeMillis() + getDelta();
	}
	
	public int getSoTimeOut() {
		return soTimeOut;
	}

	public void setSoTimeOut(int soTimeOut) {
		this.soTimeOut = soTimeOut;
	}
	
	public boolean isSecureMode() {
		return secureMode;
	}

	public void setSecureMode(boolean secureMode) {
		this.secureMode = secureMode;
	}

	public String toString() {
		return "Server [id=" + id + ", name=" + name + ", ip=" + ip + ", port="
				+ port + ", connected=" + connected + ", delta=" + delta
				+ ", userId=" + userId + ", group=" + group + ", version="
				+ version + "]";
	}
}
