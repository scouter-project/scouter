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
package scouter.util;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * @author Gun Lee (gunlee01@gmail.com)
 *
 */
public class ByteArrayKeyMap<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private ByteArrayKeyEntry<V> table[];
	private int count;
	private int threshold;
	private float loadFactor;

	public ByteArrayKeyMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new ByteArrayKeyEntry[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}

	public ByteArrayKeyMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized ByteArrayEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<ByteArrayKeyEntry<V>> entries() {
		return new Enumer(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(V value) {
		if (value == null) {
			return false;
		}
		ByteArrayKeyEntry<V> tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (ByteArrayKeyEntry<V> e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(byte[] key) {
		ByteArrayKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyEntry<V> e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}

	private int hash(byte[] h) {
		return Arrays.hashCode(h) & Integer.MAX_VALUE;
	}

	public synchronized V get(byte[] key) {
		ByteArrayKeyEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyEntry<V> e = tab[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.value;
			}
		}
		return null;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		ByteArrayKeyEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		ByteArrayKeyEntry newMap[] = new ByteArrayKeyEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (ByteArrayKeyEntry<V> old = oldMap[i]; old != null;) {
				ByteArrayKeyEntry<V> e = old;
				old = old.next;
				int index = hash(e.key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	public synchronized byte[][] keyArray() {
		byte[][] _keys = new byte[this.size()][];
		ByteArrayEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextKey();
		return _keys;
	}

	public synchronized V put(byte[] key, V value) {
		ByteArrayKeyEntry<V> tab[] = table;
		int _hash = hash(key);
		int index = _hash % tab.length;
		for (ByteArrayKeyEntry<V> e = tab[index]; e != null; e = e.next) {
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
		ByteArrayKeyEntry<V> e = new ByteArrayKeyEntry<V>(key, value, tab[index]);
		tab[index] = e;
		count++;
		return null;
	}

	public synchronized V remove(byte[] key) {
		ByteArrayKeyEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyEntry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
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
		ByteArrayKeyEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			ByteArrayKeyEntry e = (ByteArrayKeyEntry) (it.nextElement());
			if (i > 0)
				buf.append(", ");
			buf.append(Arrays.toString(e.getKey()) + "=" + e.getValue());
		}
		buf.append("}");
		return buf.toString();
	}

	public String toFormatString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{\n");
		while (it.hasMoreElements()) {
			ByteArrayKeyEntry e = (ByteArrayKeyEntry) it.nextElement();
			buf.append("\t").append(Arrays.toString(e.getKey()) + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer implements Enumeration, ByteArrayEnumer {
		ByteArrayKeyEntry[] table = ByteArrayKeyMap.this.table;
		int index = table.length;
		ByteArrayKeyEntry entry = null;
		ByteArrayKeyEntry lastReturned = null;
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
				ByteArrayKeyEntry<V> e = lastReturned = entry;
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

		public byte[] nextKey() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				ByteArrayKeyEntry<V> e = lastReturned = entry;
				entry = e.next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	public void putAll(ByteArrayKeyMap<V> other) {
		Enumeration it = other.entries();
		for (int i = 0, max = other.size(); i <= max; i++) {
			ByteArrayKeyEntry<V> e = (ByteArrayKeyEntry<V>) (it.nextElement());
			this.put(e.getKey(), e.getValue());
		}
	}

	public static class ByteArrayKeyEntry<V> {
		byte[] key;
		V value;
		ByteArrayKeyEntry<V> next;

		protected ByteArrayKeyEntry(byte[] key, V value, ByteArrayKeyEntry<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new ByteArrayKeyEntry<V>(key, value, (next == null ? null : (ByteArrayKeyEntry<V>) next.clone()));
		}

		public byte[] getKey() {
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
			if (!(o instanceof ByteArrayKeyMap.ByteArrayKeyEntry))
				return false;
			ByteArrayKeyEntry e = (ByteArrayKeyEntry) o;
			return (key == e.getKey()) && (value == null ? e.getValue() == null : value.equals(e.getValue()));
		}

		public int hashCode() {
			return Arrays.hashCode(key) ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return Arrays.toString(key) + "=" + value.toString();
		}
	}
}
