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
package scouterx.webapp.framework.client.model;

import org.apache.commons.lang3.StringUtils;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.TextPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.LinkedMap;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.net.TcpProxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TextModel {
	private static ThreadLocal<Set<Integer>> failedHashesInScope = new ThreadLocal<>();
	private static ThreadLocal<Boolean> scopeStarted = new ThreadLocal<>();

	/**
	 * start boundary of thread-scope cache of failed id to obtain word from dictionary
	 */
	public static void startScope() {
		failedHashesInScope.set(new HashSet<>());
		scopeStarted.set(true);
	}

	/**
	 * end boundary of thread-scope cache of failed id to obtain word from dictionary
	 */
	public static void endScope() {
		failedHashesInScope.set(null);
		scopeStarted.set(false);
	}

	public static boolean isScopeStarted() {
		return scopeStarted.get();
	}

	public static void addFailedList(int hash) {
		failedHashesInScope.get().add(hash);
	}

	public static boolean isFailedInScope(int hash) {
		return failedHashesInScope.get().contains(hash);
	}

	private LinkedMap<Integer, String> entries = new LinkedMap<>();

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

	public String getTextIfNullDefault(long date, int hash, int serverId) {
		if(hash == 0) {
			return "";
		}

		String text = getText(date, hash, serverId);
		if(StringUtils.isBlank(text)) {
			text = new StringBuilder("**unlabeled**:").append(TextTypeEnum.of(this).getTypeName()).append(":").append(hash).toString();
		}
		return text;
	}

	public String getText(long date, int hash, int serverId) {
		if (hash == 0) {
			return null;
		}

		String value = getCachedText(hash);
		if (value != null) {
			return value;
		}

		ArrayList list = new ArrayList();
		list.add(hash);
		String yyyymmdd = DateUtil.yyyymmdd(date);
		load(yyyymmdd, list, serverId);

		return getCachedText(hash);
	}

	public String getCachedText(int hash) {
		return entries.get(hash);
	}

	public String getCachedTextIfNullDefault(int hash) {
		if (hash == 0) {
			return "";
		}
		String s = entries.get(hash);
		if(s == null) {
			s = new StringBuilder("**unlabeled**:").append(TextTypeEnum.of(this).getTypeName()).append(":").append(hash).toString();
		}
		return s;
	}

	public boolean load(String date, Collection<Integer> hashes, int serverId) {
		if (hashes == null || hashes.size() == 0)
			return false;
		MapPack param = new MapPack();
		param.put("date", date);
		param.put("type", type);
		ListValue hashLv = param.newList("hash");
		Iterator<Integer> itr = hashes.iterator();

		Set<Integer> failedSet = failedHashesInScope.get();
		while (itr.hasNext()) {
			int key = itr.next();
			if (entries.containsKey(key) == false) {
				if (scopeStarted.get() == true && failedSet.contains(key)) {
					//skip
				} else {
					hashLv.add(key);
				}
			}
		}
		
		if (hashLv.size() == 0)
			return false;

		List<Pack> packList;
		try (TcpProxy tcp = TcpProxy.getTcpProxy(serverId)) {
			packList = tcp.process(cmd, param);
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
					int resultKey = (int) Hexa32.toLong32(key);
					cache(resultKey, value);
				}
			}
		}

		if (scopeStarted.get() == true) {
			for(int i=0; i<hashLv.size(); i++) {
				if (entries.containsKey(hashLv.getInt(i)) == false) {
					failedSet.add(hashLv.getInt(i));
				}
			}
		}

		return true;
	}

	public void cache(TextPack pack) {
		cache(pack.hash, pack.text);
	}

	public synchronized void cache(int hash, String value) {
		entries.put(hash, value);
		if (entries.size() > limit){
			entries.removeFirst();
		}
	}
}
