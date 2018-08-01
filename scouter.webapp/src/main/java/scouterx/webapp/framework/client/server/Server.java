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

import lombok.extern.slf4j.Slf4j;
import scouter.lang.counters.CounterEngine;
import scouter.lang.value.MapValue;
import scouter.util.HashUtil;
import scouterx.webapp.framework.client.net.ConnectionPool;
import scouterx.webapp.framework.client.thread.XLogRetrieveThread;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

@Slf4j
public class Server {
	ConfigureAdaptor conf = ConfigureManager.getConfigure();

	final private int id;
	private String name;
	private String ip;
	private int port;
	private ConnectionPool connPool;
	private long session;
	private long delta;
	private CounterEngine counterEngine;

	private XLogRetrieveThread xLogRetrieveThread;
	
	private String timezone;
	private String userId;
	private String encryptedPass;
	private String group;
	private String version;
	private String recommendedClientVersion;
	private String email;
	private boolean secureMode = true;
	
	private boolean open = false;
	
	private long usedMemory;
	private long totalMemory;
	private boolean dirty = false;
	
	private int soTimeOut = 15000;
	
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
		this.connPool = new ConnectionPool(conf.getNetWebappTcpClientPoolSize());
		this.counterEngine = new CounterEngine();
		this.xLogRetrieveThread = new XLogRetrieveThread(this);
		xLogRetrieveThread.start();
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

	public String getRecommendedClientVersion() {
		return this.recommendedClientVersion;
	}
	public void setRecommendedClientVersion(String recommendedClientVersion) {
		this.recommendedClientVersion = recommendedClientVersion;
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

	public void setCounterEngine(CounterEngine counterEngine) {
		this.counterEngine = counterEngine;
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

	@Override
	public String toString() {
		return "Server [id=" + id + ", name=" + name + ", ip=" + ip + ", port="
				+ port + ", delta=" + delta
				+ ", userId=" + userId + ", group=" + group + ", version="
				+ version + "]";
	}
}
