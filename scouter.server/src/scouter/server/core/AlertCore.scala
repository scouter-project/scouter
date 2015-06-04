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

import scouter.lang.pack.AlertPack
import scouter.lang.pack.TextPack
import scouter.server.Logger
import scouter.server.core.cache.AlertCache
import scouter.server.core.cache.TextCache
import scouter.server.db.AlertWR
import scouter.util.RequestQueue
import scouter.util.HashUtil
import scouter.server.util.ThreadScala
import scouter.server.tagcnt.AlertTagCount

object AlertCore {

    val queue: RequestQueue[AlertPack] = new RequestQueue(CoreRun.MAX_QUE_SIZE)

    ThreadScala.startDaemon("AlertCore", { CoreRun.running }) {
        val p = queue.get();
        p.time = System.currentTimeMillis()
        AlertCache.put(p)
        AlertWR.add(p)
        AlertTagCount.add(p)
    }

    def add(p: AlertPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("AlertCore", 10, "queue exceeded!!");
        }
    }
}