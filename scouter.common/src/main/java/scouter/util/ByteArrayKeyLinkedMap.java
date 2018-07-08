/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * @author Gun Lee (gunlee01@gmail.com)
 *
 */
public class ByteArrayKeyLinkedMap<V> {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private ByteArrayKeyLinkedEntry<V> table[];
	private ByteArrayKeyLinkedEntry<V> header;
	private int count;
	private int threshold;
	private float loadFactor;

	public ByteArrayKeyLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new ByteArrayKeyLinkedEntry[initCapacity];
		this.header = new ByteArrayKeyLinkedEntry(new byte[0], null, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public ByteArrayKeyLinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public byte[][] keyArray() {
		byte[][] _keys = new byte[this.size()][];
		ByteArrayEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextKey();
		return _keys;
	}

	public synchronized ByteArrayEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized Enumeration<V> values() {
		return new Enumer<V>(TYPE.VALUES);
	}

	public synchronized Enumeration<ByteArrayKeyLinkedEntry<V>> entries() {
		return new Enumer<ByteArrayKeyLinkedEntry>(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(Object value) {
		if (value == null) {
			throw new NullPointerException();
		}
		ByteArrayKeyLinkedEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (ByteArrayKeyLinkedEntry e = tab[i]; e != null; e = e.next) {
				if (CompareUtil.equals(e.value, value)) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(byte[] key) {
		ByteArrayKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized V get(byte[] key) {
		ByteArrayKeyLinkedEntry<V> tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return null;
	}

	public synchronized byte[] getFirstKey() {
		return this.header.link_next.key;
	}

	public synchronized byte[] getLastKey() {
		return this.header.link_prev.key;
	}

	public synchronized V getFirstValue() {
		return this.header.link_next.value;
	}

	public synchronized V getLastValue() {
		return this.header.link_prev.value;
	}

	protected void overflowed(byte[] key, V value) {
	}

	protected V create(byte[] key) {
		throw new RuntimeException("not implemented create()");
	}

	public V intern(byte[] key) {
		return _intern(key, MODE.LAST);
	}

	private synchronized V _intern(byte[] key, MODE m) {
		ByteArrayKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
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
					byte[] k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					byte[] k = header.link_next.key;
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
		ByteArrayKeyLinkedEntry<V> e = new ByteArrayKeyLinkedEntry<V>(key, value, tab[index]);
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

	private int hash(byte[] key) {
		return Arrays.hashCode(key) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		ByteArrayKeyLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		ByteArrayKeyLinkedEntry newMap[] = new ByteArrayKeyLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			ByteArrayKeyLinkedEntry<V> old = oldMap[i];
			while (old != null) {
				ByteArrayKeyLinkedEntry<V> e = old;
				old = old.next;
				byte[] key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public ByteArrayKeyLinkedMap<V> setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public V put(byte[] key, V value) {
		return _put(key, value, MODE.LAST);
	}

	public V putLast(byte[] key, V value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public V putFirst(byte[] key, V value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	private synchronized V _put(byte[] key, V value, MODE m) {
		ByteArrayKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyLinkedEntry<V> e = tab[index]; e != null; e = e.next) {
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
					// removeLast();
					byte[] k = header.link_prev.key;
					V v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					// removeFirst();
					byte[] k = header.link_next.key;
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
		ByteArrayKeyLinkedEntry<V> e = new ByteArrayKeyLinkedEntry<V>(key, value, tab[index]);
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

	public synchronized V remove(byte[] key) {
		ByteArrayKeyLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (ByteArrayKeyLinkedEntry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
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
		ByteArrayKeyLinkedEntry tab[] = table;
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
			ByteArrayKeyLinkedEntry e = (ByteArrayKeyLinkedEntry) (it.nextElement());
			if (i > 0)
				buf.append(", ");
			buf.append(Arrays.toString(e.getKey()) + "=" + e.getValue());
		}
		buf.append("}");
		return buf.toString();
	}

	public String toFormatString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{\n");
		while (it.hasMoreElements()) {
			ByteArrayKeyLinkedEntry e = (ByteArrayKeyLinkedEntry) it.nextElement();
			buf.append("\t").append(Arrays.toString(e.getKey()) + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer<V> implements Enumeration, ByteArrayEnumer {
		TYPE type;
		ByteArrayKeyLinkedEntry entry = ByteArrayKeyLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public V nextElement() {
			if (hasMoreElements()) {
				ByteArrayKeyLinkedEntry e = entry;
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

		public byte[] nextKey() {
			if (hasMoreElements()) {
				ByteArrayKeyLinkedEntry e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(ByteArrayKeyLinkedEntry link_prev, ByteArrayKeyLinkedEntry link_next, ByteArrayKeyLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(ByteArrayKeyLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class ByteArrayKeyLinkedEntry<V> {
		byte[] key;
		V value;
		ByteArrayKeyLinkedEntry<V> next;
		ByteArrayKeyLinkedEntry<V> link_next, link_prev;

		protected ByteArrayKeyLinkedEntry(byte[] key, V value, ByteArrayKeyLinkedEntry<V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new ByteArrayKeyLinkedEntry<V>(key, value, (next == null ? null : (ByteArrayKeyLinkedEntry<V>) next.clone()));
		}

		public byte[] getKey() {
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
			if (!(o instanceof ByteArrayKeyLinkedMap.ByteArrayKeyLinkedEntry))
				return false;
			ByteArrayKeyLinkedEntry e = (ByteArrayKeyLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return Arrays.hashCode(key) ^ (value == null ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	public synchronized void sort(Comparator<ByteArrayKeyLinkedEntry<V>> c) {
		ArrayList<ByteArrayKeyLinkedEntry<V>> list = new ArrayList<ByteArrayKeyLinkedEntry<V>>(this.size());
		Enumeration<ByteArrayKeyLinkedEntry<V>> en = this.entries();
		while (en.hasMoreElements()) {
			list.add(en.nextElement());
		}
		Collections.sort(list, c);
		this.clear();
		for (int i = 0; i < list.size(); i++) {
			ByteArrayKeyLinkedEntry<V> e = list.get(i);
			this.put(e.getKey(), e.getValue());
		}
	}

	public static void main(String[] args) {
		ByteArrayKeyLinkedMap<Integer> m = new ByteArrayKeyLinkedMap<Integer>();
		for (int i = 0; i < 10; i++) {
			byte[] b = new byte[1];
			b[0] = new Byte(String.valueOf(i));
			m.put(b, i);
		}
		System.out.println(m);
		m.sort(new Comparator<ByteArrayKeyLinkedEntry<Integer>>() {
			@Override
			public int compare(ByteArrayKeyLinkedEntry<Integer> o1, ByteArrayKeyLinkedEntry<Integer> o2) {
				return CompareUtil.compareTo(o2.getKey(), o1.getKey());
			}
		});
		System.out.println(m);
	}

}
