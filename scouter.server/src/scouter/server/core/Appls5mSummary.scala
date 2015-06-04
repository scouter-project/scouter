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
import scouter.lang.SummaryEnum
import scouter.lang.pack.MapPack
import scouter.lang.pack.XLogPack
import scouter.lang.value.ListValue
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.db.SummaryWR
import scouter.util.DateUtil
import scouter.util.RequestQueue
import scouter.util.ThreadUtil
import scouter.lang.value.DecimalValue
import scala.collection.JavaConversions._
import scouter.server.util.ThreadScala
import scouter.util.IntKeyMap
import scouter.server.ConfObserver

object Appls5mSummary {

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);
    var master = new IntKeyMap[IntKeyMap[Data]]();

    var TIME_INTERVAL = Configure.getInstance().appstat_interval
    ThreadScala.startFixedRate(TIME_INTERVAL) { flush() }

    ConfObserver.put(this.getClass().getName()) {
        TIME_INTERVAL = Configure.getInstance().appstat_interval;
    }

    class Data {
        var count = 0;
        var error_cnt = 0;
        var elapsed = 0L;
        var cputime = 0L;
    }

    ThreadScala.startDaemon("Appls5mSummary", { CoreRun.running }) {
        val p = queue.get();

        var t2 = master.get(p.objHash);
        if (t2 == null) {
            t2 = new IntKeyMap[Data]();
            master.put(p.objHash, t2);
        }
        var d = t2.get(p.service);
        if (d == null) {
            d = new Data();
            t2.put(p.service, d);
        }
        d.count += 1;
        d.elapsed += p.elapsed;
        if (p.error != 0)
            d.error_cnt += 1;
        d.cputime += p.cpu;
    }

    def add(p: XLogPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("ApplsHourSummary", 10, "queue exceeded!!");
        }
    }

    def flush() {
        if (master.size() == 0)
            return ;
        val table = master;
        master = new IntKeyMap[IntKeyMap[Data]]();

        val stime = (System.currentTimeMillis() - 10000) / TIME_INTERVAL * TIME_INTERVAL;

        val itr = table.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextInt()
            val value = table.get(key)
            val p = new MapPack()
            p.put("time", stime);
            p.put("objHash", key);
            val service = p.newList("service");
            val count = p.newList("count");
            val errorCnt = p.newList("errorCnt");
            val elapsedSum = p.newList("elapsedSum");
            val cpuSum = p.newList("cpuSum");
            val itr2 = value.keys();
            while (itr2.hasMoreElements()) {
                val key2 = itr2.nextInt()
                val value2 = value.get(key2)
                service.add(key2);
                count.add(value2.count);
                errorCnt.add(value2.error_cnt);
                elapsedSum.add(value2.elapsed);
                cpuSum.add(value2.cputime);
            }
            SummaryWR.add(stime, SummaryEnum.APP, p);
        }
    }
}