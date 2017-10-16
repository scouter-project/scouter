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
public class StringIntLinkedMap {
	private static final int DEFAULT_CAPACITY = 101;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private StringIntLinkedEntry table[];
	private StringIntLinkedEntry header;
	private int count;
	private int threshold;
	private float loadFactor;

	public StringIntLinkedMap(int initCapacity, float loadFactor) {
		if (initCapacity < 0)
			throw new RuntimeException("Capacity Error: " + initCapacity);
		if (loadFactor <= 0)
			throw new RuntimeException("Load Count Error: " + loadFactor);
		if (initCapacity == 0)
			initCapacity = 1;
		this.loadFactor = loadFactor;
		this.table = new StringIntLinkedEntry[initCapacity];
		this.header = new StringIntLinkedEntry(null, 0, null);
		this.header.link_next = header.link_prev = header;
		threshold = (int) (initCapacity * loadFactor);
	}

	public StringIntLinkedMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	private int NONE = 0;

	public StringIntLinkedMap setNullValue(int none) {
		this.NONE = none;
		return this;
	}

	public int size() {
		return count;
	}

	public String[] keyArray() {
		String[] _keys = new String[this.size()];
		StringEnumer en = this.keys();
		for (int i = 0; i < _keys.length; i++)
			_keys[i] = en.nextString();
		return _keys;
	}

	public synchronized StringEnumer keys() {
		return new Enumer(TYPE.KEYS);
	}

	public synchronized IntEnumer values() {
		return new Enumer(TYPE.VALUES);
	}

	public synchronized Enumeration<StringIntLinkedEntry> entries() {
		return new Enumer(TYPE.ENTRIES);
	}

	public synchronized boolean containsValue(int value) {
		StringIntLinkedEntry tab[] = table;
		int i = tab.length;
		while (i-- > 0) {
			for (StringIntLinkedEntry e = tab[i]; e != null; e = e.next) {
				if (e.value == value) {
					return true;
				}
			}
		}
		return false;
	}

