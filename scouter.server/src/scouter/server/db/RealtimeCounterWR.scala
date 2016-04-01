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
package scouter.server.db;

import scouter.lang.counters.CounterConstants
import scouter.lang.pack.PerfCounterPack
import scouter.lang.value.DecimalValue
import scouter.server.Logger
import scouter.server.db.counter.RealtimeCounterDBHelper
import scouter.server.plugin.PlugInManager
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.RequestQueue
import scouter.util.HashUtil
import scouter.util.IClose
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala
object RealtimeCounterWR {
    val queue = new RequestQueue[PerfCounterPack](DBCtr.MAX_QUE_SIZE);
    ThreadScala.start("scouter.server.db.RealtimeCounterWR") {
        val last_logtime = System.currentTimeMillis();
        var dBHelper: RealtimeCounterDBHelper = null
        while (DBCtr.running) {
            val pack = queue.get()
            try {
                if (dBHelper == null) {
                    dBHelper = writeOpen(pack)
                } else if (dBHelper.currentDateUnit != DateUtil.getDateUnit(pack.time)) {
                    dBHelper.close()
                    dBHelper = writeOpen(pack)
                }
                val objHash = HashUtil.hash(pack.objName)
                dBHelper.activeTime = System.currentTimeMillis()
                dBHelper.counterDbHeader.intern(pack.data.keySet())

                val counterBytes = RealtimeCounterDBHelper.getTagBytes(dBHelper.counterDbHeader.getTagStrInt(), pack.data)
                val dataOffset = dBHelper.counterData.write(counterBytes)
                dBHelper.counterIndex.write(objHash, pack.time, dataOffset)

            } catch {
                case t: Throwable => Logger.println("S133", 10, t.toString())
            }
        }
        FileUtil.close(dBHelper);
    }
    def addWait(p: PerfCounterPack, max: Int) {
        while (queue.size() >= max) {
            ThreadUtil.sleep(100);
        }
        add(p);
    }
    def add(p: PerfCounterPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S134", 10, "queue exceeded!!");
        }
    }

    def writeOpen(pack: PerfCounterPack): RealtimeCounterDBHelper = {
        val db = new RealtimeCounterDBHelper().open(DateUtil.yyyymmdd(pack.time), false);
        db.currentDateUnit = DateUtil.getDateUnit(pack.time);
        return db;
    }
}
