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
 */
package scouter.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
/**
 * @author Paul Kim (sjkim@whatap.io)
 */
public class LongKeyMap<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongKeyEntry<V> table[];
	private int count;
	private int threshold;
	private float loadFactor;

	public LongKeyMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new LongKeyEntry[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}

	public LongKeyMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized long[] keyArray() {
		long[] _keys = new long[this.size()];
		LongEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextLong();
		return _keys;
	}

	public synchronized LongEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<LongKeyEntry<V>> entries() {
		return new Enumer(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(V value) {
		if (value == null) {
			return false;
		}
		LongKeyEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (LongKeyEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(long key) {
		LongKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(long key) {
		LongKeyEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyEntry e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return (V) e.value;
			}
		}
		return null;
	}

	private int hash(long key) {
		return (int) (key ^ (key >>> 32)) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LongKeyEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongKeyEntry newMap[] = new LongKeyEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongKeyEntry old = oldMap[i];
			while (old != null) {
				LongKeyEntry e = old;
				old = old.next;
				long key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	public synchronized V put(long key, V value) {
		LongKeyEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyEntry<V> e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				V old = e.value;
				e.value = value;
				return old;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}
		LongKeyEntry e = new LongKeyEntry(key, value, tab[index]);
		tab[index] = e;
		count++;
		return null;
	}

	public synchronized Object remove(long key) {
		LongKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.key == key) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				Object oldValue = e.value;
				e.value = null;
				return oldValue;
			}
		}
		return null;
	}

	public synchronized void clear() {
		LongKeyEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			LongKeyEntry e = (LongKeyEntry) (it.nextElement());
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
			LongKeyEntry e = (LongKeyEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer implements Enumeration, LongEnumer {
		LongKeyEntry[] table = LongKeyMap.this.table;
		int index = table.length;
		LongKeyEntry entry = null;
		LongKeyEntry lastReturned = null;
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
				LongKeyEntry e = lastReturned = entry;
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

		public long nextLong() {
			while (entry == null && index > 0)
				entry = table[--index];

			if (entry != null) {
				LongKeyEntry e = lastReturned = entry;
				entry = e.next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	public static class LongKeyEntry<V> {
		long key;
		V value;
		LongKeyEntry<V> next;

		protected LongKeyEntry(long key, V value, LongKeyEntry<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new LongKeyEntry<V>(key, value, (next == null ? null : (LongKeyEntry) next.clone()));
		}

		public long getKey() {
			return key;
		}

		public Object getValue() {
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
			if (!(o instanceof LongKeyEntry))
				return false;
			LongKeyEntry<V> e = (LongKeyEntry<V>) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return (int) (key ^ (key >>> 32)) ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value.toString();
		}
	}
}
