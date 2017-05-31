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
 * The initial code  from  "Guarded Suspension", Java 언어로 배우는 디자인 패턴 입문 - 멀티 쓰레드 편 저자 Yuki Hiroshi
 */
package scouter.util;


public class RequestQueue<V> {
    private java.util.LinkedList<V> queue;
    private int capacity;

    public RequestQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new java.util.LinkedList<V>();
    }

    public synchronized V get() {
        while (queue.size() <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        return queue.removeFirst();
    }

    public synchronized V getNoWait() {
        if (queue.size() > 0) {
            return queue.removeFirst();
        } else {
            return null;
        }
    }

    public synchronized V get(long timeout) {

        if (queue.size() > 0) {
            return queue.removeFirst();
        }
        long timeTo = System.currentTimeMillis() + timeout;
        long time = timeout;
        while (queue.isEmpty()) {
            try {
                if (time > 0)
                    wait(time);

            } catch (InterruptedException e) {
            }
            time = timeTo - System.currentTimeMillis();
            if (time <= 0) {
                break;
            }
        }
        if (queue.size() > 0) {
            return queue.removeFirst();
        }
        return null;
    }

    public synchronized boolean putForce(V o) {
        if (capacity <= 0 || queue.size() < capacity) {
            queue.add(o);
            notifyAll();
            return true;
        } else {
            while (queue.size() >= capacity) {
                queue.removeFirst();
            }
            queue.add(o);
            notifyAll();
            return false;
        }
    }

    public synchronized boolean put(V o) {
        if (capacity <= 0 || queue.size() < capacity) {
            queue.add(o);
            notifyAll();
            return true;
        } else {
            notify();
            return false;
        }
    }

    public synchronized boolean putNotifySingle(V o) {
        if (capacity <= 0 || queue.size() < capacity) {
            queue.add(o);
            notify();
            return true;
        } else {
            notify();
            return false;
        }
    }

    public synchronized void clear() {
        queue.clear();
    }

    public int size() {
        return queue.size();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void setCapacity(int size) {
        this.capacity = size;
    }
}