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
public class IntSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private IntSetry table[];
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
		table = new IntSetry[initCapacity];
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
		IntSetry buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (IntSetry e = buk[index]; e != null; e = e.next) {
			if (e.key == key) {
				return true;
			}
		}
		return false;
	}
	public synchronized int get(int key) {
		IntSetry buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (IntSetry e = buk[index]; e != null; e = e.next) {
			if (e.key == key) {
				return e.key;
			}
		}
		return 0;
	}
	protected void rehash() {
		int oldCapacity = table.length;
		IntSetry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		IntSetry newMap[] = new IntSetry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			IntSetry old = oldMap[i];
			while (old != null) {
				IntSetry e = old;
				old = old.next;
				int index = (e.key & Integer.MAX_VALUE) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}
	public synchronized boolean add(int value) {
		IntSetry buk[] = table;
		int index = (value & Integer.MAX_VALUE) % buk.length;
		for (IntSetry e = buk[index]; e != null; e = e.next) {
			if (e.key == value) {
				return false;
			}
		}
		if (count >= threshold) {
			rehash();
			buk = table;
			index = (value & Integer.MAX_VALUE) % buk.length;
		}
		IntSetry e = new IntSetry(value, buk[index]);
		buk[index] = e;
		count++;
		return true;
	}
	public synchronized int remove(int key) {
		IntSetry buk[] = table;
		int index = (key & Integer.MAX_VALUE) % buk.length;
		for (IntSetry e = buk[index], prev = null; e != null; prev = e, e = e.next) {
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
		IntSetry buk[] = table;
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
	private static class IntSetry {
		int key;
		IntSetry next;
		protected IntSetry(int key, IntSetry next) {
			this.key = key;
			this.next = next;
		}
		protected Object clone() {
			return new IntSetry(key, (next == null ? null : (IntSetry) next.clone()));
		}
		public int getKey() {
			return key;
		}
		public boolean equals(Object o) {
			if (!(o instanceof IntSetry))
				return false;
			IntSetry e = (IntSetry) o;
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
		IntSetry[] table = IntSet.this.table;
		int index = table.length;
		IntSetry entry = null;
		IntSetry lastReturned = null;
		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];
			return entry != null;
		}
		public int nextInt() {
			while (entry == null && index > 0)
				entry = table[--index];
			if (entry != null) {
				IntSetry e = lastReturned = entry;
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
