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
public class IntIntLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private IntIntLinkedEntry table[];
	private IntIntLinkedEntry header;
	private int count;
	private int threshold;
	private float loadFactor;

	private int NONE = 0;

	public IntIntLinkedMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public IntIntLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new IntIntLinkedEntry[initCapacity];
		this.header = new IntIntLinkedEntry(0, 0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public IntIntLinkedMap() {
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

	public synchronized IntEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<IntIntLinkedEntry> entries() {
		return new Enumer<IntIntLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(int value) {
		IntIntLinkedEntry tab[] = table;
		for (int i = tab.length; i-- > 0;) {
			for (IntIntLinkedEntry e = tab[i]; e != null; e = e.hash_next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(int key) {
		IntIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		IntIntLinkedEntry e = tab[index];
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
			e = e.hash_next;
		}
		return false;
	}

	public synchronized int get(int key) {
		IntIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
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

	public synchronized int getFirstValue() {
		return this.header.link_next.value;
	}

	public synchronized int getLastValue() {
		return this.header.link_prev.value;
	}

	private int hash(int key) {
		return key & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		IntIntLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntIntLinkedEntry newMap[] = new IntIntLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			IntIntLinkedEntry old = oldMap[i];
			while (old != null) {
				IntIntLinkedEntry e = old;
				old = old.hash_next;
				int key = e.key;
				int index = hash(key) % newCapacity;
				e.hash_next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public IntIntLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public int put(int key, int value) {
		return _put(key, value, MODE.LAST);
	}

	public int putLast(int key, int value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public int putFirst(int key, int value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	public int add(int key, int value) {
		return _add(key, value, MODE.LAST);
	}

	public int addLast(int key, int value) {
		return _add(key, value, MODE.FORCE_LAST);
	}

	public int addFirst(int key, int value) {
		return _add(key, value, MODE.FORCE_FIRST);
	}

	private synchronized int _put(int key, int value, MODE m) {
		IntIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
			if (CompareUtil.equals(e.key, key)) {
				int old = e.value;
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
					int v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					int k = header.link_next.key;
					int v = remove(k);
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
		IntIntLinkedEntry e = new IntIntLinkedEntry(key, value, tab[index]);
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

	protected void overflowed(int key, int value) {
	}

	private synchronized int _add(int key, int value, MODE m) {
		IntIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (IntIntLinkedEntry e = tab[index]; e != null; e = e.hash_next) {
			if (CompareUtil.equals(e.key, key)) {
				e.value += value;
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
				return e.value;
			}
		}
		if (max > 0) {
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					//removeLast();
					int k = header.link_prev.key;
					int v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					//removeFirst();
					int k = header.link_next.key;
					int v = remove(k);
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
		IntIntLinkedEntry e = new IntIntLinkedEntry(key, value, tab[index]);
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

	public synchronized int remove(int key) {
		IntIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;

		IntIntLinkedEntry e = tab[index];
		IntIntLinkedEntry prev = null;
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.hash_next = e.hash_next;
				} else {
					tab[index] = e.hash_next;
				}
				count--;
				int oldValue = e.value;
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

	public synchronized int removeFirst() {
		if (isEmpty())
			return 0;
		return remove(header.link_next.key);
	}

	public synchronized int removeLast() {
		if (isEmpty())
			return 0;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		IntIntLinkedEntry tab[] = table;
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
			IntIntLinkedEntry e = (IntIntLinkedEntry) (it.nextElement());
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
			IntIntLinkedEntry e = (IntIntLinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration, IntEnumer {
		TYPE type;
		IntIntLinkedEntry entry = IntIntLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return entry != null && header != entry;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				IntIntLinkedEntry e = entry;
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
				IntIntLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return e.key;
				case VALUES:
					return e.value;
				}
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(IntIntLinkedEntry link_prev, IntIntLinkedEntry link_next, IntIntLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(IntIntLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class IntIntLinkedEntry {
		int key;
		int value;
		IntIntLinkedEntry hash_next;
		IntIntLinkedEntry link_next, link_prev;

		protected IntIntLinkedEntry(int key, int value, IntIntLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.hash_next = next;
		}

		protected Object clone() {
			return new IntIntLinkedEntry(key, value,
					(hash_next == null ? null : (IntIntLinkedEntry) hash_next.clone()));
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
			if (!(o instanceof IntIntLinkedEntry))
				return false;
			IntIntLinkedEntry e = (IntIntLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return key ^ value;
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	public synchronized void sort(Comparator<IntIntLinkedEntry> c){
		ArrayList<IntIntLinkedEntry> list = new ArrayList<IntIntLinkedEntry>(this.size());
		Enumeration<IntIntLinkedEntry> en = this.entries();
		while(en.hasMoreElements()){
			list.add(en.nextElement());
		}
		Collections.sort(list, c);
		this.clear();
		for(int i = 0 ; i<list.size() ; i++){
			IntIntLinkedEntry e = list.get(i);
			this.put(e.getKey(), e.getValue());
		}
	}
}
