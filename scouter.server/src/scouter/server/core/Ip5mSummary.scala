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
import scouter.io.DataInputX
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.db.SummaryWR
import scouter.util.DateUtil
import scouter.util.RequestQueue
import scouter.util.IntEnumer
import scouter.util.IntKeyLinkedMap
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scouter.util.ArrayUtil
import scouter.util.IntKeyMap

object Ip5mSummary {
    val TIME_INTERVAL = DateUtil.MILLIS_PER_FIVE_MINUTE

    class Data {
        var count = 0;
        var error_cnt = 0;
        var elapsed = 0L;
        var cputime = 0L;
    }

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);
    def add(p: XLogPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("IpSummary", 10, "queue exceeded!!");
        }
    }

    ThreadScala.startFixedRate(TIME_INTERVAL) { flush() }
    ThreadScala.startDaemon("IpSummary", { CoreRun.running }) {
        val p = queue.get();
        val ipHash = DataInputX.toInt(p.ipaddr, 0);

        var t2 = master.get(p.objHash);
        if (t2 == null) {
            t2 = new IntKeyLinkedMap[Data]().setMax(50000);
            master.put(p.objHash, t2);
        }
        if (ArrayUtil.len(p.ipaddr) == 4) {
            var d = t2.get(ipHash);
            if (d == null) {
                d = new Data();
                t2.put(ipHash, d);
            }
            d.count += 1;
            d.elapsed += p.elapsed;
            if (p.error != 0)
                d.error_cnt += 1;
            d.cputime += p.cpu;
        }
    }

    var master = new IntKeyMap[IntKeyLinkedMap[Data]]();

    def flush() {
        if (master.size == 0)
            return ;
        val table = master;
        master = new IntKeyMap[IntKeyLinkedMap[Data]]

        val stime = (System.currentTimeMillis() - 10000) / TIME_INTERVAL * TIME_INTERVAL;

        val itr = table.entries();
        while (itr.hasMoreElements) {
            val ent = itr.nextElement
            val p = new MapPack
            p.put("time", stime);
            p.put("objHash", ent.getKey);
            val ip = p.newList("ip");
            val count = p.newList("count");
            val errorCnt = p.newList("errorCnt");
            val elapsedSum = p.newList("elapsedSum");
            val cpuSum = p.newList("cpuSum");
            val itr2 = ent.getValue().keys();
            while (itr2.hasMoreElements) {
                val ipHash = itr2.nextInt
                val data = ent.getValue.get(ipHash);
                ip.add(ipHash);
                count.add(data.count);
                errorCnt.add(data.error_cnt);
                elapsedSum.add(data.elapsed);
                cpuSum.add(data.cputime);
            }
            SummaryWR.add(stime, SummaryEnum.IP, p);
        }
    }
}