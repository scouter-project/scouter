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
import scouter.lang.pack.SummaryPack
import scouter.server.Logger
import scouter.server.db.SummaryWR
import scouter.server.util.ThreadScala
import scouter.util.DateUtil
import scouter.util.RequestQueue
import scouter.server.plugin.PlugInManager
object SummaryCore {
    val TIME_INTERVAL = DateUtil.MILLIS_PER_FIVE_MINUTE;
    val queue = new RequestQueue[SummaryPack](CoreRun.MAX_QUE_SIZE);
    ThreadScala.startDaemon("SummaryCore") {
        while (CoreRun.running) {
            val p = queue.get();
            PlugInManager.summary(p);
            SummaryWR.add(p);
        }
    }
    def add(p: SummaryPack): Unit = {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("SummaryCore", 10, "queue exceeded!!");
        }
    }
}
