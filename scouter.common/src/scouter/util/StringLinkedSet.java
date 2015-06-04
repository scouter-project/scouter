/*
 *  Copyright 2015 LG CNS.
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

import java.util.NoSuchElementException;

public class StringLinkedSet  {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;


	private ENTRY table[];
	private ENTRY header;
	
	private int count;
	private int threshold;
	private float loadFactor;

	public StringLinkedSet(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);

		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new ENTRY[initCapacity];
		
		this.header =new ENTRY(null,null);
		this.header.link_next = header.link_prev = header;
		
		threshold = (int) (initCapacity * loadFactor);
	}

	public StringLinkedSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}
	public String[] getArray() {
		String[] _keys=new String[this.size()];
	    StringEnumer en = this.elements();
		for(int i = 0 ; i<_keys.length;i++)
			_keys[i]=en.nextString();
		return _keys;
	}
	public synchronized StringEnumer elements() {
		return new Enumer();
	}
	public synchronized boolean contains(String key) {
		if(key==null)
			return false;
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized String getFirst() {
	     return this.header.link_next.key;
	}
	public synchronized String getLast() {
	     return this.header.link_prev.key;
	}
	
	private int hash(String key) {
		return key.hashCode() & Integer.MAX_VALUE;
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

				String key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;
	public StringLinkedSet setMax(int max){
		this.max=max;
		return this;
	}
    private static enum MODE{FORCE_FIRST, FORCE_LAST, FIRST, LAST};
	public  String put(String key) {
		return _put(key, MODE.LAST);
	}
	public  String putLast(String key) {
		return _put(key, MODE.FORCE_LAST);
	}
	public  String putFirst(String key) {
		return _put(key, MODE.FORCE_FIRST);
	}
 	private synchronized String _put(String key, MODE m) {
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
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
				return e.key;
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

		ENTRY e = new ENTRY(key, tab[index]);
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

 	public String unipoint(String key){
 		String old = _put(key, MODE.LAST);
 		return old==null?key:old;
 	}
	public synchronized String remove(String key) {
		if(key==null)
			return null;
		ENTRY tab[] = table;
		int index = hash(key) % tab.length;
		for (ENTRY e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				//
				unchain(e);
				
				return key;
			}
		}
		return null;
	}
	public synchronized String removeFirst() {
		if (isEmpty())
			return null;
		return remove(header.link_next.key);
	}
	public synchronized String removeLast() {
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
		StringEnumer it = elements();

		buf.append("{");
		while(it.hasMoreElements()) {
			if (buf.length()>1)
				buf.append(",");
			buf.append(it.nextString());
		
		}
		buf.append("}");
		return buf.toString();
	}



	public static class ENTRY<V> {
		String key;
		ENTRY<V> next;
		ENTRY<V> link_next, link_prev;

		protected ENTRY(String key,  ENTRY<V> next) {
			this.key = key;
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(key,  (next == null ? null : (ENTRY) next.clone()));
		}

		public String getKey() {
			return key;
		}


		public boolean equals(Object o) {
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return 	CompareUtil.equals(e.key,key) ;
		}

		public int hashCode() {
			return key.hashCode() ;
		}

		public String toString() {
			return key ;
		}
	}

	private class Enumer implements  StringEnumer {

  		ENTRY entry = StringLinkedSet.this.header.link_next;
		
		public boolean hasMoreElements() {
			return header!=entry && entry!=null;
		}
		public String nextString() {		
			if (hasMoreElements()) {
				ENTRY e  = entry;
				entry = e.link_next;
				return e.key;
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

}