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
 *  The initial idea for this class is from "org.apache.commons.lang.IntHashMap"; 
 *  http://commons.apache.org/commons-lang-2.6-src.zip
 *
 *  https://github.com/itext/itextpdf/archive/iText_5_5_1.zip
 *                           com.itextpdf.text.pdf.IntHashtable.java                         
 */
package scouter.util;
import java.util.Enumeration;
import java.util.NoSuchElementException;
/**
 * @author Paul Kim (sjkim@whatap.io)
 */
public class IntFloatMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private IntFloatEntry table[];
	private int count;
	private int threshold;
	private float loadFactor;
	private int NONE = 0;
	public IntFloatMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}
	public IntFloatMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new IntFloatEntry[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}
	public IntFloatMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	public int size() {
		return count;
	}
	public synchronized IntEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}
	public synchronized FloatEnumer values() {
		return new Enumer(TYPE.VALUES);
	}
	public synchronized Enumeration<IntFloatEntry> entries() {
		return new Enumer(TYPE.ENTRIES);
	}
	public synchronized boolean containsValue(int value) {
		IntFloatEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (IntFloatEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}
	public synchronized boolean containsKey(int key) {
		IntFloatEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntFloatEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}
	private int hash(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		h = h ^ (h >>> 7) ^ (h >>> 4);
		return h & Integer.MAX_VALUE;
	}
	public synchronized float get(int key) {
		IntFloatEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntFloatEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.value;
			}
		}
		return NONE;
	}
	protected void rehash() {
		int oldCapacity = table.length;
		IntFloatEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntFloatEntry newMap[] = new IntFloatEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (IntFloatEntry old = oldMap[i]; old != null;) {
				IntFloatEntry e = old;
				old = old.next;
				int index = hash(e.key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}
	public synchronized int[] keyArray() {
		int[] _keys = new int[this.size()];
		IntEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextInt();
		return _keys;
	}
	public synchronized float[] valueArray() {
		float[] _values = new float[this.size()];
		FloatEnumer en = this.values();
		for (int i = 0; i < _values.length; i++)
			_values[i] = en.nextFloat();
		return _values;
	}
	public synchronized float put(int key, float value) {
		IntFloatEntry tab[] = table;
		int _hash = hash(key);
		int index = _hash % tab.length;
		for (IntFloatEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				float old = e.value;
				e.value = value;
				return old;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = _hash % tab.length;
		}
		IntFloatEntry e = new IntFloatEntry(key, value, tab[index]);
		tab[index] = e;
		count++;
		return NONE;
	}
	public synchronized float remove(int key) {
		IntFloatEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntFloatEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.key == key) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				float oldValue = e.value;
				e.value = NONE;
				return oldValue;
			}
		}
		return NONE;
	}
	public synchronized void clear() {
		IntFloatEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}
	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			IntFloatEntry e = (IntFloatEntry) (it.nextElement());
			if (i > 0)
				buf.append(", ");
			buf.append(e.getKey() + "=" + e.getValue());
		}
		buf.append("}");
		return buf.toString();
	}
	public String toFormatString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{\n");
		while (it.hasMoreElements()) {
			IntFloatEntry e = (IntFloatEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}
	public static class IntFloatEntry {
		int key;
		float value;
		IntFloatEntry next;
		protected IntFloatEntry(int key, float value, IntFloatEntry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
		protected Object clone() {
			return new IntFloatEntry(key, value, (next == null ? null : (IntFloatEntry) next.clone()));
		}
		public int getKey() {
			return key;
		}
		public float getValue() {
			return value;
		}
		public float setValue(float value) {
			float oldValue = this.value;
			this.value = value;
			return oldValue;
		}
		public boolean equals(Object o) {
			if (!(o instanceof IntFloatEntry))
				return false;
			IntFloatEntry e = (IntFloatEntry) o;
			return (key == e.getKey()) && (value == e.getValue());
		}
		public int hashCode() {
			return key ^ (int)value;
		}
		public String toString() {
			return key + "=" + value;
		}
	}
	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}
	private class Enumer implements Enumeration, IntEnumer, FloatEnumer {
		IntFloatEntry[] table = IntFloatMap.this.table;
		int index = table.length;
		IntFloatEntry entry = null;
		IntFloatEntry lastReturned = null;
		TYPE type;
		Enumer(TYPE type) {
			this.type = type;
		}
		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];
			return entry != null;
		}
		public Object nextElement() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				IntFloatEntry e = lastReturned = entry;
				entry = e.next;
				switch (type) {
				case KEYS:
					return e.key;
				case VALUES:
					return e.value;
				default:
					return e;
				}
			}
			throw new NoSuchElementException("no more next");
		}
		public int nextInt() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				IntFloatEntry e = lastReturned = entry;
				entry = e.next;
				switch (type) {
				case KEYS:
					return e.key;
				default:
					return NONE;
				}
			}
			throw new NoSuchElementException("no more next");
		}
		public float nextFloat() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				IntFloatEntry e = lastReturned = entry;
				entry = e.next;
				switch (type) {
				case VALUES:
					return e.value;
				default:
					return NONE;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}
	public void putAll(IntFloatMap other) {
		Enumeration it = other.entries();
		for (int i = 0, max = other.size(); i <= max; i++) {
			IntFloatEntry e = (IntFloatEntry) (it.nextElement());
			this.put(e.getKey(), e.getValue());
		}
	}
}
