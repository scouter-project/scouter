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
 * 
 *  The initial idea for this class is from "org.apache.commons.lang.IntHashMap"; 
 *  http://commons.apache.org/commons-lang-2.6-src.zip
 *
 */
package scouter.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * HashTable with entry expiration.
 *  - it use only active expiration : delete entry when access expired data
 * @param <K> Key type
 * @param <V> Value type
 */
public class CacheTable<K, V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private ENTRY<K, V> table[];
	private ENTRY<K, V> header;

	private int count;
	private int threshold;
	private float loadFactor;

	private long defaultKeepTime = 0;

	public CacheTable() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public CacheTable(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);

		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new ENTRY[initCapacity];

		this.header = new ENTRY(null, null, 0, null);
		this.header.link_next = header.link_prev = header;

		threshold = (int) (initCapacity * loadFactor);
	}

	public int size() {
		return count;
	}

	public synchronized Enumeration<K> keys() {
		return new Enumer<K>(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer<V>(TYPE.VALUES);
	}

	public synchronized Enumeration<ENTRY<K, V>> entries() {
		return new Enumer<ENTRY>(TYPE.ENTRIES);
	}

	public synchronized boolean containsKey(K key) {
		return getEntry(key) != null;
	}

	public synchronized V get(K key) {
		ENTRY<K, V> e = getEntry(key);
		return e == null ? null : e.getValue();
	}

	public synchronized V getKeepAlive(K key, long keepAlive) {
		ENTRY<K, V> e = getEntry(key);
		if (e == null)
			return null;
		e.keepAlive(keepAlive);
		return e.getValue();
	}

	public synchronized V getKeepAlive(K key) {
		ENTRY<K, V> e = getEntry(key);
		if (e == null)
			return null;
		e.keepAlive(defaultKeepTime);
		return e.getValue();
	}

	private ENTRY<K, V> getEntry(K key) {
		if (key == null)
			return null;
		ENTRY<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<K, V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (e.isExpired()) {
					remove(e.getKey());
					return null;
				} else {
					return e;
				}
			}
		}
		return null;
	}

	private V getValue(ENTRY<K, V> e) {
		if (e == null)
			return null;
		if (e.isExpired()) {
			remove(e.key);
			return null;
		}
		return e.value;
	}

	public synchronized V getFirstValue() {
		if (isEmpty())
			return null;
		return getValue(this.header.link_next);
	}

	public synchronized V getLastValue() {
		if (isEmpty())
			return null;
		return getValue(this.header.link_prev);
	}

	public synchronized int getRemindTime(K key) {
		ENTRY entry = getEntry(key);
		if (entry != null) {
			if (entry.timeOfExpiration == 0)
				return Integer.MAX_VALUE;
			return (int) (entry.timeOfExpiration - System.currentTimeMillis());
		} else {
			return 0;
		}

	}

	public void clearExpiredItems() {

		try {
			ArrayList<K> delete = new ArrayList<K>();
			Enumeration<CacheTable.ENTRY<K, V>> en = this.entries();
			while (en.hasMoreElements()) {
				ENTRY e = en.nextElement();
				if (e.isExpired()) {
					delete.add((K) e.getKey());
				}
			}
			for (int i = 0; i < delete.size(); i++) {
				remove(delete.get(i));
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private int hash(Object key) {
		return (int) (key.hashCode()) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		ENTRY oldMap[] = table;

		int newCapacity = oldCapacity * 2 + 1;
		ENTRY newMap[] = new ENTRY[newCapacity];

		threshold = (int) (newCapacity * loadFactor);
		table = newMap;

		for (int i = oldCapacity; i-- > 0;) {
			for (ENTRY<K, V> old = oldMap[i]; old != null;) {
				ENTRY<K, V> e = old;
				old = old.next;

				K key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public CacheTable<K, V> setMaxRow(int max) {
		this.max = max;
		return this;
	}

	public CacheTable<K, V> setDefaultKeepTime(long time) {
		this.defaultKeepTime = time;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public V put(K key, V value, long keepTime) {
		return _put(key, value, keepTime, MODE.LAST);
	}

	public V putLast(K key, V value, long keepTime) {
		return _put(key, value, keepTime, MODE.FORCE_LAST);
	}

	public V putFirst(K key, V value, long keepTime) {
		return _put(key, value, keepTime, MODE.FORCE_FIRST);
	}

	public V put(K key, V value) {
		return _put(key, value, defaultKeepTime, MODE.LAST);
	}

	public V putLast(K key, V value) {
		return _put(key, value, defaultKeepTime, MODE.FORCE_LAST);
	}

	public V putFirst(K key, V value) {
		return _put(key, value, defaultKeepTime, MODE.FORCE_FIRST);
	}

	private synchronized V _put(K key, V value, long keepTime, MODE m) {
		ENTRY<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<K, V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				V old = e.value;
				e.value = value;
				e.keepAlive(keepTime);

				switch (m) {
				case FORCE_FIRST:
					if (header.link_next != e) {
						unchain(e);
						chain(header, header.link_next, e);
					}
					break;
				case FORCE_LAST:
					if (header.link_prev != e) {
						unchain(e);
						chain(header.link_prev, header, e);
					}
					break;
				}
				return old;
			}
		}

		if (max > 0) {
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					removeLast();
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					removeFirst();
				}
				break;
			}
		}

		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}

		ENTRY e = new ENTRY(key, value, keepTime, tab[index]);
		tab[index] = e;

		switch (m) {
		case FORCE_FIRST:
		case FIRST:
			chain(header, header.link_next, e);
			break;
		case FORCE_LAST:
		case LAST:
			chain(header.link_prev, header, e);
			break;
		}

		count++;
		return null;
	}

	public synchronized V remove(Object key) {
		if (key == null)
			return null;
		ENTRY<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				V oldValue = e.value;
				e.value = null;
				//
				unchain(e);

				return oldValue;
			}
		}
		return null;
	}

	public synchronized V removeFirst() {
		if (isEmpty())
			return null;
		return remove(header.link_next.key);
	}

	public synchronized V removeLast() {
		if (isEmpty())
			return null;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		ENTRY tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;

		this.header.link_next = header.link_prev = header;

		count = 0;
	}

	public String toString() {

		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();

		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			ENTRY e = (ENTRY) (it.nextElement());
			if (i > 0)
				buf.append(", ");
			buf.append(e.getKey() + "=" + e.getValue());

		}
		buf.append("}");
		return buf.toString();
	}

	public String toKeyString() {

		StringBuffer buf = new StringBuffer();
		Enumeration<K> it = keys();

		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			K key = it.nextElement();
			if (i > 0)
				buf.append(", ");
			buf.append(key);

		}
		buf.append("}");
		return buf.toString();
	}

	public String toFormatString() {

		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();

		buf.append("{\n");
		while (it.hasMoreElements()) {
			ENTRY e = (ENTRY) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	public static class ENTRY<K, V> {
		private K key;
		private V value;
		private long timeOfExpiration;
		private long keepTime = 0;

		ENTRY<K, V> next;
		ENTRY<K, V> link_next, link_prev;

		protected ENTRY(K key, V value, long keepTime, ENTRY next) {
			this.key = key;
			this.value = value;
			this.keepAlive(keepTime);
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(key, value, keepTime, (next == null ? null : (ENTRY) next.clone()));
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean isExpired() {
			if (timeOfExpiration > 0) {
				if (timeOfExpiration < System.currentTimeMillis())
					return true;
				else
					return false;
			} else {
				return false;
			}
		}

		public void keepAlive(long keepTime) {
			if (keepTime > 0) {
				this.keepTime = keepTime;
				this.timeOfExpiration = System.currentTimeMillis() + keepTime;
			} else {
				this.keepTime = 0;
				this.timeOfExpiration = 0;
			}
		}

		public void keep() {
			if (this.keepTime > 0) {
				this.timeOfExpiration = System.currentTimeMillis() + this.keepTime;
			}
		}

		public boolean equals(Object o) {
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return CompareUtil.equals(key, e.key) && CompareUtil.equals(value, e.value);
		}

		public int hashCode() {
			return key.hashCode() ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration {

		TYPE type;
		ENTRY entry = CacheTable.this.header.link_next;
		ENTRY lastEnt;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public V nextElement() {
			if (hasMoreElements()) {
				ENTRY e = lastEnt = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return (V) e.key;
				case VALUES:
					return (V) e.value;
				default:
					return (V) e;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(ENTRY link_prev, ENTRY link_next, ENTRY e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(ENTRY e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static void main(String[] args) throws Exception {
		CacheTable t = new CacheTable().setDefaultKeepTime(1000).setMaxRow(100);
		for (int i = 0; i < 100; i++) {
			// if (i % 5 == 0) {
			// t.put(i, i, 10000);
			// } else {
			t.put(i, i);
			// }

		}
		Enumeration e = t.keys();

		System.out.println(t.get(0));
		System.out.println(t.get(10));
		System.out.println(t.get(99));
		Thread.sleep(2000);
		// t.clearExpiredItems();
		System.out.println("----->" + t.size());
		System.out.println(t.getFirstValue());
	}

}