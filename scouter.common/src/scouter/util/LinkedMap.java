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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * <pre>
 * LinkedMap = LinkedList + HashMap
 * 1. 순처 조회및 해쉬조회 가능
 *    - key() Enumeration : 입력된 순서가 보장된 조회 
 *    - get(key) : key에 대한 hash조회
 *    - getFirst() : 첫번째 입력된 데이터 조회
 *    - getLast() : 마지막 입력된 데이터 조회   
 * 2. 입력 순서 유지및 변경 가능
 *    - putFirst- 기존 입력된 데이터를 다시 처음으로 입력
 *    - putLast - 기존 입력된 데이터를 다시 마지막으로 입력
 *    - put - 기존 입력된 데이터를 값만 변경하고 없으면 마지막으로 입력 
 * 3. 최대 Max Size를 지정 및 재설정 가능, 초과시 밀어내기 삭제
 *    - setMax(int)로 최대 크기를 변경가능  
 *    - 밀어내기 삭제시 이벤트 처리  overflowed()
 * 4. intern기능
 *    - 데이터가 존재하면 get()과 같고 없으면  
 *    - create()로 데이터를 만들고
 * 5. 입력 순서를 고려한 데이터 삭제 가능
 *    - remove(key) : 입력된 키를 삭제 
 *    - removeFirst :  처음 입력된 데이터 삭제 
 *    - removeLast : 마지막 입력된 데이터 삭제 
 * 
 * LinkedMap<String,Connection> map = new LinkedMap<String,Connection>(){
 *   protected Connection create(String key){
 *      return Manager.connect();
 *   }
 *  } 
 *  
 *  Connection con = map.intern("con1");
 *  con != null이 보장된다. intern함수는 값이 nul인경우 create()를 호출하여
 *   새로운 값을 생성하여 저장하고 리턴한다. 
 *   
 *  LinkedMap<String,Connection> map = new LinkedMap<String,Connection>(){
 *   protected void overflowed(String key, Connection con){
 *      con.close();
 *   }
 *  }.setMax(10);
 *  
 *  Connection con = Manager.connect();
 *  map.put("con10", con);
 *  map.size()==10인 상태에서 put()함수가 호출되면 초과되는 데이터가 
 *  overflowed()에 전달된다.
 * </pre>
 * 
 * @author Paul Kim(sjkim@whatap.io)
 *
 */
public class LinkedMap<K, V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LinkedEntry<K, V> table[];
	private LinkedEntry<K, V> header;
	private int count;
	private int threshold;
	private float loadFactor;

	public LinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LinkedEntry[initCapacity];
		this.header = new LinkedEntry(null, null, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public LinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
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

	public synchronized Enumeration<LinkedEntry<K, V>> entries() {
		return new Enumer<LinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(V value) {
		if (value == null) {
			return false;
		}
		LinkedEntry<K, V> tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (LinkedEntry<K, V> e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(Object key) {
		if (key == null)
			return false;
		LinkedEntry<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedEntry<K, V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(K key) {
		if (key == null)
			return null;
		LinkedEntry<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedEntry<K, V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return null;
	}

	public synchronized V getFirstValue() {
		if (isEmpty())
			return null;
		return this.header.link_next.value;
	}

	public synchronized V getLastValue() {
		if (isEmpty())
			return null;
		return this.header.link_prev.value;
	}

	private int hash(Object key) {
		return (int) (key.hashCode()) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LinkedEntry<K, V> oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LinkedEntry<K, V> newMap[] = new LinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (LinkedEntry<K, V> old = oldMap[i]; old != null;) {
				LinkedEntry<K, V> e = old;
				old = old.next;
				K key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LinkedMap<K, V> setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public V put(K key, V value) {
		return _put(key, value, MODE.LAST);
	}

	public V putLast(K key, V value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public V putFirst(K key, V value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized V _put(K key, V value, MODE m) {
		LinkedEntry<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedEntry<K, V> e = tab[index]; e != null; e = e.next) {
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
					K k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					K k = header.link_next.key;
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
		LinkedEntry<K, V> e = new LinkedEntry(key, value, tab[index]);
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

	protected void overflowed(K key, V value) {
	}

	protected V create(K key) {
		throw new RuntimeException("not implemented create()");
	}

	public V intern(K key) {
		return _intern(key, MODE.LAST);
	}

	private synchronized V _intern(K key, MODE m) {
		LinkedEntry<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedEntry<K, V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				V old = e.value;
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

		V value = create(key);
		if (value == null)
			return null;

		if (max > 0) {
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					K k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					K k = header.link_next.key;
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
		LinkedEntry e = new LinkedEntry(key, value, tab[index]);
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

	public synchronized V remove(K key) {
		if (key == null)
			return null;
		LinkedEntry<K, V> tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedEntry<K, V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
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
		LinkedEntry<K, V> tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		this.header.link_next = header.link_prev = header;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration<LinkedEntry<K, V>> it = entries();
		buf.append("{");
		for (int i = 0; it.hasMoreElements(); i++) {
			LinkedEntry<K, V> e = (LinkedEntry<K, V>) (it.nextElement());
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
			LinkedEntry e = (LinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration {
		TYPE type;
		LinkedEntry entry = LinkedMap.this.header.link_next;
		LinkedEntry lastEnt;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public V nextElement() {
			if (hasMoreElements()) {
				LinkedEntry e = lastEnt = entry;
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

	private void chain(LinkedEntry link_prev, LinkedEntry link_next, LinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class LinkedEntry<K, V> {
		K key;
		V value;
		LinkedEntry<K, V> next;
		LinkedEntry<K, V> link_next, link_prev;

		protected LinkedEntry(K key, V value, LinkedEntry<K, V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new LinkedEntry<K, V>(key, value, (next == null ? null : (LinkedEntry) next.clone()));
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

		public boolean equals(Object o) {
			if (!(o instanceof LinkedEntry))
				return false;
			LinkedEntry e = (LinkedEntry) o;
			return CompareUtil.equals(key, e.key) && CompareUtil.equals(value, e.value);
		}

		public int hashCode() {
			return key.hashCode() ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}
	}
	public synchronized void sort(Comparator<LinkedEntry<K,V>> c) {
		ArrayList<LinkedEntry<K,V>> list = new ArrayList<LinkedEntry<K,V>>(this.size());
		Enumeration<LinkedEntry<K,V>> en = this.entries();
		while (en.hasMoreElements()) {
			LinkedEntry<K,V> n=en.nextElement();
			if(n!=null){
				list.add(n);
			}
		}
		Collections.sort(list, c);
		this.clear();
		for (int i = 0; i < list.size(); i++) {
			LinkedEntry<K,V> e = list.get(i);
			this.put(e.getKey(), e.getValue());
		}
	}
}
