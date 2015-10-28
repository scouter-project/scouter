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

package scouter.server.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import scouter.util.LongKeyLinkedMap;
import scala.collection.mutable
class TimedSeries[K, V] {

    private val table = new Hashtable[K, LongKeyLinkedMap[V]]();
    private val tableIndex = new Hashtable[K, Array[Long]]();
    var minTime = Long.MaxValue
    var maxTime = Long.MinValue
    var addend = true

    def add(key: K, time: Long, value: V) {
        var tree = table.get(key);
        if (tree == null) {
            tree = new LongKeyLinkedMap[V]();
            table.put(key, tree);
        }
        tree.put(time, value)

        maxTime = Math.max(time, maxTime);
        minTime = Math.min(time, minTime);

        addend = false
    }

    def getInTimeList(time: Long): List[V] = {
        return getInTimeList(time, Long.MaxValue - time);
    }

    def getInTimeList(time: Long, valid: Long): List[V] = {
        if (addend == false) {
            addEnd();
        }
        val out = new ArrayList[V]();
        EnumerScala.foreach(table.keys(),
            (key: K) => {
                val timeX = tableIndex.get(key);
                val n = getCeiling(timeX, time);
                if (n >= 0 && time + valid >= timeX(n)) {
                    val bt = table.get(key);
                    out.add(bt.get(timeX(n)))
                }
            });
        return out
    }

    def get(key: K, time: Long): V = {
        return getInTime(key, time, Long.MaxValue - time);
    }

    def getInTime(key: K, time: Long, valid: Long): V = {
        if (addend == false) {
            addEnd();
        }
        val timeX = tableIndex.get(key);
        val bt = table.get(key);
        val n = getCeiling(timeX, time);

        if (n >= 0 && time + valid >= timeX(n)) {
            bt.get(timeX(n))
        } else {
            null.asInstanceOf[V]
        }
    }

    private def getCeiling(timeEntries: Array[Long], time: Long): Int = {
        var n = Arrays.binarySearch(timeEntries, time);
        return if (n >= timeEntries.length) -1 else -n - 1
    }

    def getSeriesCount(): Int = {
        return table.size();
    }

    def getMinTime(): Long = {
        return minTime;
    }

    def getMaxTime(): Long = {
        return maxTime;
    }

    def addEnd() {
        tableIndex.clear();
        val en = table.keys();
        while (en.hasMoreElements()) {
            val key = en.nextElement();

            val bt = table.get(key);
            var n = bt.keyArray();
            n = Arrays.sort(n).asInstanceOf[Array[Long]];
            tableIndex.put(key, n);
        }
        addend = true;
    }
}