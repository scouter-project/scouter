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
public class LinkedSet<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LinkedSetry<V> table[];
	private LinkedSetry<V> header;

	private int count;
	private int threshold;
	private float loadFactor;

	public LinkedSet(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LinkedSetry[initCapacity];

		this.header = new LinkedSetry(null, null);
		this.header.link_next = header.link_prev = header;

		threshold = (int) (initCapacity * loadFactor);
	}

	public LinkedSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public V[] getArray() {
		V[] _keys = (V[]) new Object[this.size()];
		Enumeration<V> en = this.elements();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextElement();
		return _keys;
	}

	public synchronized Enumeration<V> elements() {
		return new Enumer<V>();
	}

	public synchronized boolean contains(V key) {
		if (key == null)
			return false;
		LinkedSetry tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedSetry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V getFirst() {
		return this.header.link_next.key;
	}

	public synchronized V getLast() {
		return this.header.link_prev.key;
	}

	private int hash(V key) {
		return key.hashCode() & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LinkedSetry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LinkedSetry newMap[] = new LinkedSetry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			for (LinkedSetry<V> old = oldMap[i]; old != null;) {
				LinkedSetry<V> e = old;
				old = old.next;
				V key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LinkedSet<V> setMax(int max) {
		this.max = max;
		return this;
	}

	private enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public V put(V key) {
		return _put(key, MODE.LAST);
	}

	public V putLast(V key) {
		return _put(key, MODE.FORCE_LAST);
	}

	public V putFirst(V key) {
		return _put(key, MODE.FORCE_FIRST);
	}

	private synchronized V _put(V key, MODE m) {
		if (key == null)
			return null;
		LinkedSetry tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedSetry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
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
				return e.key;
			}
		}

		if (max > 0) {
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					V v = header.link_prev.key;
					remove(v);
					overflowed(v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					V v = header.link_next.key;
					remove(v);
					overflowed(v);
				}
				break;
			}
		}

		if (count >= threshold) {
			rehash();
			tab = table;
			index = hash(key) % tab.length;
		}
		LinkedSetry e = new LinkedSetry(key, tab[index]);
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
		return key;
	}

	protected void overflowed(V value) {		
	}

	public synchronized boolean remove(V key) {
		if (key == null)
			return false;

		LinkedSetry tab[] = table;
		int index = hash(key) % tab.length;
		for (LinkedSetry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				//
				unchain(e);

				return true;
			}
		}
		return false;
	}

	public synchronized boolean removeFirst() {
		if (isEmpty())
			return false;
		return remove(header.link_next.key);
	}

	public synchronized boolean removeLast() {
		if (isEmpty())
			return false;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		LinkedSetry tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;

		this.header.link_next = header.link_prev = header;

		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Enumeration<V> it = elements();
		buf.append("{");
		while (it.hasMoreElements()) {
			if (buf.length() > 1)
				buf.append(",");
			buf.append(it.nextElement());

		}
		buf.append("}");
		return buf.toString();
	}

	private class Enumer<V> implements Enumeration<V> {
		LinkedSetry<V> entry = (LinkedSetry<V>) LinkedSet.this.header.link_next;

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public V nextElement() {
			if (hasMoreElements()) {
				LinkedSetry<V> e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}

	}

	private void chain(LinkedSetry link_prev, LinkedSetry link_next, LinkedSetry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LinkedSetry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class LinkedSetry<V> {
		V key;
		LinkedSetry<V> next;
		LinkedSetry<V> link_next, link_prev;

		protected LinkedSetry(V key, LinkedSetry<V> next) {
			this.key = key;
			this.next = next;
		}

		protected Object clone() {
			return new LinkedSetry(key, (next == null ? null : (LinkedSetry) next.clone()));
		}

		public V getKey() {
			return key;
		}

		public boolean equals(Object o) {
			if (!(o instanceof LinkedSetry))
				return false;
			LinkedSetry e = (LinkedSetry) o;
			return CompareUtil.equals(e.key, key);
		}

		public int hashCode() {
			return key.hashCode();
		}

		public String toString() {
			return key.toString();
		}
	}
}
