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

public class IntSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private ENTRY table[];
	private int count;
	private int threshold;
	private float loadFactor;

	public IntSet(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);

		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		table = new ENTRY[initCapacity];
		threshold = (int) (initCapacity * loadFactor);
	}

	public IntSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized IntEnumer values() {
		return new Enumer();
	}

	public synchronized boolean contains(int key) {
		ENTRY buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}

	public synchronized int get(int key) {
		ENTRY buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.key;
			}
		}
		return 0;
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

				int index = (e.key & Integer.MAX_VALUE) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	public synchronized boolean add(int value) {
		ENTRY buk[] = table;
		int index = (value & Integer.MAX_VALUE) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.key == value) {
				return false;
			}
		}

		if (count >= threshold) {
			rehash();

			buk = table;
			index = (value & Integer.MAX_VALUE) % buk.length;
		}

		ENTRY e = new ENTRY(value, buk[index]);
		buk[index] = e;
		count++;
		return true;
	}

	public synchronized int remove(int key) {
		ENTRY buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (ENTRY e = buk[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.key == key) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					buk[index] = e.next;
				}
				count--;

				return key;
			}
		}
		return 0;
	}

	public synchronized void clear() {
		ENTRY buk[] = table;
		for (int index = buk.length; --index >= 0;)
			buk[index] = null;
		count = 0;
	}

	public synchronized String toString() {
		int max = size() - 1;
		StringBuffer buf = new StringBuffer();
		IntEnumer it = this.values();
		buf.append("{");
		for (int i = 0; i <= max; i++) {
			int key = it.nextInt();
			buf.append(key);
			if (i < max)
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}

	public static IntEnumer emptyEnumer = new IntEnumer() {
		public int nextInt() {
			return 0;
		}

		public boolean hasMoreElements() {
			return false;
		}
	};

	private static class ENTRY {
		int key;
		ENTRY next;

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
			return (key == e.getKey());
		}

		public int hashCode() {
			return key;
		}

		public String toString() {
			return Integer.toString(key);
		}
	}

	private class Enumer implements IntEnumer {
		ENTRY[] table = IntSet.this.table;
		int index = table.length;
		ENTRY entry = null;
		ENTRY lastReturned = null;

		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];

			return entry != null;
		}

		public int nextInt() {
			while (entry == null && index > 0)
				entry = table[--index];

			if (entry != null) {
				ENTRY e = lastReturned = entry;
				entry = e.next;
				return e.key;

			}
			throw new NoSuchElementException("no more next");
		}

	}

	public int[] toArray() {
		int[] _keys = new int[this.size()];
		IntEnumer en = this.values();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextInt();
		return _keys;
	}
}