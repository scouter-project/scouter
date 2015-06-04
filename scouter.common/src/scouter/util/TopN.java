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
 */

package scouter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

abstract public class TopN<V> {
	public static class Entry<V> {
		public V k;

		public Entry(V k) {
			this.k = k;
		}

	}

	abstract public Order order(V o1, V o2);

	private V minValue;
	private int base = -1;
	private LinkedList<Entry<V>> table = new LinkedList();
	private int topN;

	public TopN(int max) {
		this.topN = max;
	}

	public void add(V k) {
		if (table.size() == 0) {
			table.add(new Entry(k));
			minValue = k;
			return;
		}
		Order ord = order(minValue, k);
		if (table.size() >= topN) {
			switch (ord) {
			case OK:
				return;
			}
		}
		switch (ord) {
		case OK:
		case EQUAL:
			if (table.size() < topN) {
				table.addLast(new Entry(k));
			}
			break;
		case REVERSE:
			boolean added = false;
			LinkedList.ENTRY<Entry<V>> en = table.getFirst();
			while (en != null) {
				if (order(k, en.item.k) != Order.REVERSE) {
					added = true;
					table.putBefore(new Entry(k), en);
					break;
				}
				en = en.next;
			}
			if (added == false && table.size() < topN) {
				table.addLast(new Entry(k));
				minValue = k;
			}
		}
		while (table.size() > topN) {
			Entry<V> e = table.removeLast();
			minValue = e.k;
		}
	}

	public List<V> getList() {
		List<V> out = new ArrayList();
		int i = 0;
		for (LinkedList.ENTRY<Entry<V>> x = (LinkedList.ENTRY<Entry<V>>) table
				.getFirst(); x != null; x = (LinkedList.ENTRY<Entry<V>>) table
				.getNext((LinkedList.ENTRY<Entry<V>>) x)) {
			out.add(x.item.k);
		}
		return out;
	}

	public int size() {
		return table.size();
	}

	public static void main(String[] args) {
		TopN<Integer> tn = new TopN<Integer>(10) {
			@Override
			public Order order(Integer o1, Integer o2) {
				return OrderUtil.asc(o1.intValue() , o2.intValue());
			}
		};
		Random r = new Random();
		long stime = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			tn.add(r.nextInt(1000));
		}
		long etime = System.currentTimeMillis();
		System.out.println((etime - stime) + " ms");
		System.out.println(tn.getList());

	}

	public void clear() {
		this.table.clear();
	}

}