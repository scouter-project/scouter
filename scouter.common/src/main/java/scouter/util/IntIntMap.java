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
public class IntIntMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private IntIntEntry table[];
	private int count;
	private int threshold;
	private float loadFactor;
	private int NONE = 0;

	public IntIntMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public IntIntMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new IntIntEntry[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}

	public IntIntMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized IntEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized IntEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<IntIntEntry> entries() {
		return new Enumer(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(int value) {
		IntIntEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (IntIntEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(int key) {
		IntIntEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntEntry e = tab[index]; e != null; e = e.next) {
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

	public synchronized int get(int key) {
		IntIntEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.value;
			}
		}
		return NONE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		IntIntEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntIntEntry newMap[] = new IntIntEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (IntIntEntry old = oldMap[i]; old != null;) {
				IntIntEntry e = old;
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

	public synchronized int[] valueArray() {
		int[] _values = new int[this.size()];
		IntEnumer en = this.values();
		for (int i = 0; i < _values.length; i++)
			_values[i] = en.nextInt();
		return _values;
	}

	public synchronized int put(int key, int value) {
		IntIntEntry tab[] = table;
		int _hash = hash(key);
		int index = _hash % tab.length;
		for (IntIntEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				int old = e.value;
				e.value = value;
				return old;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = _hash % tab.length;
		}
		IntIntEntry e = new IntIntEntry(key, value, tab[index]);
		tab[index] = e;
		count++;
		return NONE;
	}

	public synchronized int add(int key, int value) {
		IntIntEntry tab[] = table;
		int _hash = hash(key);
		int index = _hash % tab.length;
		for (IntIntEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				int old = e.value;
				e.value += value;
				return e.value;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = _hash % tab.length;
		}
		IntIntEntry e = new IntIntEntry(key, value, tab[index]);
		tab[index] = e;
		count++;
		return value;
	}

	public synchronized int remove(int key) {
		IntIntEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.key == key) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				int oldValue = e.value;
				e.value = NONE;
				return oldValue;
			}
		}
		return NONE;
	}

	public synchronized void clear() {
		IntIntEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			IntIntEntry e = (IntIntEntry) (it.nextElement());
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
			IntIntEntry e = (IntIntEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer implements Enumeration, IntEnumer {
		IntIntEntry[] table = IntIntMap.this.table;
		int index = table.length;
		IntIntEntry entry = null;
		IntIntEntry lastReturned = null;
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
				IntIntEntry e = lastReturned = entry;
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
				IntIntEntry e = lastReturned = entry;
				entry = e.next;
				switch (type) {
				case KEYS:
					return e.key;
				case VALUES:
					return e.value;
				default:
					return NONE;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}

	public void putAll(IntIntMap other) {
		Enumeration<IntIntEntry> it = other.entries();
		for (int i = 0, max = other.size(); i <= max; i++) {
			IntIntEntry e = it.nextElement();
			this.put(e.getKey(), e.getValue());
		}
	}

	public static class IntIntEntry {
		int key;
		int value;
		IntIntEntry next;

		protected IntIntEntry(int key, int value, IntIntEntry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new IntIntEntry(key, value, (next == null ? null : (IntIntEntry) next.clone()));
		}

		public int getKey() {
			return key;
		}

		public int getValue() {
			return value;
		}

		public int setValue(int value) {
			int oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof IntIntEntry))
				return false;
			IntIntEntry e = (IntIntEntry) o;
			return (key == e.getKey()) && (value == e.getValue());
		}

		public int hashCode() {
			return key ^ value;
		}

		public String toString() {
			return key + "=" + value;
		}
	}
}
