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
package scouter.client.model;

import java.util.ArrayList;
import java.util.HashMap;

import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class AgentDailyListProxy {
	public ArrayList<AgentObject> getObjectList(String date, int serverId) {
		ArrayList<AgentObject> agentList = new ArrayList<AgentObject>();
		TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			MapPack out = (MapPack) proxy.getSingle(RequestCmd.OBJECT_LIST_LOAD_DATE, param);
			ListValue objTypeLv = out.getList("objType");
			ListValue objHashLv = out.getList("objHash");
			ListValue objNameLv = out.getList("objName");
			if (objHashLv == null || objHashLv.size() < 0) {
				return agentList;
			}
			for (int i = 0; i < objHashLv.size(); i++) {
				int objHash = (int) objHashLv.getLong(i);
				String objType = objTypeLv.getString(i);
				String objName = objNameLv.getString(i);
				AgentObject agentObject = new AgentObject(objType, objHash, objName,  serverId);
				agentList.add(agentObject);
			}
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(proxy);
		}
		return agentList;
	}
	
	public AgentObject getAgentObject(String date, int serverId, int objHash) {
		TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			MapPack out = (MapPack) proxy.getSingle(RequestCmd.OBJECT_LIST_LOAD_DATE, param);
			ListValue objTypeLv = out.getList("objType");
			ListValue objHashLv = out.getList("objHash");
			ListValue objNameLv = out.getList("objName");
			if (objHashLv == null || objHashLv.size() < 0) {
				return null;
			}
			for (int i = 0; i < objHashLv.size(); i++) {
				int hash = (int) objHashLv.getLong(i);
				if (objHash == hash) {
					String objType = objTypeLv.getString(i);
					String objName = objNameLv.getString(i);
					return new AgentObject(objType, objHash, objName,  serverId);
				}
			}
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(proxy);
		}
		return null;
	}
	
	static class Key {
		String date;
		int serverId;
		String objType;
		Key (String date, int serverId, String objType) {
			this.date = date;
			this.serverId = serverId;
			this.objType = objType;
		}
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((date == null) ? 0 : date.hashCode());
			result = prime * result
					+ ((objType == null) ? 0 : objType.hashCode());
			result = prime * result + serverId;
			return result;
		}
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (date == null) {
				if (other.date != null)
					return false;
			} else if (!date.equals(other.date))
				return false;
			if (objType == null) {
				if (other.objType != null)
					return false;
			} else if (!objType.equals(other.objType))
				return false;
			if (serverId != other.serverId)
				return false;
			return true;
		}
	}
	
	HashMap<Key, ListValue> cacheMap = new HashMap<Key, ListValue>();
	
	public ListValue getObjHashLv(String date, int serverId, String objType) {
		Key key = new Key(date, serverId, objType);
		ListValue lv = cacheMap.get(key);
		if (DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)).equals(date) == false && lv != null) {
			return lv;
		}
		lv = new ListValue();
		cacheMap.put(key, lv);
		TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			MapPack out = (MapPack) proxy.getSingle(RequestCmd.OBJECT_LIST_LOAD_DATE, param);
			ListValue objHashLv = out.getList("objHash");
			if (objHashLv == null || objHashLv.size() < 0) {
				return lv;
			}
			ListValue objTypeLv = out.getList("objType");
			for (int i = 0; i < objHashLv.size(); i++) {
				String type = objTypeLv.getString(i);
				if (objType.equals(type)) {
					int objHash = (int) objHashLv.getLong(i);
					lv.add(objHash);
				}
			}
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(proxy);
		}
		return lv;
	}
}
