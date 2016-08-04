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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;
/**
 * @author Paul Kim (sjkim@whatap.io)
 */
public class IntLongLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private IntLongLinkedEntry table[];
	private IntLongLinkedEntry header;
	private int count;
	private int threshold;
	private float loadFactor;
	private int NONE = 0;

	public IntLongLinkedMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public IntLongLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new IntLongLinkedEntry[initCapacity];
		this.header = new IntLongLinkedEntry(0, 0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public IntLongLinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public int[] keyArray() {
		int[] _keys = new int[this.size()];
		IntEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextInt();
		return _keys;
	}

	public synchronized IntEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized LongEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<IntLongLinkedEntry> entries() {
		return new Enumer<IntLongLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(int value) {
		IntLongLinkedEntry tab[] = table;
		for (int i = tab.length; i-- > 0;) {
			for (IntLongLinkedEntry e = tab[i]; e != null; e = e.hash_next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(int key) {
		IntLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		IntLongLinkedEntry e = tab[index];
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
			e = e.hash_next;
		}
		return false;
	}

	public synchronized long get(int key) {
		IntLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntLongLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return NONE;
	}

	public synchronized int getFirstKey() {
		return this.header.link_next.key;
	}

	public synchronized int getLastKey() {
		return this.header.link_prev.key;
	}

	public synchronized long getFirstValue() {
		return this.header.link_next.value;
	}

	public synchronized long getLastValue() {
		return this.header.link_prev.value;
	}

	private int hash(int key) {
		return key & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		IntLongLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntLongLinkedEntry newMap[] = new IntLongLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			IntLongLinkedEntry old = oldMap[i];
			while (old != null) {
				IntLongLinkedEntry e = old;
				old = old.hash_next;
				int key = e.key;
				int index = hash(key) % newCapacity;
				e.hash_next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public IntLongLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public long put(int key, long value) {
		return _put(key, value, MODE.LAST);
	}

	public long putLast(int key, long value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public long putFirst(int key, long value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized long _put(int key, long value, MODE m) {
		IntLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntLongLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
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
					int k = header.link_prev.key;
					long v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					int k = header.link_next.key;
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
		IntLongLinkedEntry e = new IntLongLinkedEntry(key, value, tab[index]);
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

	protected void overflowed(int key, long value) {
	}

	public synchronized long remove(int key) {
		IntLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		IntLongLinkedEntry e = tab[index];
		IntLongLinkedEntry prev = null;
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.hash_next = e.hash_next;
				} else {
					tab[index] = e.hash_next;
				}
				count--;
				long oldValue = e.value;
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
		IntLongLinkedEntry tab[] = table;
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
			IntLongLinkedEntry e = (IntLongLinkedEntry) (it.nextElement());
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
			IntLongLinkedEntry e = (IntLongLinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration, IntEnumer, LongEnumer {
		TYPE type;
		IntLongLinkedEntry entry = IntLongLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return entry != null && header != entry;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				IntLongLinkedEntry e = entry;
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

		public int nextInt() {
			if (hasMoreElements()) {
				IntLongLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return e.key;
				}
			}
			throw new NoSuchElementException("no more next");
		}

		public long nextLong() {
			if (hasMoreElements()) {
				IntLongLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case VALUES:
					return e.value;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(IntLongLinkedEntry link_prev, IntLongLinkedEntry link_next, IntLongLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(IntLongLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class IntLongLinkedEntry {
		int key;
		long value;
		IntLongLinkedEntry hash_next;
		IntLongLinkedEntry link_next, link_prev;

		protected IntLongLinkedEntry(int key, long value, IntLongLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.hash_next = next;
		}

		protected Object clone() {
			return new IntLongLinkedEntry(key, value,
					(hash_next == null ? null : (IntLongLinkedEntry) hash_next.clone()));
		}

		public int getKey() {
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
			if (!(o instanceof IntLongLinkedEntry))
				return false;
			IntLongLinkedEntry e = (IntLongLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return key ^ (int) (value ^ (value >>> 32));
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	public synchronized void sort(Comparator<IntLongLinkedEntry> c){
		ArrayList<IntLongLinkedEntry> list = new ArrayList<IntLongLinkedEntry>(this.size());
		Enumeration<IntLongLinkedEntry> en = this.entries();
		while(en.hasMoreElements()){
			list.add(en.nextElement());
		}
		Collections.sort(list, c);
		this.clear();
		for(int i = 0 ; i<list.size() ; i++){
			IntLongLinkedEntry e = list.get(i);
			this.put(e.getKey(), e.getValue());
		}
	}
	
	public static void main(String[] args) {
		IntLongLinkedMap m = new IntLongLinkedMap().setMax(6);
		for (int i = 0; i < 10; i++) {
			m.put(i, i);
			System.out.println(m);
		}
		System.out.println();
		// m.putFirst(1, 0);
		System.out.println(m);
		System.out.println("==================================");
		IntEnumer en = m.keys();
		while (en.hasMoreElements()) {
			m.remove(5);
			System.out.println(en.nextInt());
		}
		// System.out.println("==================================");
		// for (int i = 0; i < 10; i++) {
		// m.putLast(i, i);
		// System.out.println(m);
		// }
		// System.out.println("==================================");
		// for (int i = 0; i < 10; i++) {
		// m.putFirst(i, i);
		// System.out.println(m);
		// }
		// System.out.println("==================================");
		// for (int i = 0; i < 10; i++) {
		// m.removeFirst();
		// System.out.println(m);
		// }
	}

	private static void print(Object e) {
		System.out.println(e);
	}
}
