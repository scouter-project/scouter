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
 *  The initial idea for this class is from "org.apache.commons.lang.IntHashMap"; 
 *  http://commons.apache.org/commons-lang-2.6-src.zip
 *
 *
 */
package scouter.util;

import java.util.NoSuchElementException;

public class IntLinkedSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private ENTRY table[];
	private ENTRY header;

	private int count;
	private int threshold;
	private float loadFactor;

	public IntLinkedSet(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);

		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new ENTRY[initCapacity];

		this.header = new ENTRY(0, null);
		this.header.link_next = header.link_prev = header;

		threshold = (int) (initCapacity * loadFactor);
	}

	public IntLinkedSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public int[] getArray() {
		int[] _keys = new int[this.size()];
		IntEnumer en = this.elements();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextInt();
		return _keys;
	}

	public synchronized IntEnumer elements() {
		return new Enumer();
	}

	public synchronized boolean contains(int key) {
		ENTRY buk[] = table;
		int index = hash(key) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized int getFirst() {
		return this.header.link_next.key;
	}

	public synchronized int getLast() {
		return this.header.link_prev.key;
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
				old = old.next;

				int key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public IntLinkedSet setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public int put(int key) {
		return _put(key, MODE.LAST);
	}

	public int putLast(int key) {
		return _put(key, MODE.FORCE_LAST);
	}

	public int putFirst(int key) {
		return _put(key, MODE.FORCE_FIRST);
	}

	private synchronized int _put(int key, MODE m) {
		ENTRY buk[] = table;
		int index = hash(key) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
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
				return key;
			}
		}

		if (max > 0) {
			while (count >= max) {
				removeFirst();
			}
		}

		if (count >= threshold) {
			rehash();
			buk = table;
			index = hash(key) % buk.length;
		}

		ENTRY e = new ENTRY(key, buk[index]);
		buk[index] = e;

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
		return 0;
	}

	public synchronized int remove(int key) {
		ENTRY buk[] = table;
		int index = hash(key) % buk.length;
		for (ENTRY e = buk[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					buk[index] = e.next;
				}
				count--;
				//
				unchain(e);

				return key;
			}
		}
		return 0;
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
		ENTRY buk[] = table;
		for (int index = buk.length; --index >= 0;)
			buk[index] = null;

		this.header.link_next = header.link_prev = header;

		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		IntEnumer it = elements();

		buf.append("{");
		while (it.hasMoreElements()) {
			if (buf.length() > 1)
				buf.append(",");
			buf.append(it.nextInt());

		}
		buf.append("}");
		return buf.toString();
	}

	public static class ENTRY {
		int key;
		ENTRY next;
		ENTRY link_next, link_prev;

		protected ENTRY(int key, ENTRY next) {
			this.key = key;
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(key, (next == null ? null : (ENTRY) next.clone()));
		}

		public int getKey() {
			return key;
		}

		public boolean equals(Object o) {
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return CompareUtil.equals(e.key, key);
		}

		public int hashCode() {
			return key;
		}

		public String toString() {
			return Integer.toString(key);
		}
	}

	private class Enumer implements IntEnumer {

		ENTRY entry = IntLinkedSet.this.header.link_next;

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public int nextInt() {
			if (hasMoreElements()) {
				ENTRY e = entry;
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