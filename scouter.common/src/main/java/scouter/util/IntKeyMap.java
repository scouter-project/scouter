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
public class IntKeyMap<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private IntKeyEntry<V> table[];
	private int count;
	private int threshold;
	private float loadFactor;

	public IntKeyMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new IntKeyEntry[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}

	public IntKeyMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized IntEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<IntKeyEntry<V>> entries() {
		return new Enumer(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(V value) {
		if (value == null) {
			return false;
		}
		IntKeyEntry<V> tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (IntKeyEntry<V> e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(int key) {
		IntKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntKeyEntry<V> e = tab[index]; e != null; e = e.next) {
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

	public synchronized V get(int key) {
		IntKeyEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (IntKeyEntry<V> e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.value;
			}
		}
		return null;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		IntKeyEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntKeyEntry newMap[] = new IntKeyEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (IntKeyEntry<V> old = oldMap[i]; old != null;) {
				IntKeyEntry<V> e = old;
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

	public synchronized V put(int key, V value) {
		IntKeyEntry<V> tab[] = table;
		int _hash = hash(key);
		int index = _hash % tab.length;
		for (IntKeyEntry<V> e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				V old = e.value;
				e.value = value;
				return old;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = _hash % tab.length;
		}
		IntKeyEntry<V> e = new IntKeyEntry<V>(key, value, tab[index]);
		tab[index] = e;
		count++;
		return null;
	}

	public synchronized V remove(int key) {
		IntKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntKeyEntry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.key == key) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				V oldValue = e.value;
				e.value = null;
				return oldValue;
			}
		}
		return null;
	}

	public synchronized void clear() {
		IntKeyEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			IntKeyEntry e = (IntKeyEntry) (it.nextElement());
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
			IntKeyEntry e = (IntKeyEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer implements Enumeration, IntEnumer {
		IntKeyEntry[] table = IntKeyMap.this.table;
		int index = table.length;
		IntKeyEntry entry = null;
		IntKeyEntry lastReturned = null;
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
				IntKeyEntry<V> e = lastReturned = entry;
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
				IntKeyEntry<V> e = lastReturned = entry;
				entry = e.next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	public void putAll(IntKeyMap<V> other) {
		Enumeration it = other.entries();
		for (int i = 0, max = other.size(); i <= max; i++) {
			IntKeyEntry<V> e = (IntKeyEntry<V>) (it.nextElement());
			this.put(e.getKey(), e.getValue());
		}
	}

	public static class IntKeyEntry<V> {
		int key;
		V value;
		IntKeyEntry<V> next;

		protected IntKeyEntry(int key, V value, IntKeyEntry<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new IntKeyEntry<V>(key, value, (next == null ? null : (IntKeyEntry<V>) next.clone()));
		}

		public int getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			if (value == null)
				throw new NullPointerException();
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof IntKeyEntry))
				return false;
			IntKeyEntry e = (IntKeyEntry) o;
			return (key == e.getKey()) && (value == null ? e.getValue() == null : value.equals(e.getValue()));
		}

		public int hashCode() {
			return key ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value.toString();
		}
	}
}
