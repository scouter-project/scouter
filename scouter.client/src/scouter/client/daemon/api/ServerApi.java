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
package scouter.client.daemon.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.io.DataInputX;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;

public class ServerApi {
	public static List<Pack> getObjectList(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return  tcp.process(RequestCmd.OBJECT_LIST_REAL_TIME, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static List<Pack> getObjectList(int serverId, String type) {
		List<Pack> typeList = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			List<Pack> agentList = tcp.process(RequestCmd.OBJECT_LIST_REAL_TIME, null);
			for (int i = 0; agentList != null && i < agentList.size(); i++) {
				ObjectPack m = (ObjectPack) agentList.get(i);
				String objType = m.objType;
				if(type != null && !"".equals(type) && type.equals(objType)){
					typeList.add(m);
				}
			}
			return typeList;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack getObjectDailyList(MapPack param, int serverId){
		TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) proxy.getSingle(RequestCmd.OBJECT_LIST_LOAD_DATE, param);
		}catch(Exception e){}
		return null;
	}
	
	public static long last_loop = 0;
	public static int last_index = 0;
	public static ArrayList<AlertPack> getAlerts(MapPack param, int serverId){
		final ArrayList<AlertPack> alerts = new ArrayList<AlertPack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack packet = null;
		try {
			tcp.process(RequestCmd.ALERT_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack packet = in.readPack();
					if (packet instanceof MapPack) {
						MapPack param = (MapPack) packet;
						last_loop = param.getLong("loop");
						last_index = param.getInt("index");
					} else {
						alerts.add((AlertPack) packet);
					}
				}
			});
			return alerts;
		} catch(Exception e){
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack getCounterXmlServer(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		try {
			p = tcp.getSingle(RequestCmd.GET_XML_COUNTER, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return (MapPack) p;
	}
}
