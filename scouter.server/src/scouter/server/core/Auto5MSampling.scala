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

package scouter.server.core;

import java.util.Date
import java.util.HashMap
import java.util.Iterator
import java.util.Map
import java.util.Timer
import java.util.TimerTask
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.value.Value
import scouter.lang.value.ValueEnum
import scouter.server.Configure
import scouter.server.core.cache.CounterCache
import scouter.server.db.DailyCounterWR
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.LinkedSet;
import scouter.server.util.ThreadScala

object Auto5MSampling {
  ThreadScala.startFixedRate(DateUtil.MILLIS_PER_FIVE_MINUTE) {
    if (Configure.getInstance().auto_5m_sampling == true) {
      val work = map;
      map = new HashMap[CounterKey, Value]();

      val itr = work.keySet().iterator();
      while (itr.hasNext() == true) {
        val key = itr.next();
        val key5m = new CounterKey(key.objHash, key.counter, FIVE_MIN_CODE);
        if (map5m.contains(key5m) == false) {
          // System.out.println("AUTO 5M " +key);
          val value = work.get(key);

          val now = System.currentTimeMillis();
          DailyCounterWR.add(CastUtil.cint(DateUtil.yyyymmdd(now)), key5m,
            CastUtil.cint(DateUtil.hhmm(now)), value);

          CounterCache.put(key5m, value);
        }
      }
    }
  }

  var map = new HashMap[CounterKey, Value]();
  val map5m = new LinkedSet[CounterKey]().setMax(1000);

  def add(key: CounterKey, value: Value) {

   key.timetype match {
      case TimeTypeEnum.REALTIME =>
        value.getValueType() match {
          case ValueEnum.BOOLEAN => map.put(key, value);
          case ValueEnum.FLOAT => map.put(key, value);
          case ValueEnum.DOUBLE => map.put(key, value);
          case ValueEnum.DECIMAL => map.put(key, value);
          case _ => 
        }
      case TimeTypeEnum.FIVE_MIN =>
        map5m.put(key)
      case _ =>
    }
  }

  val FIVE_MIN_CODE = TimeTypeEnum.FIVE_MIN

}