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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TopN<V extends Comparable<V>> {
	
	private LinkedList<V> list = new LinkedList<V>();
	private int topN;
	public enum DIRECTION {
		ASC,
		DESC
	}
	private DIRECTION direction = DIRECTION.ASC;
	
	public TopN(int max) {
		this.topN = max;
	}
	
	public TopN(int max, DIRECTION direction) {
		this.topN = max;
		this.direction = direction;
	}

	public void add(V k) {
		if (list.size() == 0) {
			list.add(k);
			return;
		}
		
		int high = list.size() - 1;
		int low = 0;
		
		while (true) {
			int mid = (high + low) / 2;
			V v = list.get(mid);
			int compare = v.compareTo(k);
			if (compare == 0) {
				list.add(mid, k);
				break;
			} else {
				if (compare < 0) {
					low = mid;
				} else {
					high = mid;
				}
				if (high == low) {
					if (list.get(high).compareTo(k) > 0) {
						list.add(high, k);
					} else {
						list.add(high + 1, k);
					}
					break;
				} else if (high - low == 1) {
					if (list.get(low).compareTo(k) > 0) {
						list.add(low, k);
					} else if (list.get(high).compareTo(k) < 0) {
						list.add(high + 1, k);
					} else {
						list.add(high, k);
					}
					break;
				}
			}
		}
		while (list.size() > topN) {
			switch (direction) {
				case ASC:
					list.removeLast();
					break;
				case DESC:
					list.removeFirst();
					break;
			}
		}
	}

	public List<V> getList() {
		List<V> out = new ArrayList<V>();
		if (direction == DIRECTION.DESC) {
			for (int i = list.size() - 1; i >= 0; i--) {
				out.add(list.get(i));
			}
		} else {
			for (int i = 0; i < list.size(); i++) {
				out.add(list.get(i));
			}
		}
		return out;
	}

	public int size() {
		return list.size();
	}

	public static void main(String[] args) {
		long stime = System.currentTimeMillis();
		TopN<Integer> list = new TopN<Integer>(1000, TopN.DIRECTION.DESC);
		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		for (int i = 0; i < 100000; i++) {
			list.add(new Integer(r.nextInt(100000)));
		}
		System.out.println(list.size() + " : " + list.getList());
		long etime = System.currentTimeMillis();
		System.out.println((etime - stime) + " ms");

	}
}