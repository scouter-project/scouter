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
package scouter.client.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TimedSeries<K, V> {
	public static class TimeValue<V> {
		long time;
		V value;

		public TimeValue(long time, V value) {
			this.time = time;
			this.value = value;
		}

	}

	private Hashtable<Object, TreeMap<Long, TimeValue>> table = new Hashtable();
	private long minTime = Long.MAX_VALUE;
	private long maxTime = Long.MIN_VALUE;

	public void add(K key, long time, V value) {
		TreeMap<Long, TimeValue> tree = table.get(key);
		if (tree == null) {
			tree = new TreeMap();
			table.put(key, tree);
		}
		tree.put(time, new TimeValue(time, value));

		if (time > maxTime)
			maxTime = time;
		if (time < minTime)
			minTime = time;
	}

	public List<V> getInTimeList(long time) {
		return getInTimeList(time, Long.MAX_VALUE - time);
	}

	public List<V> getInTimeList(long time, long valid) {
		ArrayList<V> out = new ArrayList<V>();
		Enumeration<TreeMap<Long, TimeValue>> en = table.elements();
		while (en.hasMoreElements()) {
			TreeMap<Long, TimeValue> bt = en.nextElement();
			Map.Entry<Long, TimeValue> ent = bt.ceilingEntry(time);
			if (ent != null && (time + valid >= ent.getKey().longValue())) {
				out.add((V)ent.getValue().value);
			}
		}
		return out;
	}
	
	public V get(K key, long time) {
		return getInTime(key, time, Long.MAX_VALUE - time);
	}

	public V getInTime(K key, long time, long valid) {
		V out = null;
		TreeMap<Long, TimeValue> bt = table.get(key);
		if (bt != null) {
			Map.Entry<Long, TimeValue> ent = bt.ceilingEntry(time);
			if (ent != null && (time + valid >= ent.getKey().longValue())) {
				out = (V)ent.getValue().value;
			}
		}
		return out;
	}

	public int getSeriesCount() {
		return table.size();
	}

	public long getMinTime() {
		return minTime;
	}

	public long getMaxTime() {
		return maxTime;
	}
}