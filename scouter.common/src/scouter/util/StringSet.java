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
 * 
 *  The initial idea for this class is from "org.apache.commons.lang.IntHashMap"; 
 *  http://commons.apache.org/commons-lang-2.6-src.zip
 *
 */
package scouter.util;

import java.util.HashSet;
import java.util.NoSuchElementException;

public class StringSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private transient ENTRY table[];
	private transient int count;
	private int threshold;
	private float loadFactor;

	public StringSet(int initCapacity, float loadFactor) {
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

	public StringSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public StringSet(String[] arr) {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
		if (arr == null)
			return;
		for (int i = 0; i < arr.length; i++) {
			this.put(arr[i]);
		}
	}

	public int size() {
		return count;
	}

	public synchronized StringEnumer keys() {
		return new Enumer();
	}

	public synchronized boolean hasKey(String key) {
		if (key == null)
			return false;
		ENTRY tab[] = table;
		int hash = key.hashCode();
		int index = (hash & Integer.MAX_VALUE) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.next) {
			if ((e.hash == hash) && CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
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

				int index = (e.hash & Integer.MAX_VALUE) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	public String put(String key) {
		return unipoint(key);
	}

	public synchronized String unipoint(String key) {
		if (key == null)
			return null;

		ENTRY tab[] = table;
		int hash = key.hashCode();
		int index = (hash & Integer.MAX_VALUE) % tab.length;
		for (ENTRY e = tab[index]; e != null; e = e.next) {
			if ((e.hash == hash) && CompareUtil.equals(e.key, key)) {
				return e.key;
			}
		}

		if (count >= threshold) {
			rehash();
			tab = table;
			index = (hash & Integer.MAX_VALUE) % tab.length;
		}

		ENTRY e = new ENTRY(hash, key, tab[index]);
		tab[index] = e;
		count++;
		return key;
	}

	public synchronized boolean remove(String key) {
		if (key == null)
			return false;
		ENTRY tab[] = table;
		int hash = key.hashCode();
		int index = (hash & Integer.MAX_VALUE) % tab.length;
		for (ENTRY e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if ((e.hash == hash) && CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				return true;
			}
		}
		return false;
	}

	public synchronized void clear() {
		ENTRY tab[] = table;
		for (int index = tab.length; --index >= 0;)
			tab[index] = null;
		count = 0;
	}

	public synchronized String toString() {
		int max = size() - 1;
		StringBuffer buf = new StringBuffer();
		StringEnumer it = keys();

		buf.append("{");
		for (int i = 0; i <= max; i++) {
			buf.append(it.nextString());
			if (i < max)
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}

	private static class ENTRY {
		int hash;
		String key;
		ENTRY next;

		protected ENTRY(int hash, String key, ENTRY next) {
			this.hash = hash;
			this.key = key;
			this.next = next;
		}

		protected Object clone() {
			return new ENTRY(hash, key, (next == null ? null : (ENTRY) next.clone()));
		}

		public String getKey() {
			return key;
		}

		public boolean equals(Object o) {
			if (!(o instanceof ENTRY))
				return false;
			ENTRY e = (ENTRY) o;
			return CompareUtil.equals(e.key, key);
		}

		public int hashCode() {
			return hash;
		}

		public String toString() {
			return key.toString();
		}
	}

	public static StringEnumer emptyEnumer = new StringEnumer() {

		@Override
		public String nextString() {
			return null;
		}

		@Override
		public boolean hasMoreElements() {
			return false;
		}
	};

	private class Enumer implements StringEnumer {
		ENTRY[] table = StringSet.this.table;
		int index = table.length;
		ENTRY entry = null;

		Enumer() {
		}

		public boolean hasMoreElements() {
			while (entry == null && index > 0)
				entry = table[--index];

			return entry != null;
		}

		public String nextString() {
			while (entry == null && index > 0)
				entry = table[--index];

			if (entry != null) {
				ENTRY e = entry;
				entry = e.next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	public static void main(String[] args) {
		HashSet<String> st = new HashSet();
		st.add("sss1");
		st.add("sss2");
		st.add("sss3");
		System.out.println(st);
	}
}