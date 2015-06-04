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

import scouter.lang.pack.MapPack
import scouter.server.Logger
import scouter.server.db.SummaryRD
import scouter.server.db.SummaryWR
import scouter.util.DateUtil
import scouter.util.RequestQueue
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala
/**
 * will be removed
 * @deprecated
 */
object MapPackCore {

    val TIME_INTERVAL = DateUtil.MILLIS_PER_FIVE_MINUTE;

    val queue = new RequestQueue[MapPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("MapPackCore", { CoreRun.running }) {
        val p = queue.get();
        if ("stat".equals(p.getText("_pack_"))) {
            val _type = p.getInt("type").toByte;
            if (_type != 0) {
                val time = (System.currentTimeMillis() - 10000) / TIME_INTERVAL * TIME_INTERVAL;
                SummaryWR.add(time, _type, p);
            }
        }
    }

    def add(p: MapPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("MapPackCore", 10, "queue exceeded!!");
        }
    }
}