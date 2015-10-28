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
 *
 */
package scouter.util;

import java.util.NoSuchElementException;

public class LongSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private ENTRY table[];
	private int count;
	private int threshold;
	private float loadFactor;

	public LongSet(int initCapacity, float loadFactor) {
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

	public LongSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public int size() {
		return count;
	}

	public synchronized LongEnumer values() {
		return new Enumer();
	}

	public synchronized boolean contains(long value) {
		ENTRY buk[] = table;
		int index = hash(value) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return true;
			}
		}
		return false;
	}

	public synchronized long get(long value) {
		ENTRY buk[] = table;
		int index = hash(value) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return e.value;
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

				int index = hash(e.value) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	protected static int hash(long value) {
		return (int) (value ^ (value >>> 32)) & Integer.MAX_VALUE;
	}

	public synchronized boolean add(long value) {
		ENTRY buk[] = table;
		int index = hash(value) % buk.length;
		for (ENTRY e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return false;
			}
		}

		if (count >= threshold) {
			rehash();

			buk = table;
			index = hash(value) % buk.length;
		}

		ENTRY e = new ENTRY(value, buk[index]);
		buk[index] = e;
		count++;
		return true;
	}

	public synchronized long remove(long value) {
		ENTRY buk[] = table;
		int index = hash(value) % buk.length;
		for (ENTRY e = buk[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.value == value) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					buk[index] = e.next;
				}
				count--;

				return value;
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
		LongEnumer it = this.values();
		buf.append("{");
		for (int i = 0; i <= max; i++) {
			long value = it.nextLong();
			buf.append(value);
			if (i < max)
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}
	
	public final static LongEnumer emptyEnumer = new LongEnumer() {
		public long nextLong() {
			return 0;
		}
		public boolean hasMoreElements() {
			return false;
		}
	};


	private static class ENTRY {
		long value;
		ENTRY next;

		protected ENTRY(long value, ENTRY next) {
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(value, (next == null ? null : (ENTRY) next.clone()));
		}

		public long getKey() {
			return value;
		}

		public boolean equals(Object o) {
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return (value == e.getKey());
		}

		public int hashCode() {
			return hash(value);
		}

		public String toString() {
			return Long.toString(value);
		}
	}

	private class Enumer implements LongEnumer {
		ENTRY[] table = LongSet.this.table;
		int index = table.length;
		ENTRY entry = null;

		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];

			return entry != null;
		}

		public long nextLong() {
			while (entry == null && index > 0)
				entry = table[--index];

			if (entry != null) {
				ENTRY e = entry;
				entry = e.next;
				return e.value;

			}
			throw new NoSuchElementException("no more next");
		}

	}

	public long[] toArray() {
		long[] _values = new long[this.size()];
		LongEnumer en = this.values();
		for (int i = 0; i < _values.length; i++)
			_values[i] = en.nextLong();
		return _values;
	}
}