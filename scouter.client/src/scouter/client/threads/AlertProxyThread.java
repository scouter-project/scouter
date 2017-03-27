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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.io.DataInputX;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BooleanValue;
import scouter.net.RequestCmd;
import scouter.util.ThreadUtil;

public class AlertProxyThread extends Thread {
	
	private static AlertProxyThread thread;
	
	ArrayList<IAlertListener> listeners = new ArrayList<IAlertListener>();
	
	Map<Integer, MapPack> paramMap = new HashMap<Integer, MapPack>();
	
	public synchronized static AlertProxyThread getInstance() {
		if (thread == null) {
			thread = new AlertProxyThread();
			thread.setDaemon(true);
			thread.setName(ThreadUtil.getName(thread));
			thread.start();
		}
		return thread;
	}
	
	public void addAlertListener(IAlertListener listener) {
		listeners.add(listener);
	}
	
	public void removeAlertListener(IAlertListener listener) {
		listeners.remove(listener);
	}
	
	public void reset() {
		paramMap.clear();
	}
	
	boolean running = true;

	public void run() {
		while (running) {
			Set<Integer> serverSet = ServerManager.getInstance().getOpenServerList();
			for (final int serverId : serverSet) {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = paramMap.get(serverId);
					if (param == null) {
						param = new MapPack();
						paramMap.put(serverId, param);
						param.put("first", new BooleanValue(true));
					}
					tcp.process(RequestCmd.ALERT_REAL_TIME, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Pack packet = in.readPack();
							if (packet instanceof MapPack) {
								MapPack param = (MapPack) packet;
								paramMap.put(serverId, param);
							} else {
								final AlertPack alert = (AlertPack) packet;
								for (IAlertListener listener : listeners) {
									listener.ariseAlert(serverId, alert);
								}
							}
						}
					});
				} catch (Throwable th) {
					ConsoleProxy.errorSafe("AlertProxyThread : " + th.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
			ThreadUtil.sleep(2000);
		}
	}
	
	public interface IAlertListener {
		public void ariseAlert(int serverId, AlertPack alert);
	}
}
