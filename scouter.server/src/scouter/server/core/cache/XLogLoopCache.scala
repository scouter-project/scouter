/*
*  Copyright 2015 the original author or authors.
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
 */

package scouter.server.core.cache;

import java.util.ArrayList;
import java.util.List;

import scouter.util.IntSet;

class XLogLoopCache[V](capacity: Int) {

    val queue = new Array[Any](capacity);
    val objHashTable = new Array[Int](capacity);
    val elapsed = new Array[Int](capacity);
    val error = new Array[Boolean](capacity);

    var index = 0;
    var loop = 0L;

    def put(objHash: Int, time: Int, err: Boolean, record: V) {
        queue.synchronized {
            queue(index) = record;
            objHashTable(index) = objHash;
            elapsed(index) = time;
            error(index) = err;
            index += 1;
            if (index >= queue.length) {
                loop += 1;
                index = 0;
            }
        }
    }

    def getList(start_loop: Long, start_index: Int, time: Int): List[V] = {
        val end_index = this.index;
        val end_loop = this.loop;

        val buff = new ArrayList[V](queue.length);
        (end_loop - start_loop) match {
            case 0 =>
                if (start_index < end_index) {
                    copy(buff, start_index, end_index, time);
                }
            case 1 =>
                if (start_index <= end_index) {
                    copy(buff, end_index, queue.length, time);
                    copy(buff, 0, end_index, time);
                } else {
                    copy(buff, start_index, queue.length, time);
                    copy(buff, 0, end_index, time);
                }
            case _ =>
                copy(buff, end_index, queue.length, time);
                copy(buff, 0, end_index, time);
        }
        return buff;
    }

    def copy(buff: List[V], _from: Int, _to: Int, time: Int) {
        var i = _from
        while (i < _to) {
            if (queue(i) != null && (elapsed(i) >= time || error(i) == true)) {
                buff.add(queue(i).asInstanceOf[V]);
            }
            i += 1
        }
    }

    def getList(objHashSet: IntSet, start_loop: Long, start_index: Int, time: Int): List[V] = {
        val end_index = this.index;
        val end_loop = this.loop;

        val buff = new ArrayList[V](queue.length);
        (end_loop - start_loop) match {
            case 0 =>
                if (start_index < end_index) {
                    copy(objHashSet, buff, start_index, end_index, time);
                }
            case 1 =>
                if (start_index <= end_index) {
                    copy(objHashSet, buff, end_index, queue.length, time);
                    copy(objHashSet, buff, 0, end_index, time);
                } else {
                    copy(objHashSet, buff, start_index, queue.length, time);
                    copy(objHashSet, buff, 0, end_index, time);
                }
            case _ =>
                copy(objHashSet, buff, end_index, queue.length, time);
                copy(objHashSet, buff, 0, end_index, time);
        }
        return buff;
    }

    private def copy(objHashSet: IntSet, buff: List[V], _from: Int, _to: Int, time: Int) {
        var i = _from
        while (i < _to) {
            if (objHashSet == null || objHashSet.contains(objHashTable(i))) {
                if (queue(i) != null && (elapsed(i) >= time || error(i) == true)) {
                    buff.add(queue(i).asInstanceOf[V]);
                }
            }
            i += 1
        }
    }

    def get(last_loop: Long, last_index: Int, time: Int): CacheOut[V] = {
        val d = new CacheOut[V]();
        d.data = getList(last_loop, last_index, time);
        d.loop = this.loop;
        d.index = this.index;
        return d;
    }

    def get(objHashSet: IntSet, last_loop: Long, last_index: Int, time: Int): CacheOut[V] = {
        val d = new CacheOut[V]();
        d.data = getList(objHashSet, last_loop, last_index, time);
        d.loop = this.loop;
        d.index = this.index;
        return d;
    }
}