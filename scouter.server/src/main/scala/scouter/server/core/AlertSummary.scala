/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.server.core;

import java.util.HashMap
import scala.collection.JavaConversions.asScalaSet
import scouter.lang.SummaryEnum
import scouter.lang.pack.AlertPack
import scouter.lang.pack.SummaryPack
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.DateUtil
import scouter.util.RequestQueue
import scouter.server.Configure

object AlertSummary {
    val conf =Configure.getInstance()
    val queue = new RequestQueue[AlertPack](CoreRun.MAX_QUE_SIZE);
    var master = new HashMap[Int, HashMap[String, (Byte, Int)]]()
    var typeMap = new HashMap[Int, String]()

    ThreadScala.startFixedRate(DateUtil.MILLIS_PER_FIVE_MINUTE) { doFlush() }

    ThreadScala.startDaemon("Alert5mSummary") {
        while (CoreRun.running) {
            val p = queue.get();
            var t1 = master.get(p.objHash);
            if (t1 == null) {
                t1 = new HashMap[String, (Byte, Int)]();
                master.put(p.objHash, t1);
                typeMap.put(p.objHash, p.objType);
            }
            var d1 = t1.get(p.title);
            if (d1 == null) {
                t1.put(p.title, (p.level, 1));
            } else {
                t1.put(p.title, (p.level, 1 + d1._2));
            }
        }
    }

    def add(p: AlertPack): Unit = {
        val ok = queue.put(p)
        if (ok == false) {
            Logger.println("S205", 10, "queue exceeded!!");
        }
    }

    def doFlush(): Unit = {
        if (master.size == 0)
            return ;
        val table = master;
        master = new HashMap[Int, HashMap[String, (Byte, Int)]]()

        val tm = DateUtil.MILLIS_PER_FIVE_MINUTE
        val stime = (System.currentTimeMillis() - 10000) / tm * tm
        for (ent <- table.entrySet()) {
            val sp = new SummaryPack();

            sp.time = stime;
            sp.objHash = ent.getKey();
            sp.objType = typeMap.get(sp.objHash);
            sp.stype = SummaryEnum.ALERT;

            val entSet = ent.getValue().entrySet();

            val countLv = sp.table.newList("count");
            val titleLv = sp.table.newList("title");
            val levelLv = sp.table.newList("level");

            var inx = 0;
            for (ent2 <- entSet) {
                titleLv.add(ent2.getKey());
                levelLv.add(ent2.getValue()._1);
                countLv.add(ent2.getValue()._2);
                inx += 1;
            }
            SummaryCore.add(sp);
        }
    }

}
