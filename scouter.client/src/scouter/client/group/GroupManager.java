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
package scouter.client.group;

import scouter.client.util.ClientFileUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.util.CacheTable;
import scouter.util.FileUtil;
import scouter.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupManager {
	private File groupFile = new File(RCPUtil.getWorkingDirectory(), ClientFileUtil.GROUP_FILE);
	private static volatile GroupManager instance;
	private Set<String> reserved = new HashSet<String>();
	
	private MapPack objGroupMap;
	private MapValue groupTypeMap;
	
	private static final String KEY_OBJTYPE = "_objType_";
	public static final String OTHERS = "Others";

	public static GroupManager getInstance() {
		if (instance == null) {
			synchronized (GroupManager.class) {
				if (instance == null) {
					instance = new GroupManager();
				}
			}
		}
		return instance;
	}
	
	private GroupManager() {
		reserved.add(KEY_OBJTYPE);
		reserved.add(OTHERS);
		loadGroupFile();
	}
	
	private synchronized void loadGroupFile() {
		try {
			if (groupFile.exists()) {
				byte[] bytes = FileUtil.readAll(groupFile);
				if (bytes != null) {
					objGroupMap = (MapPack) new DataInputX(bytes).readPack();
					groupTypeMap = (MapValue) objGroupMap.get(KEY_OBJTYPE);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (objGroupMap == null) {
			objGroupMap = new MapPack();
			groupTypeMap = new MapValue();
			objGroupMap.put(KEY_OBJTYPE, groupTypeMap);
		}
	}
	
	private void saveGroupFile() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				try {
					byte[] b = new DataOutputX().writePack(objGroupMap).toByteArray();
					if (groupFile.exists()) {
						groupFile.delete();
					}
					FileUtil.save(groupFile, b);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public boolean addGroup(String objType, String groupName) {
		if (reserved.contains(groupName)) {
			return false;
		}
		groupName = groupName.trim();
		if (StringUtil.isEmpty(groupName)) {
			return false;
		}
		groupTypeMap.put(groupName, objType);
		saveGroupFile();
		return true;
	}
	
	public boolean addObject(int objHash, String groupName) {
		if (groupTypeMap.get(groupName) != null) {
			addObjGroupMap(objHash, groupName);
			saveGroupFile();
			return true;
		}
		return false;
	}
	
	public boolean addObject(int[] objHashs, String groupName) {
		if (groupTypeMap.get(groupName) != null) {
			for (int objHash : objHashs) {
				addObjGroupMap(objHash, groupName);
			}
			saveGroupFile();
			return true;
		}
		return false;
	}
	
	private void addObjGroupMap(int objHash, String groupName) {
		String objStr = String.valueOf(objHash);
		Value v2 = objGroupMap.get(objStr);
		if (v2 != null && v2 instanceof ListValue) {
			ListValue lv = (ListValue) v2;
			lv.add(groupName);
		} else {
			ListValue lv = objGroupMap.newList(objStr);
			lv.add(groupName);
		}
	}
	
	public List<String> listGroup() {
		List<String> groupList = new ArrayList<String>();
		Enumeration<String> groups = groupTypeMap.keys();
		while (groups.hasMoreElements()) {
			String name = groups.nextElement();
			groupList.add(name);
		}
		return groupList;
	}
	
	public Set<String> getObjGroups(int objHash) {
		List<Value> groupList = new ArrayList<Value>();
		Set<String> groups = new HashSet<String>();
		Value v = objGroupMap.get(String.valueOf(objHash));
		if (v != null && v instanceof ListValue) {
			ListValue lv = (ListValue) v;
			for (int i = 0; i < lv.size(); i++) {
				TextValue groupName = (TextValue) lv.get(i);
				if (groupTypeMap.containsKey(groupName.value)) {
					groupList.add(groupName);
					groups.add(groupName.value);
				}
			}
			objGroupMap.put(String.valueOf(objHash), new ListValue(groupList));
		}
		return groups;
	}
	
	public String getGroupObjType(String groupName) {
		return groupTypeMap.getText(groupName);
	}
	
	public Set<String> getObjTypeList() {
		Set<String> typeSet = new LinkedHashSet<String>();
		Enumeration<String> groupList = groupTypeMap.keys();
		while (groupList.hasMoreElements()) {
			String group = groupList.nextElement();
			String objType = groupTypeMap.getText(group);
			if (objType != null) {
				typeSet.add(objType);
			}
		}
		return typeSet;
	}
	
	public void removeGroup(String groupName) {
		if (groupTypeMap.remove(groupName) != null) {
			saveGroupFile();
		}
	}
	
	private void removeObject(int objHash, String groupName) {
		Value v = objGroupMap.get(String.valueOf(objHash));
		if (v != null && v instanceof ListValue) {
			ListValue groups = (ListValue) v;
			ListValue newLv = new ListValue();
			for (int i = 0; i < groups.size(); i++) {
				String name = groups.getString(i);
				if (groupName.equals(name)) {
					continue;
				}
				newLv.add(name);
			}
			objGroupMap.put(String.valueOf(objHash), newLv);
		}
	}
	
	public void removeObject(int[] objHashs, String groupName) {
		for (int objHash : objHashs) {
			removeObject(objHash, groupName);
		}
		saveGroupFile();
	}
	
	public void assginGroups(int objHash, String[] groups) {
		ListValue groupList = new ListValue();
		for (String group : groups) {
			groupList.add(group);
		}
		objGroupMap.put(String.valueOf(objHash), groupList);
		saveGroupFile();
	}
	
	public Set<String> getGroupsByType(String objType) {
		Set<String> groupSet = new HashSet<String>();
		Enumeration<String> names = groupTypeMap.keys();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if(objType.equals(groupTypeMap.getText(name))) {
				groupSet.add(name);
			}
		}
		return groupSet;
	}
	
	private CacheTable<String, Set> groupObjTable = new CacheTable<String, Set>().setDefaultKeepTime(10000);
	
	public Set<Integer> getObjectsByGroup(String grpName) {
		Set objSet = groupObjTable.get(grpName);
		if (objSet != null) {
			return objSet;
		}
		objSet = new HashSet<Integer>();
		Iterator<String> keys = objGroupMap.keys();
		while (keys.hasNext()) {
			String objHash = keys.next();
			Value v = objGroupMap.get(objHash);
			if (v != null && v instanceof ListValue) {
				ListValue groupLv = (ListValue) v;
				for (int i = 0; i < groupLv.size(); i++) {
					if (grpName.equals(groupLv.getString(i))) {
						objSet.add(Integer.valueOf(objHash));
						break;
					}
				}
			}
		}
		groupObjTable.put(grpName, objSet);
		return objSet;
	}
	
	public void printAll() {
		System.out.println("********************************");
		System.out.println(objGroupMap);
		System.out.println("********************************");
		ConsoleProxy.infoSafe(objGroupMap.toString());
	}
}
