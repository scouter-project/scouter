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

package scouter.lang.value;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ListValue implements Value {

	private List<Value> valueList;

	public ListValue() {
		valueList = new ArrayList<Value>();
	}

	public ListValue(int size) {
		valueList = new ArrayList<Value>(size);
	}

	public ListValue(List<Value> value) {
		this.valueList = value;
	}

	public static ListValue ofStringValueList(List<String> orgList) {
		ListValue lv = new ListValue();
		for (String value : orgList) {
			lv.add(value);
		}
		return lv;
	}

	public Value get(int i) {
		return valueList.get(i);
	}

	public boolean getBoolean(int i) {
		Value v = valueList.get(i);
		if (v instanceof BooleanValue) {
			return ((BooleanValue) v).value;
		}
		return false;
	}

	public double getDouble(int i) {
		Value v = valueList.get(i);
		if (v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		return 0;
	}

	public float getFloat(int i) {
		Value v = valueList.get(i);
		if (v instanceof Number) {
			return ((Number) v).floatValue();
		}
		return 0;
	}

	public long getLong(int i) {
		Value v = valueList.get(i);
		if (v instanceof Number) {
			return ((Number) v).longValue();
		}
		return 0;
	}
	public int getInt(int i) {
		Value v = valueList.get(i);
		if (v instanceof Number) {
			return ((Number) v).intValue();
		}
		return 0;
	}
	public String getString(int i) {
		Value v = valueList.get(i);
		if (v instanceof TextValue) {
			return ((TextValue) v).value;
		}
		if (v == null)
			return null;
		return v.toString();
	}

	public void set(int i, Value value) {
		valueList.set(i, value);
	}

	public ListValue add(Value value) {
		valueList.add(value);
		return this;
	}

	public ListValue add(Value[] value) {
		for (int i = 0; i < value.length; i++) {
			valueList.add(value[i]);
		}
		return this;
	}

	public ListValue add(boolean value) {
		valueList.add(new BooleanValue(value));
		return this;
	}

	public void add(double value) {
		valueList.add(new DoubleValue(value));
	}

	public void add(long value) {
		valueList.add(new DecimalValue(value));
	}

	public void add(float value) {
		valueList.add(new FloatValue(value));
	}

	public void add(String value) {
		valueList.add(new TextValue(value));
	}

	public byte getValueType() {
		return ValueEnum.LIST;
	}

	public void write(DataOutputX out) throws IOException {
		int sz = size();
		out.writeDecimal(sz);
		for (int i = 0; i < sz; i++) {
			out.writeValue(valueList.get(i));
		}
	}

	public int size() {
		return valueList.size();
	}

	public Value read(DataInputX in) throws IOException {
		int count = (int) in.readDecimal();
		for (int i = 0; i < count; i++) {
			this.valueList.add(in.readValue());
		}
		return this;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0, sz = size(); i < sz; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(valueList.get(i));
		}
		sb.append("]");
		return sb.toString();
	}

	public ListValue addNull() {
		this.add(new NullValue());
		return this;
	}

	public Object toJavaObject() {
		return this.valueList;
	}

	public Object[] toObjectArray() {
		int sz = this.valueList.size();
		Object[] o = new Object[sz];
		for (int i = 0; i < sz; i++) {
			o[i] = this.valueList.get(i).toJavaObject();
		}
		return o;
	}

	public ListValue add(Set<Value> keySet) {
		for (Value val : keySet) {
			this.add(val);
		}
		return this;
	}

	public ListValue add(String[] str) {
		if (str == null)
			return this;
		for (String s : str) {
			this.add(s);
		}
		return this;
	}

	public ListValue add(boolean[] booleans) {
		if (booleans == null)
			return this;
		for (boolean s : booleans) {
			this.add(s);
		}
		return this;
	}

	public ListValue add(int[] ints) {
		if (ints == null)
			return this;
		for (int s : ints) {
			this.add(s);
		}
		return this;
	}
	
	public Iterator<Value> iterator() {
		return this.valueList.iterator();
	}

	public String[] toStringArray() {
		String[] out = new String[this.size()];
		for (int i = 0; i < this.size(); i++) {
			out[i] = getString(i);
		}
		return out;
	}

	private int hash;
	private int hash_sz;

	public int hashCode() {
		if (this.size() == hash_sz && hash != 0)
			return hash;
		this.hash_sz = this.size();
		for (int i = 0; i < this.valueList.size(); i++) {
			this.hash ^= this.get(i).hashCode();
		}
		return this.hash;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ListValue other = (ListValue) obj;
		if (this.size() != other.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			if (this.get(i).equals(other.get(i)) == false)
				return false;
		}
		return true;
	}

}
