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

import java.util.{HashMap, Map}

import scouter.lang.{CounterKey, TimeTypeEnum}
import scouter.lang.value.Value
import scouter.server.util.ThreadScala
import scouter.util.{CacheTable, DateUtil, ThreadUtil}

/**
  * Singleton object of the memory cache for counter data.
  */
object CounterCache {
    //val counterEngine = CounterManager.getInstance().getCounterEngine();
    val cache = new CacheTable[CounterKey, Value]();

    ThreadScala.startDaemon("scouter.server.core.cache.CounterCache") {
        while (true) {
            ThreadUtil.sleep(5000);
            cache.clearExpiredItems();
            StatusCache.clearDirty();
        }
    }

    def put(key: CounterKey, value: Value) {
        var keepTime = getKeepTime(key.timetype);
        cache.put(key, value, keepTime);
    }

    def get(key: CounterKey): Value = {
        return cache.get(key);
    }

    def getObjectCounters(objHash: Int, timeType: Byte): Map[String, Value] = {
        val map = new HashMap[String, Value]();
        try {

            val en = cache.keys();
            while (en.hasMoreElements()) {
                val key = en.nextElement();
                if (key.timetype == timeType && key.objHash == objHash) {
                    val value = cache.get(key);
                    if (value != null) {
                        map.put(key.counter, value);
                    }
                }
            }
        } catch {
            case e: Throwable => println(e)
        }
        return map;
    }

    def getKeepTime(timeType: Byte): Long = {
        timeType match {
            case TimeTypeEnum.REALTIME =>
                return 10000;
            case TimeTypeEnum.ONE_MIN =>
                return DateUtil.MILLIS_PER_MINUTE + 3000;
            case TimeTypeEnum.FIVE_MIN =>
                return DateUtil.MILLIS_PER_MINUTE * 5 + 3000;
            case TimeTypeEnum.TEN_MIN =>
                return DateUtil.MILLIS_PER_MINUTE * 10 + 3000;
            case TimeTypeEnum.HOUR =>
                return DateUtil.MILLIS_PER_HOUR + 3000;
            case _ =>
                return 30 * 10000;
        }
    }

}
