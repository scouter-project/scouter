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
 */

package scouter.lang.pack;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MapPack implements Pack {

	protected Map<String, Value> table;

	public MapPack() {
		this.table = new LinkedHashMap<String, Value>();
	}

	public MapPack(Map<String, ? extends Value> table) {
		this.table = new LinkedHashMap<String, Value>(table);
	}

	public static MapPack ofStringValueMap(Map<String, String> map) {
		LinkedHashMap<String, Value> tempTable = new LinkedHashMap<String, Value>();
		for (Map.Entry<String, String> e : map.entrySet()) {
			tempTable.put(e.getKey(), new TextValue(e.getValue()));
		}
		return new MapPack(tempTable);
	}

	public int size() {
		return table.size();
	}

	public boolean isEmpty() {
		return table.isEmpty();
	}

	public boolean containsKey(String key) {
		return table.containsKey(key);
	}

	public Iterator<String> keys() {
		return table.keySet().iterator();
	}

	public Set<String> keySet() {
		return table.keySet();
	}

	public Value get(String key) {
		return (Value) table.get(key);
	}

	public boolean getBoolean(String key) {
		Value v = get(key);
		if (v instanceof BooleanValue) {
			return ((BooleanValue) v).value;
		}
		return false;
	}

	public int getInt(String key) {
		Value v = get(key);
		if (v instanceof Number) {
			return (int) ((Number) v).intValue();
		}
		return 0;
	}

	public long getLong(String key) {
		Value v = get(key);
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		return 0;
	}

	public long getLongDefault(String key, long d) {
		Value v = get(key);
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		return d;
	}

	public float getFloat(String key) {
		Value v = get(key);
		if (v instanceof Number) {
			return (float) ((Number) v).floatValue();
		}
		return 0;
	}

	public String getText(String key) {
		Value v = get(key);
		if (v instanceof TextValue) {
			return ((TextValue) v).value;
		}
		return null;
	}

	public Value put(String key, Value value) {
		return (Value) table.put(key, value);
	}

	public Value put(String key, String value) {
		return put(key, new TextValue(value));
	}

	public Value put(String key, long value) {
		return put(key, new DecimalValue(value));
	}
	
	public Value put(String key, boolean value) {
		return put(key, new BooleanValue(value));
	}

	public Value remove(String key) {
		return (Value) table.remove(key);
	}

	public void clear() {
		table.clear();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("MapPack ");
		buf.append(table);
		return buf.toString();
	}

	public byte getPackType() {
		return PackEnum.MAP;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeDecimal(table.size());
		Iterator<Map.Entry<String, Value>> en = table.entrySet().iterator();
		while (en.hasNext()) {
			Map.Entry<String, Value> e = en.next();
			dout.writeText(e.getKey());
			dout.writeValue(e.getValue());

		}
	}

	public Pack read(DataInputX din) throws IOException {
		int count = (int) din.readDecimal();
		for (int t = 0; t < count; t++) {
			String key = din.readText();
			Value value = din.readValue();
			this.put(key, value);
		}
		return this;
	}

	public ListValue newList(String name) {
		ListValue list = new ListValue();
		this.put(name, list);
		return list;
	}

	public ListValue getList(String key) {
		return (ListValue) table.get(key);
	}

	public ListValue getListNotNull(String key) {
		ListValue lv = (ListValue) table.get(key);
		return lv == null ? new ListValue() : lv;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapPack other = (MapPack) obj;
		if (size() != other.size())
			return false;
		Set<String> keySet = keySet();
		for (String key : keySet) {
			Value v1 = get(key);
			Value v2 = other.get(key);
			if (v2 == null) {
				return false;
			}
			if (v1.toJavaObject().equals(v2.toJavaObject()) == false) {
				return false;
			}
		}
		return true;
	}

	public Object toJavaObject() {
		return this.table;
	}

	public Map<String, Value> toMap() {
		return this.table;
	}

	public MapValue toMapValue() {
		MapValue map = new MapValue();
		map.putAll(this.table);
		return map;
	}

	public MapPack setMapValue(MapValue mapValue) {
		if(mapValue==null)
			return this;
		Enumeration<String> keys = mapValue.keys();	
		while(keys.hasMoreElements()){
			String key = keys.nextElement();
			Value value=mapValue.get(key);
			this.table.put(key, value);
		}
		return this;
	}
}
