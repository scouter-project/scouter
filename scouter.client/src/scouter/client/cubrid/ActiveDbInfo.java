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

	private static Hashtable<String, String> dbInfo = new Hashtable<>();

	public synchronized static ActiveDbInfo getInstance() {
		if (instance == null) {
			instance = new ActiveDbInfo();
		}
		return instance;
	}

	private ActiveDbInfo() {
	}

	public ArrayList<String> getDbList() {
		ArrayList<String> dbList = new ArrayList<>();
		for (String dbName : dbInfo.keySet()) {
			dbList.add(dbName);
		}

		return dbList;
	}

	public Set<String> keySet() {
		return dbInfo.keySet();
	}

	public String getObjectName(String dbname) {
		return dbInfo.get(dbname);
	}

	public int getObjectHash(String dbname) {
		if (dbInfo.get(dbname) != null) {
			return HashUtil.hash(dbInfo.get(dbname));
		} else {
			return -1;
		}
	}

	public void setActiveDBInfo(Hashtable<String, String> info) {
		dbInfo = info;
	}

	public boolean isEmpty() {
		return dbInfo.isEmpty();
	}

	public int size() {
		return dbInfo.size();
	}

	public void clear() {
		dbInfo.clear();
	}

	public void put(String dbName, String ObjectName) {
		dbInfo.put(dbName, ObjectName);
	}

	public Hashtable<String, String> getActiveDBInfo() {
		return dbInfo;
	}

	public boolean equals(Hashtable<String, String> other) {
		if (other == null)
			return false;
		if (dbInfo == null)
			return false;

		if (dbInfo.equals(other)) {
			return true;
		}

		return false;
	}
}
