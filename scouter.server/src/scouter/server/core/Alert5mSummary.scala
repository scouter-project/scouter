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

import scouter.lang.SummaryEnum
import scouter.lang.pack.AlertPack
import scouter.lang.pack.MapPack
import scouter.lang.value.ListValue
import scouter.server.Logger
import scouter.util.DateUtil
import scouter.util.Queue
import scouter.util.ThreadUtil
import java.util.{ Timer, TimerTask, Date, HashMap }
import scouter.util.RequestQueue
import scouter.server.db.SummaryWR
import scala.collection.JavaConversions._
import scouter.server.util.ThreadScala
import scouter.server.util.EnumerScala
import scouter.util.IntKeyMap

object Alert5mSummary {

    val queue = new RequestQueue[AlertPack](CoreRun.MAX_QUE_SIZE);
    var master = new IntKeyMap[HashMap[String, (Byte, Int)]]()

    ThreadScala.startFixedRate(DateUtil.MILLIS_PER_FIVE_MINUTE) { doFlush() }

    ThreadScala.startDaemon("Alert5mSummary") {
        val p = queue.get();
        var t1 = master.get(p.objHash);
        if (t1 == null) {
            t1 = new HashMap[String, (Byte, Int)]();
            master.put(p.objHash, t1);
        }
        var d1 = t1.get(p.title);
        t1.put(p.title, (p.level, if (d1 == null) 1 else 1 + d1._2));
    }

    def add(p: AlertPack) {
        val ok = queue.put(p)
        if (ok == false) {
            Logger.println("Alert5mSummary", 10, "queue exceeded!!");
        }
    }

    def doFlush() {
        if (master.size == 0)
            return ;
        val table = master;
        master = new IntKeyMap[HashMap[String, (Byte, Int)]]()

        val tm = DateUtil.MILLIS_PER_FIVE_MINUTE
        val stime = (System.currentTimeMillis() - 10000) / tm * tm

        val itr = table.keys()
        while (itr.hasMoreElements()) {
            val key = itr.nextInt()
            val p = new MapPack();
            p.put("time", stime);
            p.put("objHash", key);
            val titleLv = p.newList("title");
            val levelLv = p.newList("level");
            val count = p.newList("count");

            val itr2 = table.get(key).entrySet().iterator()
            while (itr2.hasNext) {
                val ent2 = itr2.next
                titleLv.add(ent2.getKey)
                levelLv.add(ent2.getValue._1)
                count.add(ent2.getValue._2)
            }
            SummaryWR.add(stime, SummaryEnum.ALERT, p);
        }
    }

}