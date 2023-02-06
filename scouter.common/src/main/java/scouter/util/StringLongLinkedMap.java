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
public class StringLongLinkedMap  {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private StringLongLinkedEntry[] table;
	private StringLongLinkedEntry header;
	private int count;
	private int threshold;
	private final float loadFactor;
	public StringLongLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new StringLongLinkedEntry[initCapacity];
		this.header = new StringLongLinkedEntry(null, 0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}
	public StringLongLinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	private long NONE=0;
	public StringLongLinkedMap setNullValue(long none){
		this.NONE=none;
		return this;
	}
	public int size() {
		return count;
	}
	public String[] keyArray() {
		String[] _keys=new String[this.size()];
		StringEnumer en = this.keys();
		for(int i = 0 ; i<_keys.length;i++)
			_keys[i]=en.nextString();
		return _keys;
	}
	
	public synchronized StringEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}
	public synchronized LongEnumer values() {
		return new Enumer(TYPE.VALUES);
	}
	public synchronized Enumeration<StringLongLinkedEntry> entries() {
		return new Enumer(TYPE.ENTRIES);
	}
	public synchronized boolean containsValue(long value) {
		StringLongLinkedEntry[] tab = table;
		int i = tab.length; while(i-->0){
			for (StringLongLinkedEntry e = tab[i]; e != null; e = e.next) {
				if (e.value == value) {
					return true;
				}
			}
		}
		return false;
	}
	public synchronized boolean containsKey(String key) {
		StringLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				return true;
			}
		}
		return false;
	}
	public synchronized long get(String key) {
		if(key==null)
			return NONE;
		
		StringLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
				return e.value;
			}
		}
		return NONE;
	}
	public synchronized long getFirsValue() {
		if (isEmpty())
			return NONE;
		return this.header.link_next.value;
	}
	public synchronized long getLastValue() {
		if (isEmpty())
			return NONE;
		return this.header.link_prev.value;
	}
	private int hash(String key) {
		return key.hashCode() & Integer.MAX_VALUE;
	}
	protected void rehash() {
		int oldCapacity = table.length;
		StringLongLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		StringLongLinkedEntry newMap[] = new StringLongLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			StringLongLinkedEntry old = oldMap[i]; while(old!=null) {
				StringLongLinkedEntry e = old;
				old = old.next;
				String key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}
	private int max;
	public StringLongLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}
	private enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};
	public long put(String key, long value) {
		return _put(key, value, MODE.LAST);
	}
	public long putLast(String key, long value) {
		return _put(key, value, MODE.FORCE_LAST);
	}
	public long putFirst(String key, long value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}
	private synchronized long _put(String key, long value, MODE m) {
		if(key==null)
			return NONE;
		
		StringLongLinkedEntry[] tab = table;
		int index = hash(key) % tab.length;
		for (StringLongLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
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
			switch(m){
			case FORCE_FIRST:
			case FIRST:
				  while(count >= max){
					  //removeLast();
					  String k=header.link_prev.key;
					  long v= remove(k);
					  overflowed(k,v);
				  }
				break;
			case FORCE_LAST:
			case LAST:
				while(count >= max){
					  //removeFirst();
					  String k=header.link_next.key;
					  long v= remove(k);
					  overflowed(k,v);
				  }
				break;
			}
		}
		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}
		StringLongLinkedEntry e = new StringLongLinkedEntry(key, value, tab[index]);
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
	protected void overflowed(String key, long value) {
	}
	public synchronized long remove(String key) {
		if(key==null)
			return NONE;
		StringLongLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringLongLinkedEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key,key)) {
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
			return NONE;
		return remove(header.link_next.key);
	}
	public synchronized long removeLast() {
		if (isEmpty())
			return NONE;
		return remove(header.link_prev.key);
	}
	public boolean isEmpty() {
		return size() == 0;
	}
	public synchronized void clear() {
		StringLongLinkedEntry tab[] = table;
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
			StringLongLinkedEntry e = (StringLongLinkedEntry) (it.nextElement());
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
			StringLongLinkedEntry e =  (StringLongLinkedEntry)it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}
	public static class StringLongLinkedEntry {
		String key;
		long value;
		StringLongLinkedEntry next;
		StringLongLinkedEntry link_next, link_prev;
		protected StringLongLinkedEntry(String key, long value, StringLongLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
		protected Object clone() {
			return new StringLongLinkedEntry(key, value, (next == null ? null : (StringLongLinkedEntry) next.clone()));
		}
		public String getKey() {
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
			if (!(o instanceof StringLongLinkedEntry))
				return false;
			StringLongLinkedEntry e = (StringLongLinkedEntry) o;
			return 	CompareUtil.equals(e.key,key) && CompareUtil.equals(e.value,value);
		}
		public int hashCode() {
			return key.hashCode() ^  (int) (value ^ (value >>> 32));
		}
		public String toString() {
			return key + "=" + value;
		}
	}
	private enum TYPE{KEYS, VALUES, ENTRIES }
	 
	private class Enumer implements Enumeration, StringEnumer, LongEnumer {
		TYPE type;
		StringLongLinkedEntry entry = StringLongLinkedMap.this.header.link_next;
		Enumer(TYPE type) {
			this.type = type;
		}
		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}
		public Object nextElement() {
			if (hasMoreElements()) {
				StringLongLinkedEntry e = entry;
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
		public long nextLong() {
			if (hasMoreElements()) {
				StringLongLinkedEntry e = entry;
				entry = e.link_next;
				return e.value;
			}
			throw new NoSuchElementException("no more next");
		}
		public String nextString() {
			if (hasMoreElements()) {
				StringLongLinkedEntry e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}
	private void chain(StringLongLinkedEntry link_prev, StringLongLinkedEntry link_next, StringLongLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}
	private void unchain(StringLongLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}
	public static void main(String[] args) {
	
	}
	
	
}
