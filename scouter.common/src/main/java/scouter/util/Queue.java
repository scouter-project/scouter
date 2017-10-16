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

public class Queue<V> {

	private V[] queue;
	private int head = 0;
	private int tail = 0;
	private int count = 0;

	public Queue(int capacity) {
		this.queue = (V[]) new Object[capacity + 1];
	}

	public synchronized V push(V item) {
		queue[head] = item;
		inchead();
		if (head == tail) {
			inctail();
			return null;
		} else {
			count++;
			return item;
		}
	}

	public synchronized V pop() {
		if (head != tail) {
			V o = queue[tail];
			queue[tail] = null;
			inctail();
			count--;
			return o;
		}
		return null;
	}

	public synchronized V[] pop(int n) {
		Object[] out = new Object[n];
		for (int i = 0; i < n && head != tail; i++) {
			out[i] = queue[tail];
			queue[tail] = null;
			inctail();
			count--;
		}
		return (V[]) out;
	}

	public synchronized V[] popAll() {
		int n = count;
		Object[] out = new Object[n];
		for (int i = 0; i < n && head != tail; i++) {
			out[i] = queue[tail];
			queue[tail] = null;
			inctail();
			count--;
		}
		return (V[]) out;
	}

	public synchronized void clear() {
			head = tail = count = 0;
	}

	public int size() {
		return count;
	}

	public V enqueue(V item) {
		return push(item);
	}

	public V dequeue() {
		return pop();
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public boolean isFull() {
		return count >= queue.length - 1;
	}

	private void inchead() {
		head++;
		if (head >= queue.length) {
			head = 0;
		}
	}

	private void inctail() {
		tail++;
		if (tail >= queue.length) {
			tail = 0;
		}
	}

	public static void main(String[] args) {
		Integer v;
		Queue<Integer> q = new Queue<Integer>(4);
		q.push(1);
		System.out.println("full ? " + q.isFull());
		q.push(2);
		System.out.println("full ? " + q.isFull());
		q.push(2);
		System.out.println("full ? " + q.isFull());
		q.push(2);
		System.out.println("full ? " + q.isFull());
		q.push(2);
		System.out.println("full ? " + q.isFull());

		print("out = ", q.pop(q.size()));
		print("out = ", q.pop(q.size()));
		print("out = ", q.pop(q.size()));

	}

	private static void print(String msg, Object[] pop) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pop.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(pop[i]);
		}
		System.out.println(msg + sb);
	}

}