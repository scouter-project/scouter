/*
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
 * 
 *  The initial idea for this class is from "org.apache.commons.lang.IntHashMap"; 
 *  http://commons.apache.org/commons-lang-2.6-src.zip
 *
 */
package scouter.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;


public class LongKeyLinkedMap<V>  {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;


	private ENTRY<V> table[];
	private ENTRY<V> header;
	
	private int count;
	private int threshold;
	private float loadFactor;

	public LongKeyLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);

		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new ENTRY[initCapacity];
		
		this.header =new ENTRY(0,null,null);
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
		long[] _keys=new long[this.size()];
	    LongEnumer en = this.keys();
		for(int i = 0 ; i<_keys.length;i++)
			_keys[i]=en.nextLong();
		return _keys;
	}
	public synchronized LongEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer<V>(TYPE.VALUES);
	}

	public synchronized Enumeration<ENTRY> entries() {
		return new Enumer<ENTRY>(TYPE.ENTRIES);
	}
	
	
	public synchronized boolean containsValue(Object value) {
		if (value == null) {
			return false;
		}

		ENTRY tab[] = table;
		int i = tab.length; while(i-->0){
			for (ENTRY e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value,value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(long key) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(long key) {
		ENTRY<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				return  e.value;
			}
		}
		return null;
	}
	public synchronized long getFirstKey() {
	     return this.header.link_next.key;
	}
	public synchronized long getLastKey() {
	     return this.header.link_prev.key;
	}
	public synchronized V getFirstValue() {
	    return this.header.link_next.value;
	}
	public synchronized V getLastValue() {
	    return this.header.link_prev.value;
	}
	
	private int hash(long key) {
		return (int) (key ^ (key >>> 32)) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		ENTRY oldMap[] = table;

		int newCapacity = oldCapacity * 2 + 1;
		ENTRY newMap[] = new ENTRY[newCapacity];

		threshold = (int) (newCapacity * loadFactor);
		table = newMap;

		for (int i = oldCapacity; i-- > 0;) {
			ENTRY old = oldMap[i]; while(old!=null) {
				ENTRY e = old;
				old = old.next;

				long key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;
	public LongKeyLinkedMap<V> setMax(int max){
		this.max=max;
		return this;
	}
    private static enum MODE{FORCE_FIRST, FORCE_LAST, FIRST, LAST};
	public  V put(long key, V value) {
		return _put(key, value, MODE.LAST);
	}
	public  V putLast(long key, V value) {
		return _put(key, value, MODE.FORCE_LAST);
	}
	public  V putFirst(long key, V value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}
 	private synchronized V _put(long key, V value, MODE m) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				V old = e.value;
				e.value = value;
				switch(m){
				case FORCE_FIRST:
					if(header.link_next!=e){
						unchain(e);
						chain(header, header.link_next, e);
					}
					break;
				case FORCE_LAST:
					if(header.link_prev!=e){
						unchain(e);
						chain(header.link_prev, header, e);
					}
					break;
				}
				return old;
			}
		}
     
	   if(max >0){
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
		
		switch(m){
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
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
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

	public  String toString() {

		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();

		buf.append("{");
		for(int i=0;it.hasMoreElements();i++){
			ENTRY e = (ENTRY) (it.nextElement());
			if (i>0)
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
		while(it.hasMoreElements()){
			ENTRY e =  (ENTRY)it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}


	public static class ENTRY<V> {
		long key;
		V value;
		ENTRY<V> next;
		ENTRY<V> link_next, link_prev;

		protected ENTRY(long key, V value, ENTRY<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(key, value, (next == null ? null : (ENTRY) next.clone()));
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
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return 	CompareUtil.equals(e.key,key) && CompareUtil.equals(e.value,value);
		}

		public int hashCode() {
			return (int) (key ^ (key >>> 32)) ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	 private enum TYPE{KEYS, VALUES, ENTRIES }
	
	private class Enumer<V> implements Enumeration, LongEnumer {

  		TYPE type;
		ENTRY entry = LongKeyLinkedMap.this.header.link_next;
		
		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header!=entry;
		}

		public V nextElement() {
			if (entry != null) {
				ENTRY e  = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return (V)new Long(e.key);
				case VALUES:
					return (V)e.value;
				default:
					return (V)e;
				}
			}
			throw new NoSuchElementException("Enumerator");
		}

		public long nextLong() {		
			if (entry != null) {
				ENTRY e  = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("Enumer");
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
		LongKeyLinkedMap m = new LongKeyLinkedMap().setMax(2);
		m.putLast(10, 10);
		m.putLast(20, 20);
		m.putFirst(30, 30);
		
		//m.removeFirst();
		System.out.println(m);

		m = new LongKeyLinkedMap().setMax(2);
		m.putFirst(10, 10);
		m.putFirst(20, 20);
		m.putFirst(30, 30);
		
		//m.removeLast();
		System.out.println(m);

	}

	private static void print(Object e) {
		System.out.println(e);
	}

	
	
}