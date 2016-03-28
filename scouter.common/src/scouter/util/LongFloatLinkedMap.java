/*
 * 
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
public class LongFloatLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongFloatLinkedEntry table[];
	private LongFloatLinkedEntry header;
	private int count;
	private int threshold;
	private float loadFactor;
	private int NONE = 0;

	public LongFloatLinkedMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public LongFloatLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LongFloatLinkedEntry[initCapacity];
		this.header = new LongFloatLinkedEntry(0, 0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public LongFloatLinkedMap() {
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

	public synchronized FloatEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<LongFloatLinkedEntry> entries() {
		return new Enumer<LongFloatLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(float value) {
		LongFloatLinkedEntry tab[] = table;
		for (int i = tab.length; i-- > 0;) {
			for (LongFloatLinkedEntry e = tab[i]; e != null; e = e.hash_next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(long key) {
		LongFloatLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		LongFloatLinkedEntry e = tab[index];
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
			e = e.hash_next;
		}
		return false;
	}

	public synchronized float get(long key) {
		LongFloatLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongFloatLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
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

	public synchronized float getFirstValue() {
		return this.header.link_next.value;
	}

	public synchronized float getLastValue() {
		return this.header.link_prev.value;
	}

	private int hash(long key) {
		return (int) (key ^ (key >>> 32)) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LongFloatLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongFloatLinkedEntry newMap[] = new LongFloatLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongFloatLinkedEntry old = oldMap[i];
			while (old != null) {
				LongFloatLinkedEntry e = old;
				old = old.hash_next;
				long key = e.key;
				int index = hash(key) % newCapacity;
				e.hash_next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LongFloatLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public float put(long key, float value) {
		return _put(key, value, MODE.LAST);
	}

	public float putLast(long key, float value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public float putFirst(long key, float value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized float _put(long key, float value, MODE m) {
		LongFloatLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (LongFloatLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
			if (CompareUtil.equals(e.key, key)) {
				float old = e.value;
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
					float v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					long k = header.link_next.key;
					float v = remove(k);
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
		LongFloatLinkedEntry e = new LongFloatLinkedEntry(key, value, tab[index]);
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

	protected void overflowed(long key, float value) {
	}

	public synchronized float remove(long key) {
		LongFloatLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		LongFloatLinkedEntry e = tab[index];
		LongFloatLinkedEntry prev = null;
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.hash_next = e.hash_next;
				} else {
					tab[index] = e.hash_next;
				}
				count--;
				float oldValue = e.value;
				e.value = NONE;
				//
				unchain(e);
				return oldValue;
			}
			prev = e;
			e = e.hash_next;
		}
		return NONE;
	}

	public synchronized float removeFirst() {
		if (isEmpty())
			return 0;
		return remove(header.link_next.key);
	}

	public synchronized float removeLast() {
		if (isEmpty())
			return 0;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		LongFloatLinkedEntry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		this.header.link_next = header;
		this.header.link_prev = header;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			LongFloatLinkedEntry e = (LongFloatLinkedEntry) (it.nextElement());
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
			LongFloatLinkedEntry e = (LongFloatLinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration, FloatEnumer, LongEnumer {
		TYPE type;
		LongFloatLinkedEntry entry = LongFloatLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return entry != null && header != entry;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				LongFloatLinkedEntry e = entry;
				entry = e.link_next;
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

		public float nextFloat() {
			if (hasMoreElements()) {
				LongFloatLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case VALUES:
					return e.value;
				}
			}
			throw new NoSuchElementException("no more next");
		}

		public long nextLong() {
			if (hasMoreElements()) {
				LongFloatLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return e.key;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(LongFloatLinkedEntry link_prev, LongFloatLinkedEntry link_next, LongFloatLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LongFloatLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	private static void print(Object e) {
		System.out.println(e);
	}

	public static class LongFloatLinkedEntry {
		long key;
		float value;
		LongFloatLinkedEntry hash_next;
		LongFloatLinkedEntry link_next, link_prev;

		protected LongFloatLinkedEntry(long key, float value, LongFloatLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.hash_next = next;
		}

		protected Object clone() {
			return new LongFloatLinkedEntry(key, value,
					(hash_next == null ? null : (LongFloatLinkedEntry) hash_next.clone()));
		}

		public long getKey() {
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
			if (!(o instanceof LongFloatLinkedEntry))
				return false;
			LongFloatLinkedEntry e = (LongFloatLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			int x = Float.floatToIntBits(value);
			return x;
		}

		public String toString() {
			return key + "=" + value;
		}
	}
}
