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
 */

package scouter.server.core.cache;

import java.util.ArrayList;
import java.util.List;

import scouter.util.IntSet;

/**
  * Circular queue that store objType, objHash and any data
  * This queue has an index and a loop count for access point.
  * @param capacity
  * @tparam V
  */
class LoopCache[V](capacity: Int) {

    var queue = new Array[Any](capacity);
    var objTypeTable = new Array[String](capacity);
    var objHashTable = new Array[Int](capacity);

    var index = 0;
    var loop = 0L;

    def put(objType: String, objHash: Int, record: V) {
        queue.synchronized {
            queue(index) = record
            objTypeTable(index) = objType
            objHashTable(index) = objHash
            index += 1
            if (index >= queue.length) {
                loop += 1;
                index = 0;
            }
        }
    }

    def getList(objType: String, start_loop: Long, start_index: Int): List[V] = {
        val end_index = this.index;
        val end_loop = this.loop;

        val buff = new ArrayList[V](queue.length);
        (end_loop - start_loop) match {
            case 0 =>
                if (start_index < end_index) {
                    copy(objType, buff, start_index, end_index);
                }
            case 1 =>
                if (start_index <= end_index) {
                    copy(objType, buff, end_index, queue.length);
                    copy(objType, buff, 0, end_index);
                } else {
                    copy(objType, buff, start_index, queue.length);
                    copy(objType, buff, 0, end_index);
                }
            case _ =>
                copy(objType, buff, end_index, queue.length);
                copy(objType, buff, 0, end_index);
        }
        return buff;
    }

    def copy(objType: String, buff: List[V], _from: Int, _to: Int) {
        for (i <- _from to _to - 1) {
            if (objType == null || objType.equals(objTypeTable(i))) {
                if (queue(i) != null) {
                    buff.add(queue(i).asInstanceOf[V]);
                }
            }
        }
    }

    def getList(objHashSet: IntSet, start_loop: Long, start_index: Int): List[V] = {
        var end_index = this.index;
        var end_loop = this.loop;

        var buff = new ArrayList[V](queue.length);
        (end_loop - start_loop) match {
            case 0 =>
                if (start_index < end_index) {
                    copy(objHashSet, buff, start_index, end_index);
                }
            case 1 =>
                if (start_index <= end_index) {
                    copy(objHashSet, buff, end_index, queue.length);
                    copy(objHashSet, buff, 0, end_index);
                } else {
                    copy(objHashSet, buff, start_index, queue.length);
                    copy(objHashSet, buff, 0, end_index);
                }
            case _ =>
                copy(objHashSet, buff, end_index, queue.length);
                copy(objHashSet, buff, 0, end_index);
        }
        return buff;
    }

    def copy(objHashSet: IntSet, buff: List[V], _from: Int, _to: Int) {
        for (i <- _from to _to - 1) {
            if (objHashSet == null || objHashSet.contains(objHashTable(i))) {
                if (queue(i) != null) {
                    buff.add(queue(i).asInstanceOf[V]);
                }
            }
        }
    }

    /**
      * get cache data by the specific objType after last index and last loop
      * @param objType
      * @param last_loop
      * @param last_index
      * @return
      */
    def get(objType: String, last_loop: Long, last_index: Int): CacheOut[V] = {
        val d = new CacheOut[V]();
        d.data = getList(objType, last_loop, last_index);
        d.loop = this.loop;
        d.index = this.index;
        return d;
    }

    /**
      * get cache data by objHash(list of IntSet) after last index and last loop
      * @param objHashSet
      * @param last_loop
      * @param last_index
      * @return
      */
    def get(objHashSet: IntSet, last_loop: Long, last_index: Int): CacheOut[V] = {
        val d = new CacheOut[V]();
        d.data = getList(objHashSet, last_loop, last_index);
        d.loop = this.loop;
        d.index = this.index;
        return d;
    }
}