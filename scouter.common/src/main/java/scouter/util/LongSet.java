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
import java.util.NoSuchElementException;
/**
 * @author Paul Kim (sjkim@whatap.io)
 */
public class LongSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongSetity table[];
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
		table = new LongSetity[initCapacity];
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
		LongSetity buk[] = table;
		int index = hash(value) % buk.length;
		for (LongSetity e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return true;
			}
		}
		return false;
	}
	public synchronized long get(long value) {
		LongSetity buk[] = table;
		int index = hash(value) % buk.length;
		for (LongSetity e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return e.value;
			}
		}
		return 0;
	}
	protected void rehash() {
		int oldCapacity = table.length;
		LongSetity oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongSetity newMap[] = new LongSetity[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongSetity old = oldMap[i];
			while (old != null) {
				LongSetity e = old;
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
		LongSetity buk[] = table;
		int index = hash(value) % buk.length;
		for (LongSetity e = buk[index]; e != null; e = e.next) {
			if (e.value == value) {
				return false;
			}
		}
		if (count >= threshold) {
			rehash();
			buk = table;
			index = hash(value) % buk.length;
		}
		LongSetity e = new LongSetity(value, buk[index]);
		buk[index] = e;
		count++;
		return true;
	}
	public synchronized long remove(long value) {
		LongSetity buk[] = table;
		int index = hash(value) % buk.length;
		for (LongSetity e = buk[index], prev = null; e != null; prev = e, e = e.next) {
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
		LongSetity buk[] = table;
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
	
	private class Enumer implements LongEnumer {
		LongSetity[] table = LongSet.this.table;
		int index = table.length;
		LongSetity entry = null;
		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];
			return entry != null;
		}
		public long nextLong() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				LongSetity e = entry;
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
	
	public  static class LongSetity {
		long value;
		LongSetity next;

		protected LongSetity(long value, LongSetity next) {
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new LongSetity(value, (next == null ? null : (LongSetity) next.clone()));
		}

		public long getKey() {
			return value;
		}

		public boolean equals(Object o) {
			if (!(o instanceof LongSetity))
				return false;
			LongSetity e = (LongSetity) o;
			return (value == e.getKey());
		}

		public int hashCode() {
			return (int) (value ^ (value >>> 32)) & Integer.MAX_VALUE;
		}

		public String toString() {
			return Long.toString(value);
		}
	}
}
