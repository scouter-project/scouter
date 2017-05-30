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

package scouter.server.core

import java.util
import scouter.lang.value.{Value, ValueEnum}
import scouter.lang.{CounterKey, TimeTypeEnum}
import scouter.server.Configure
import scouter.server.core.cache.CounterCache
import scouter.server.db.DailyCounterWR
import scouter.server.util.ThreadScala
import scouter.util.{CastUtil, DateUtil, LinkedSet}

/**
  * peak 1 sample data from real time counter data and make it as 5 minute summary data.
  */
object Auto5MSampling {

    val FIVE_MIN_CODE = TimeTypeEnum.FIVE_MIN

    var counterMap = new util.HashMap[CounterKey, Value]()
    val counterMap5m = new LinkedSet[CounterKey]().setMax(1000)

    //peak 1 sample data from real time counter data and make it as 5 minute summary data.
    ThreadScala.startFixedRate(DateUtil.MILLIS_PER_FIVE_MINUTE) {
        if (Configure.getInstance()._auto_5m_sampling) {
            val workMap = counterMap
            counterMap = new util.HashMap[CounterKey, Value]()
            val itr = workMap.keySet().iterator()

            while (itr.hasNext()) {
                val key = itr.next()
                val key5m = new CounterKey(key.objHash, key.counter, FIVE_MIN_CODE)
                if (!counterMap5m.contains(key5m)) {
                    // System.out.println("AUTO 5M " +key);
                    val value = workMap.get(key);

                    val now = System.currentTimeMillis()
                    DailyCounterWR.add(CastUtil.cint(DateUtil.yyyymmdd(now)), key5m,
                        CastUtil.cint(DateUtil.hhmm(now)), value)

                    //CounterCache.put(key5m, value)
                }
            }
        }
    }


    def add(key: CounterKey, value: Value) {
        key.timetype match {
            case TimeTypeEnum.REALTIME =>
                value.getValueType() match {
                    case ValueEnum.BOOLEAN => counterMap.put(key, value);
                    case ValueEnum.FLOAT => counterMap.put(key, value);
                    case ValueEnum.DOUBLE => counterMap.put(key, value);
                    case ValueEnum.DECIMAL => counterMap.put(key, value);
                    case _ =>
                }
            case TimeTypeEnum.FIVE_MIN =>
                counterMap5m.put(key)
            case _ =>
        }
    }
}