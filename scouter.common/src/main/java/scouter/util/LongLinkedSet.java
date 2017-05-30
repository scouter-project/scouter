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

import java.util.Arrays;
import java.util.NoSuchElementException;

public class LongLinkedSet {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private LongLinkedSetry table[];
	private LongLinkedSetry header;
	private int count;
	private int threshold;
	private float loadFactor;

	public LongLinkedSet(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new LongLinkedSetry[initCapacity];
		this.header = new LongLinkedSetry(0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public LongLinkedSet() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	private long NONE = 0;

	public LongLinkedSet setNullValue(long none) {
		this.NONE = none;
		return this;
	}

	public int size() {
		return count;
	}

	public long[] getArray() {
		long[] _keys = new long[this.size()];
		LongEnumer en = this.elements();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextLong();
		return _keys;
	}

	public synchronized LongEnumer elements() {
		return new Enumer();
	}

	public synchronized boolean contains(long key) {
		LongLinkedSetry buk[] = table;
		int index = hash(key) % buk.length;
		for (LongLinkedSetry e = buk[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized long getFirst() {
		return this.header.link_next.key;
	}

	public synchronized long getLast() {
		return this.header.link_prev.key;
	}

	private int hash(long key) {
		return ((int) key) & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		LongLinkedSetry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		LongLinkedSetry newMap[] = new LongLinkedSetry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			LongLinkedSetry old = oldMap[i];
			while (old != null) {
				LongLinkedSetry e = old;
				old = old.next;
				long key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public LongLinkedSet setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public long put(long key) {
		return _put(key, MODE.LAST);
	}

	public long putLast(long key) {
		return _put(key, MODE.FORCE_LAST);
	}

	public long putFirst(long key) {
		return _put(key, MODE.FORCE_FIRST);
	}

	private synchronized long _put(long key, MODE m) {
		LongLinkedSetry buk[] = table;
		int index = hash(key) % buk.length;
		for (LongLinkedSetry e = buk[index]; e != null; e = e.next) {
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
			switch (m) {
			case FORCE_FIRST:
			case FIRST:
				while (count >= max) {
					// removeLast();
					long v = header.link_prev.key;
					remove(v);
					overflowed(v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					// removeFirst();
					long v = header.link_next.key;
					remove(v);
					overflowed(v);
				}
				break;
			}
		}
		if (count >= threshold) {
			rehash();
			buk = table;
			index = hash(key) % buk.length;
		}
		LongLinkedSetry e = new LongLinkedSetry(key, buk[index]);
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
		return NONE;
	}

	protected void overflowed(long value) {
	}

	public synchronized long remove(long key) {
		LongLinkedSetry buk[] = table;
		int index = hash(key) % buk.length;
		for (LongLinkedSetry e = buk[index], prev = null; e != null; prev = e, e = e.next) {
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
		LongLinkedSetry buk[] = table;
		for (int index = buk.length; --index >= 0;)
			buk[index] = null;
		this.header.link_next = header.link_prev = header;
		count = 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		LongEnumer it = elements();
		buf.append("{");
		while (it.hasMoreElements()) {
			if (buf.length() > 1)
				buf.append(",");
			buf.append(it.nextLong());
		}
		buf.append("}");
		return buf.toString();
	}

	private class Enumer implements LongEnumer {
		LongLinkedSetry entry = LongLinkedSet.this.header.link_next;

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public long nextLong() {
			if (hasMoreElements()) {
				LongLinkedSetry e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(LongLinkedSetry link_prev, LongLinkedSetry link_next, LongLinkedSetry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(LongLinkedSetry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}

	public static class LongLinkedSetry {
		long key;
		LongLinkedSetry next;
		LongLinkedSetry link_next, link_prev;

		protected LongLinkedSetry(long key, LongLinkedSetry next) {
			this.key = key;
			this.next = next;
		}

		protected Object clone() {
			return new LongLinkedSetry(key, (next == null ? null : (LongLinkedSetry) next.clone()));
		}

		public long getKey() {
			return key;
		}

		public boolean equals(Object o) {
			if (!(o instanceof LongLinkedSetry))
				return false;
			LongLinkedSetry e = (LongLinkedSetry) o;
			return CompareUtil.equals(e.key, key);
		}

		public int hashCode() {
			return (int) key;
		}

		public String toString() {
			return Long.toString(key);
		}
	}
	public synchronized void sort(boolean asc) {
		if(this.size() <=1)
			return;
		long[] list = new long[this.size()];
		LongEnumer en = this.elements();
		for (int i=0; en.hasMoreElements();i++) {
			list[i]=en.nextLong();
		}
		Arrays.sort(list);
		this.clear();
		
		if(asc){
			for(long n : list)
				this.put(n);
		}else{
			for(int i=list.length-1 ; i>=0; i--){
				this.put(list[i]);
			}
		}
	}
	public static void main(String[] args) {
		LongLinkedSet s = new LongLinkedSet();
		s.put(10);
		s.put(20);
		System.out.println(s);
		s.sort(false);
		System.out.println(s);
	}

}
