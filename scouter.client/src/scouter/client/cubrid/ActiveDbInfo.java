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
package scouter.client.cubrid;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import scouter.util.HashUtil;

public class ActiveDbInfo {

	private static ActiveDbInfo instance;

	private static Hashtable <Integer, Hashtable<String, String>> ServerInfo = new Hashtable<>(); 

	public synchronized static ActiveDbInfo getInstance() {
		if (instance == null) {
			instance = new ActiveDbInfo();
		}
		return instance;
	}

	private ActiveDbInfo() {
	}

	public void addServerInfo(int serverId) {
		if (ServerInfo.get(serverId) == null) {
			Hashtable<String, String> dbInfo = new Hashtable<>();
			ServerInfo.put(serverId, dbInfo);
		}
	}

	public ArrayList<String> getDbList(int serverId) {
		ArrayList<String> dbList = new ArrayList<>();
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);

		if (dbInfo != null) {
			for (String dbName : dbInfo.keySet()) {
				dbList.add(dbName);
			}
			
			return dbList;
		}
		
		return null;
	}

	public Set<String> keySet(int serverId) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			return dbInfo.keySet();
		}
		
		return null;
	}

	public String getObjectName(int serverId, String dbname) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			return String.valueOf(dbInfo.get(dbname));
		}
		
		return null;
	}

	public int getObjectHash(int serverId, String dbname) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null && dbInfo.get(dbname) != null) {
			return HashUtil.hash(dbInfo.get(dbname));
		} else {
			return -1;
		}
	}

	public void setActiveDBInfo(int serverId, Hashtable<String, String> info) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			dbInfo = info;
		}
	}

	public boolean isEmpty(int serverId) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			return dbInfo.isEmpty();
		} else {
			return true;
		}
	}

	public int size(int serverId) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			return dbInfo.size();
		} else {
			return 0;
		}
	}

	public void clear(int serverId) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo != null) {
			dbInfo.clear();
		}
	}

	public void put(int serverId, String dbName, String ObjectName) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		if (dbInfo != null) {
			dbInfo.put(dbName, ObjectName);
		} 
	}

	public Hashtable<String, String> getActiveDBInfo(int serverId) {
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		return dbInfo;
	}

	public boolean equals(int serverId, Hashtable<String, String> other) {
		if (other == null)
			return false;
		
		Hashtable<String, String> dbInfo = ServerInfo.get(serverId);
		
		if (dbInfo == null)
			return false;

		if (dbInfo.equals(other)) {
			return true;
		}

		return false;
	}
}
