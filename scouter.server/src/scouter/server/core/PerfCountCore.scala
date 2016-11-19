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

;

import scouter.lang.counters.CounterConstants
import scouter.lang.value.DecimalValue
import scouter.lang.{CounterKey, TimeTypeEnum}
import scouter.lang.pack.PerfCounterPack
import scouter.server.Logger
import scouter.server.core.app.ObjectCpuChecker
import scouter.server.core.cache.CounterCache
import scouter.server.db.{DailyCounterWR, RealtimeCounterWR}
import scouter.server.plugin.PlugInManager
import scouter.server.plugin.alert.AlertEngine
import scouter.server.util.{EnumerScala, ThreadScala}
import scouter.util.{CastUtil, DateUtil, HashUtil, RequestQueue}

/**
  * request queue of performance counter data and also dispatcher of the queue
  */
object PerfCountCore {
    var queue = new RequestQueue[PerfCounterPack](CoreRun.MAX_QUE_SIZE);
    ThreadScala.startDaemon("scouter.server.core.PerfCountCore", {CoreRun.running}) {
        val counterPack = queue.get();
        val objHash = HashUtil.hash(counterPack.objName);

        PlugInManager.counter(counterPack);

        if (counterPack.timetype == TimeTypeEnum.REALTIME) {
            //counterPack.data.put(CounterConstants.COMMON_OBJHASH, new DecimalValue(objHash)) //add objHash into datafile
            //counterPack.data.put(CounterConstants.COMMON_TIME, new DecimalValue(counterPack.time)) //add objHash into datafile

            RealtimeCounterWR.add(counterPack);
            EnumerScala.foreach(counterPack.data.keySet().iterator(), (k: String) => {
                val value = counterPack.data.get(k);
                val counterKey = new CounterKey(objHash, k, counterPack.timetype);
                Auto5MSampling.add(counterKey, value);
                CounterCache.put(counterKey, value);
                AlertEngine.putRealTime(counterKey, value); //experimental
            })

            //cpu check and ask generating threaddump if the cpu threshold is exceeded
            ObjectCpuChecker.checkCpu(counterPack)

        } else {
            val yyyymmdd = CastUtil.cint(DateUtil.yyyymmdd(counterPack.time));
            val hhmm = CastUtil.cint(DateUtil.hhmm(counterPack.time));
            EnumerScala.foreach(counterPack.data.keySet().iterator(), (k: String) => {
                val value = counterPack.data.get(k);
                val counterKey = new CounterKey(objHash, k, counterPack.timetype);
                DailyCounterWR.add(yyyymmdd, counterKey, hhmm, value);
                //CounterCache.put(counterKey, value);
            })
        }
    }

    def add(p: PerfCounterPack) {
        val ok = queue.put(p);
        if (!ok) {
            Logger.println("S109", 10, "queue exceeded!!");
        }
    }
}
