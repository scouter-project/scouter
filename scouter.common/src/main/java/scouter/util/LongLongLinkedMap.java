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
public class LongLongLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongLongLinkedEntry table[];
	private LongLongLinkedEntry header;

	private int count;
	private int threshold;
	private float loadFactor;
	private long NONE = 0;

	public LongLongLinkedMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public LongLongLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LongLongLinkedEntry[initCapacity];

		this.header = new LongLongLinkedEntry(0, 0, null);
		this.header.link_next = header.link_prev = header;

		threshold = (int) (initCapacity * loadFactor);
	}

	public LongLongLinkedMap() {
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

	public synchronized LongEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<LongLongLinkedEntry> entries() {
		return new Enumer<LongLongLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(long value) {
		LongLongLinkedEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (LongLongLinkedEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(long key) {
		LongLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized long get(long key) {
		LongLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return NONE;
	}

	public synchronized long getFirstKey() {
		return this.header.link_next.key;
	}

	public synchronized long getLastKey() {
		return this.header.link_prev.key;
	}

	public synchronized long getFirstValue() {
		return this.header.link_next.value;
	}

	public synchronized long getLastValue() {
		return this.header.link_prev.value;
	}

	private int hash(long key) {
		return (int) (key ^ (key >>> 32)) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LongLongLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongLongLinkedEntry newMap[] = new LongLongLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongLongLinkedEntry old = oldMap[i];
			while (old != null) {
				LongLongLinkedEntry e = old;
				old = old.next;
				long key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LongLongLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public long put(long key, long value) {
		return _put(key, value, MODE.LAST);
	}

	public long putLast(long key, long value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public long putFirst(long key, long value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized long _put(long key, long value, MODE m) {
		LongLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				long old = e.value;
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
					long v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					long k = header.link_next.key;
					long v = remove(k);
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
		LongLongLinkedEntry e = new LongLongLinkedEntry(key, value, tab[index]);
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
		return NONE;
	}

	private void overflowed(long k, long v) {
		// TODO Auto-generated method stub
		
	}

	public synchronized long remove(long key) {
		LongLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongLongLinkedEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				long oldValue = e.value;
				e.value = NONE;
				//
				unchain(e);

				return oldValue;
			}
		}
		return NONE;
	}

	public synchronized long removeFirst() {
		if (isEmpty())
			return 0;
		return remove(header.link_next.key);
	}

	public synchronized long removeLast() {
		if (isEmpty())
			return 0;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		LongLongLinkedEntry tab[] = table;
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
			LongLongLinkedEntry e = (LongLongLinkedEntry) (it.nextElement());
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
			LongLongLinkedEntry e = (LongLongLinkedEntry) it.nextElement();
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
		LongLongLinkedEntry entry = LongLongLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public V nextElement() {
			if (hasMoreElements()) {
				LongLongLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return (V) new Long(e.key);
				case VALUES:
					return (V) new Long(e.value);
				default:
					return (V) e;
				}
			}
			throw new NoSuchElementException("no more next");
		}

		public long nextLong() {
			if (hasMoreElements()) {
				LongLongLinkedEntry e = entry;
				entry = e.link_next;
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

	private void chain(LongLongLinkedEntry link_prev, LongLongLinkedEntry link_next, LongLongLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LongLongLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class LongLongLinkedEntry {
		long key;
		long value;
		LongLongLinkedEntry next;
		LongLongLinkedEntry link_next, link_prev;

		protected LongLongLinkedEntry(long key, long value, LongLongLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new LongLongLinkedEntry(key, value, (next == null ? null : (LongLongLinkedEntry) next.clone()));
		}

		public long getKey() {
			return key;
		}

		public long getValue() {
			return value;
		}

		public long setValue(long value) {

			long oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof LongLongLinkedEntry))
				return false;
			LongLongLinkedEntry e = (LongLongLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return (int) (key ^ (key >>> 32)) ^ (int) (value ^ (value >>> 32));
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	public static void main(String[] args) {
		LongLongLinkedMap m = new LongLongLinkedMap();
		// m.put(1, 1);
		// m.put(2, 1);
		// m.put(3, 1);
		// m.put(4, 1);
		//
		LongEnumer e = m.keys();
		System.out.println(e.nextLong());
		System.out.println(e.nextLong());
		System.out.println(e.nextLong());
		System.out.println(e.nextLong());

		// System.out.println(e.nextLong());

	}

	private static void print(Object e) {
		System.out.println(e);
	}

}
