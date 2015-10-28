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
 */

package scouter.util;


public class LinkedList<E> {
	transient int size = 0;
	transient ENTRY<E> first;
	transient ENTRY<E> last;

	public void addFirst(E e) {
		final ENTRY<E> f = first;
		final ENTRY<E> newNode = new ENTRY(null, e, f);
		first = newNode;
		if (f == null)
			last = newNode;
		else
			f.prev = newNode;
		size++;
	}

	public void addLast(E e) {
		final ENTRY<E> l = last;
		final ENTRY<E> newNode = new ENTRY(l, e, null);
		last = newNode;
		if (l == null)
			first = newNode;
		else
			l.next = newNode;
		size++;
	}

	public ENTRY<E> putBefore(E e, ENTRY<E> succ) {
		final ENTRY<E> pred = succ.prev;
		final ENTRY<E> newNode = new ENTRY(pred, e, succ);
		succ.prev = newNode;
		if (pred == null)
			first = newNode;
		else
			pred.next = newNode;
		size++;
		return newNode;
	}

	public E remove(ENTRY<E> x) {
		final E element = x.item;
		final ENTRY<E> next = x.next;
		final ENTRY<E> prev = x.prev;

		if (prev == null) {
			first = next;
		} else {
			prev.next = next;
		}
		if (next == null) {
			last = prev;
		} else {
			next.prev = prev;
		}
		size--;
		return element;
	}

	public ENTRY<E> getFirst() {
		return first;
	}

	public ENTRY<E> getLast() {
		return last;
	}

	public ENTRY<E> getNext(ENTRY<E> o) {
		return o.next;
	}

	public E removeFirst() {
		final ENTRY<E> f = first;
		if (f != null)
			return remove(first);
		return null;
	}

	public E removeLast() {
		final ENTRY<E> l = last;
		if (l != null)
			return remove(l);
		return null;
	}

	public int size() {
		return size;
	}

	public boolean add(E e) {
		addLast(e);
		return true;
	}

	public void clear() {
		for (ENTRY<E> x = first; x != null;) {
			ENTRY<E> next = x.next;
			x.item = null;
			x.next = null;
			x.prev = null;
			x = next;
		}
		first = last = null;
		size = 0;
	}

	public static class ENTRY<E> {
		public E item;
		public ENTRY<E> next;
		public ENTRY<E> prev;

		ENTRY(ENTRY<E> prev, E element, ENTRY<E> next) {
			this.item = element;
			this.next = next;
			this.prev = prev;
		}
	}

	public E[] toArray() {
		E[] result =(E[]) new Object[size];
		int i = 0;
		for (ENTRY<E> x = first; x != null; x = x.next)
			result[i++] = x.item;
		return result;
	}


}