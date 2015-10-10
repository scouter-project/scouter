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
package scouter.server.core;
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.PerfCounterPack
import scouter.server.Logger
import scouter.server.core.cache.CounterCache
import scouter.server.db.DailyCounterWR
import scouter.server.db.RealtimeCounterWR
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.HashUtil
import scouter.util.RequestQueue
import scala.collection.JavaConversions._
import scouter.server.util.ThreadScala
import scouter.server.util.EnumerScala
import scouter.server.plugin.PlugInManager
object PerfCountCore {
    var queue = new RequestQueue[PerfCounterPack](CoreRun.MAX_QUE_SIZE);
    ThreadScala.startDaemon("scouter.server.core.PerfCountCore", { CoreRun.running }) {
        val p = queue.get();
        val objHash = HashUtil.hash(p.objName);
        
        //PLUGIN CONTER
        PlugInManager.counter(p);
        
        if (p.timetype == TimeTypeEnum.REALTIME) {
            RealtimeCounterWR.add(p);
            EnumerScala.foreach(p.data.keySet().iterator(), (k: String) => {
                val value = p.data.get(k);
                val key = new CounterKey(objHash, k, p.timetype);
                Auto5MSampling.add(key, value);
                CounterCache.put(key, value);
            })
        } else {
            val yyyymmdd = CastUtil.cint(DateUtil.yyyymmdd(p.time));
            val hhmm = CastUtil.cint(DateUtil.hhmm(p.time));
            EnumerScala.foreach(p.data.keySet().iterator(), (k: String) => {
                val value = p.data.get(k);
                val key = new CounterKey(objHash, k, p.timetype);
                DailyCounterWR.add(yyyymmdd, key, hhmm, value);
                CounterCache.put(key, value);
            })
        }
    }
    def add(p: PerfCounterPack) {
        if (p.time == 0) {
            p.time = System.currentTimeMillis();
        }
        if (p.timetype == 0) {
            p.timetype = TimeTypeEnum.REALTIME;
        }
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S109", 10, "queue exceeded!!");
        }
    }
}
