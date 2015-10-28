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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import scouter.client.net.TcpProxy;
import scouter.util.StringUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.Hexa32;
import scouter.util.LinkedMap;

public class TextModel {

	final private String cmd = RequestCmd.GET_TEXT_100;
	final private String type;
	private int limit;

	public TextModel(String textType, int limit) {
		this.type = textType;
		if (limit <= 0)
			limit = Integer.MAX_VALUE;
		this.limit = limit;
	}
	
	public void setLimit(int limit){
		if (limit <= 0)
			limit = Integer.MAX_VALUE;
		this.limit = limit;
	}
	
	public int getLimit(){
		return limit;
	}

	private LinkedMap<Integer, String> entries = new LinkedMap<Integer, String>();

	public void putText(int id, String name) {
		entries.put(id, name);
	}

	public String getText(int id) {
		return entries.get(id);
	}

	public boolean load(String date, Collection<Integer> hashs, int serverId) {
		if (hashs == null || hashs.size() == 0)
			return false;
		MapPack param = new MapPack();
		param.put("date", date);
		param.put("type", type);
		ListValue hashLv = param.newList("hash");
		Iterator<Integer> itr = hashs.iterator();
		while (itr.hasNext()) {
			int key = itr.next();
			if (entries.containsKey(key) == false) {
				hashLv.add(key);
			}
		}
		
		if (hashLv.size() == 0)
			return false;
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		List<Pack> packList = null;
		try {
			packList = tcp.process(cmd, param);
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (packList == null)
			return false;
		
		for (Pack pack : packList) {
			MapPack re = (MapPack) pack;
			Iterator<String> en = re.keys();
			while (en.hasNext()) {
				String key = en.next();
				String value = re.getText(key);
				if (StringUtil.isNotEmpty(value)) {
					entries.put((int) Hexa32.toLong32(key), value);
					if (entries.size() > limit){
						entries.removeFirst();
					}
				}
			}
		}

		return true;
	}

	public String getLoadText(String date, int hash, int serverId) {
		if (hash == 0)
			return null;
		String value = getText(hash);
		if (value != null)
			return value;
		ArrayList a = new ArrayList();
		a.add(hash);
		load(date, a, serverId);
		return getText(hash);
	}

	public void load(String date, ListValue hashList, int serverId) {
		ArrayList<Integer> arrList = new ArrayList<Integer>();
		for (int i = 0; i < hashList.size(); i++) {
			int hash = (int) hashList.getLong(i);
			if (hash != 0) {
				arrList.add(hash);
			}
		}
		load(date, arrList, serverId);

	}
}
