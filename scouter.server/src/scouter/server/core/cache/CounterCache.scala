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
 *
 */

package scouter.server.core.cache;

import java.util.Enumeration
import java.util.HashMap
import java.util.Map
import scouter.server.CounterManager
import scouter.server.core.AgentManager
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.counters.CounterConstants
import scouter.lang.counters.CounterEngine
import scouter.lang.pack.ObjectPack
import scouter.lang.value.DecimalValue
import scouter.lang.value.Value
import scouter.util.CacheTable
import scouter.util.DateUtil
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala

object CounterCache {
    val counterEngine = CounterManager.getInstance().getCounterEngine();

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
        //        if (key.timetype == TimeTypeEnum.REALTIME) {
        //            try {
        //                val pack = AgentManager.getInstance().getAgent(key.objHash);
        //                if (pack != null) {
        //                    if (counterEngine.isChildOf(pack.objType, CounterConstants.FAMILY_DATABASE)) {
        //                        keepTime = DateUtil.MILLIS_PER_MINUTE;
        //                    }
        //                }
        //            } catch {
        //                case e: Throwable => e.printStackTrace()
        //            }
        //        }
        cache.put(key, value, keepTime);
    }

    def get(key: CounterKey): Value = {
        return cache.get(key);
    }

    def getObjectCounters(objHash: Int): Map[String, Value] = {
        val map = new HashMap[String, Value]();
        try {

            val en = cache.keys();
            while (en.hasMoreElements()) {
                val key = en.nextElement();
                if (key.objHash == objHash) {
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