	public synchronized boolean containsKey(String key) {
		StringIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringIntLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return true;
			}
		}
		return false;
	}

	public synchronized int get(String key) {
		if (key == null)
			return NONE;
		StringIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringIntLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				return e.value;
			}
		}
		return NONE;
	}

	public synchronized int getFirsValue() {
		if (isEmpty())
			return NONE;
		return this.header.link_next.value;
	}

	public synchronized int getLastValue() {
		if (isEmpty())
			return NONE;
		return this.header.link_prev.value;
	}

	private int hash(String key) {
		return key.hashCode() & Integer.MAX_VALUE;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		StringIntLinkedEntry oldMap[] = table;
		int newCapacity = oldCapacity * 2 + 1;
		StringIntLinkedEntry newMap[] = new StringIntLinkedEntry[newCapacity];
		threshold = (int) (newCapacity * loadFactor);
		table = newMap;
		for (int i = oldCapacity; i-- > 0;) {
			StringIntLinkedEntry old = oldMap[i];
			while (old != null) {
				StringIntLinkedEntry e = old;
				old = old.next;
				String key = e.key;
				int index = hash(key) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	private int max;

	public StringIntLinkedMap setMax(int max) {
		this.max = max;
		return this;
	}

	private static enum MODE {
		FORCE_FIRST, FORCE_LAST, FIRST, LAST
	};

	public int put(String key, int value) {
		return _put(key, value, MODE.LAST);
	}

	public int putLast(String key, int value) {
		return _put(key, value, MODE.FORCE_LAST);
	}

	public int putFirst(String key, int value) {
		return _put(key, value, MODE.FORCE_FIRST);
	}

	public int add(String key, int value) {
		return _add(key, value, MODE.LAST);
	}

	public int addLast(String key, int value) {
		return _add(key, value, MODE.FORCE_LAST);
	}

	public int addFirst(String key, int value) {
		return _add(key, value, MODE.FORCE_FIRST);
	}

	private synchronized int _put(String key, int value, MODE m) {
		if (key == null)
			return NONE;
		StringIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringIntLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				int old = e.value;
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
					String k = header.link_prev.key;
					long v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					// removeFirst();
					String k = header.link_next.key;
					long v = remove(k);
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
		StringIntLinkedEntry e = new StringIntLinkedEntry(key, value, tab[index]);
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
		return NONE;
	}

	public void overflowed(String key, long value) {
	}

	private synchronized int _add(String key, int value, MODE m) {
		if (key == null)
			return NONE;
		StringIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringIntLinkedEntry e = tab[index]; e != null; e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				int old = e.value;
				e.value += value;
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
					String k = header.link_prev.key;
					long v = remove(k);
					overflowed(k, v);
				}
				break;
			case FORCE_LAST:
			case LAST:
				while (count >= max) {
					// removeFirst();
					String k = header.link_next.key;
					long v = remove(k);
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
		StringIntLinkedEntry e = new StringIntLinkedEntry(key, value, tab[index]);
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
		return NONE;
	}

	public synchronized int remove(String key) {
		if (key == null)
			return NONE;
		StringIntLinkedEntry tab[] = table;
		int index = hash(key) % tab.length;
		for (StringIntLinkedEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (CompareUtil.equals(e.key, key)) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				int oldValue = e.value;
				e.value = 0;
				//
				unchain(e);
				return oldValue;
			}
		}
		return NONE;
	}

	public synchronized int removeFirst() {
		if (isEmpty())
			return NONE;
		return remove(header.link_next.key);
	}

	public synchronized int removeLast() {
		if (isEmpty())
			return NONE;
		return remove(header.link_prev.key);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized void clear() {
		StringIntLinkedEntry tab[] = table;
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
			StringIntLinkedEntry e = (StringIntLinkedEntry) (it.nextElement());
			if (i > 0)
				buf.append(", ");
			buf.append(e.getKey() + "=" + e.getValue());
		}
		buf.append("}");
		return buf.toString();
	}

	public String toFormatString() {
		StringBuffer buf = new StringBuffer();
		Enumeration it = entries();
		buf.append("{\n");
		while (it.hasMoreElements()) {
			StringIntLinkedEntry e = (StringIntLinkedEntry) it.nextElement();
			buf.append("\t").append(e.getKey() + "=" + e.getValue()).append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	public static class StringIntLinkedEntry {
		String key;
		int value;
		StringIntLinkedEntry next;
		StringIntLinkedEntry link_next, link_prev;

		protected StringIntLinkedEntry(String key, int value, StringIntLinkedEntry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		protected Object clone() {
			return new StringIntLinkedEntry(key, value, (next == null ? null : (StringIntLinkedEntry) next.clone()));
		}

		public String getKey() {
			return key;
		}

		public int getValue() {
			return value;
		}

		public int setValue(int value) {
			int oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof StringIntLinkedEntry))
				return false;
			StringIntLinkedEntry e = (StringIntLinkedEntry) o;
			return CompareUtil.equals(e.key, key) && CompareUtil.equals(e.value, value);
		}

		public int hashCode() {
			return key.hashCode() ^ value;
		}

		public String toString() {
			return key + "=" + value;
		}
	}

	private enum TYPE {
		KEYS, VALUES, ENTRIES
	}

	private class Enumer implements Enumeration, StringEnumer, IntEnumer {
		TYPE type;
		StringIntLinkedEntry entry = StringIntLinkedMap.this.header.link_next;

		Enumer(TYPE type) {
			this.type = type;
		}

		public boolean hasMoreElements() {
			return header != entry && entry != null;
		}

		public Object nextElement() {
			if (hasMoreElements()) {
				StringIntLinkedEntry e = entry;
				entry = e.link_next;
				switch (type) {
				case KEYS:
					return e.key;
				case VALUES:
					return e.value;
				default:
					return e;
				}
			}
			throw new NoSuchElementException("no more next");
		}

		public int nextInt() {
			if (hasMoreElements()) {
				StringIntLinkedEntry e = entry;
				entry = e.link_next;
				return e.value;
			}
			throw new NoSuchElementException("no more next");
		}

		public String nextString() {
			if (hasMoreElements()) {
				StringIntLinkedEntry e = entry;
				entry = e.link_next;
				return e.key;
			}
			throw new NoSuchElementException("no more next");
		}
	}

	private void chain(StringIntLinkedEntry link_prev, StringIntLinkedEntry link_next, StringIntLinkedEntry e) {
		e.link_prev = link_prev;
		e.link_next = link_next;
		link_prev.link_next = e;
		link_next.link_prev = e;
	}

	private void unchain(StringIntLinkedEntry e) {
		e.link_prev.link_next = e.link_next;
		e.link_next.link_prev = e.link_prev;
		e.link_prev = null;
		e.link_next = null;
	}
}
