/*
 * 
 *  Copyright 2015 the original author or authors.
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
 *  
 */
package scouter.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class IntIntLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private ENTRY table[];
	private ENTRY header;

	private int count;
	private int threshold;
	private float loadFactor;
	
	private int NONE=0;
	public IntIntLinkedMap setNullValue(int none){
		this.NONE=none;
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
		this.table = new ENTRY[initCapacity];

		this.header = new ENTRY(0, 0, null);
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

	public synchronized Enumeration<ENTRY> entries() {
		return new Enumer<ENTRY>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(int value) {
		ENTRY tab[] = table;
		for (int i = tab.length; i-- > 0;) {
			for (ENTRY e = tab[i]; e != null; e = e.hash_next) {
				if (CompareUtil.equals(e.value,value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(int key) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		ENTRY e = tab[index];
		while (e != null) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
			e = e.hash_next;
		}
		return false;
	}

	public synchronized int get(int key) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.hash_next) {
			if (CompareUtil.equals(e.key,key)) {
				return  e.value;
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
		ENTRY oldMap[] = table;

		int newCapacity = oldCapacity * 2 + 1;
		ENTRY newMap[] = new ENTRY[newCapacity];

		threshold = (int) (newCapacity * loadFactor);
		table = newMap;

		for (int i = oldCapacity; i-- > 0;) {
			ENTRY old = oldMap[i];
			while (old != null) {
				ENTRY e = old;
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

	private synchronized int _put(int key, int value, MODE m) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.hash_next) {
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
			switch(m){
			case FORCE_FIRST:
			case FIRST:
				  while(count >= max){
					  removeLast();
				  }
				break;
			case FORCE_LAST:
			case LAST:
				while(count >= max){
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

		ENTRY e = new ENTRY(key, value, tab[index]);
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

	public synchronized int remove(int key) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		
		ENTRY e = tab[index];
		ENTRY prev = null;
		while ( e != null ) {
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
		ENTRY tab[] = table;
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
			ENTRY e = (ENTRY) (it.nextElement());
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
			ENTRY e = (ENTRY) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	public static class ENTRY {
		int key;
		int value;
		ENTRY hash_next;
		ENTRY link_next, link_prev;

		protected ENTRY(int key, int value, ENTRY next) {
			this.key = key;
			this.value = value;
			this.hash_next = next;
		}

		protected Object clone() {
			return new ENTRY(key, value, (hash_next == null ? null : (ENTRY) hash_next.clone()));
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
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return key ^ value;
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	private enum TYPE{KEYS, VALUES, ENTRIES }

	private class Enumer<V> implements Enumeration, IntEnumer {

		TYPE type;
		ENTRY entry = IntIntLinkedMap.this.header.link_next;

		Enumer(TYPE  type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return entry != null && header != entry;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				ENTRY e = entry;
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
				ENTRY e = entry;
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

	public static void main(String[] args) {
		IntIntLinkedMap m = new IntIntLinkedMap().setMax(6);

		for (int i = 0; i < 10; i++) {
			m.put(i, i);
			System.out.println(m);
		}
		System.out.println();
		//m.putFirst(1, 0);
		
		System.out.println(m);
		System.out.println("==================================");
		IntEnumer en = m.keys();
		while(en.hasMoreElements()){
			m.remove(5);
			System.out.println(en.nextInt());
		}
	
//		System.out.println("==================================");
//		for (int i = 0; i < 10; i++) {
//			m.putLast(i, i);
//			System.out.println(m);
//		}
//		System.out.println("==================================");
//		for (int i = 0; i < 10; i++) {
//			m.putFirst(i, i);
//			System.out.println(m);
//		}
//		System.out.println("==================================");
//		for (int i = 0; i < 10; i++) {
//			m.removeFirst();
//			System.out.println(m);
//		}

	}

	private static void print(Object e) {
		System.out.println(e);
	}

}