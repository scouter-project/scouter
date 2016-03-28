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
public class LongKeyLinkedMap<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongKeyLinkedEntry<V> table[];
	private LongKeyLinkedEntry<V> header;

	private int count;
	private int threshold;
	private float loadFactor;

	@SuppressWarnings("unchecked")
	public LongKeyLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LongKeyLinkedEntry[initCapacity];

		this.header = new LongKeyLinkedEntry<V>(0, null, null);
		this.header.link_next = header.link_prev = header;

		threshold = (int) (initCapacity * loadFactor);
	}

	public LongKeyLinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public long[] keyArray() {
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
		return new Enumer<V>(TYPE.VALUES);
	}

	public synchronized Enumeration<LongKeyLinkedEntry<V>> entries() {
		return new Enumer<LongKeyLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(Object value) {
		if (value == null || size() == 0) {
			return false;
		}
		LongKeyLinkedEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (LongKeyLinkedEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(long key) {
		if (this.size() == 0)
			return false;

		LongKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(long key) {
		if (this.size() == 0)
			return null;
		LongKeyLinkedEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return null;
	}

	public V intern(long key) {
		return _intern(key, MODE.LAST);
	}

	private synchronized V _intern(long key, MODE m) {
		LongKeyLinkedEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}

		V value = create(key);
		if (value == null)
			return null;

		if (max > 0) {
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					//removeLast();
					long k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					long k = header.link_next.key;
					V v = remove(k);
					overflowed(k, v);					
				}
				break;
			}
		}

		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}
		LongKeyLinkedEntry e = new LongKeyLinkedEntry(key, value, tab[index]);
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
		return value;
	}

	protected void overflowed(long key, V value){
	}
	protected V create(long key) {
		throw new RuntimeException("not implemented create()");
	}

	public synchronized long getFirstKey() {
		if (this.size() == 0)
			return 0;
		return this.header.link_next.key;
	}

	public synchronized long getLastKey() {
		if (this.size() == 0)
			return 0;
		return this.header.link_prev.key;
	}

	public synchronized V getFirstValue() {
		if (this.size() == 0)
			return null;
		return this.header.link_next.value;
	}

	public synchronized V getLastValue() {
		if (this.size() == 0)
			return null;
		return this.header.link_prev.value;
	}

	private int hash(long key) {
		return (int) (key ^ (key >>> 32)) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LongKeyLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongKeyLinkedEntry newMap[] = new LongKeyLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongKeyLinkedEntry old = oldMap[i];
			while (old != null) {
				LongKeyLinkedEntry e = old;
				old = old.next;
				long key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LongKeyLinkedMap<V> setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public V put(long key, V value) {
		return _put(key, value, MODE.LAST);
	}

	public V putLast(long key, V value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public V putFirst(long key, V value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized V _put(long key, V value, MODE m) {
		LongKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				V old = e.value;
				e.value = value;
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
					//removeLast();
					long k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);					
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					long k = header.link_next.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			}
		}

		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}
		LongKeyLinkedEntry e = new LongKeyLinkedEntry(key, value, tab[index]);
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

	public synchronized V remove(long key) {
		LongKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongKeyLinkedEntry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
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
		LongKeyLinkedEntry tab[] = table;
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
			LongKeyLinkedEntry e = (LongKeyLinkedEntry) (it.nextElement());
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
			LongKeyLinkedEntry e = (LongKeyLinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration, LongEnumer {
		TYPE type;
		LongKeyLinkedEntry entry = LongKeyLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry;
		}

		public V nextElement() {
			if (entry != null) {
				LongKeyLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return (V) new Long(e.key);
				case VALUES:
					return (V) e.value;
				default:
					return (V) e;
				}
			}
			throw new NoSuchElementException("Enumerator");
		}

		public long nextLong() {
			if (entry != null) {
				LongKeyLinkedEntry e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("Enumer");
		}
	}

	private void chain(LongKeyLinkedEntry link_prev, LongKeyLinkedEntry link_next, LongKeyLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LongKeyLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class LongKeyLinkedEntry<V> {
		long key;
		V value;
		LongKeyLinkedEntry<V> next;
		LongKeyLinkedEntry<V> link_next, link_prev;

		protected LongKeyLinkedEntry(long key, V value, LongKeyLinkedEntry<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new LongKeyLinkedEntry(key, value, (next == null ? null : (LongKeyLinkedEntry) next.clone()));
		}

		public long getKey() {
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
			if (!(o instanceof LongKeyLinkedEntry))
				return false;
			LongKeyLinkedEntry e = (LongKeyLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return (int) (key ^ (key >>> 32)) ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	public static void main(String[] args) {
		LongKeyLinkedMap m = new LongKeyLinkedMap().setMax(2);
		m.putLast(10, 10);
		m.putLast(20, 20);
		m.putFirst(30, 30);

		// m.removeFirst();
		System.out.println(m);
		m = new LongKeyLinkedMap().setMax(2);
		m.putFirst(10, 10);
		m.putFirst(20, 20);
		m.putFirst(30, 30);

		// m.removeLast();
		System.out.println(m);
	}

	private static void print(Object e) {
		System.out.println(e);
	}

}
