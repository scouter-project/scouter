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
package scouter.server.core;
import scouter.lang.pack.AlertPack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.cache.AlertCache
import scouter.server.db.AlertWR
import scouter.server.plugin.PlugInManager
import scouter.server.tagcnt.AlertTagCount
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
object AlertCore {
    val queue: RequestQueue[AlertPack] = new RequestQueue(CoreRun.MAX_QUE_SIZE)
    val conf = Configure.getInstance();
    ThreadScala.startDaemon("scouter.server.core.AlertCore", { CoreRun.running }) {
        val p = queue.get();
        ServerStat.put("alert.core.queue", queue.size());
        p.time = System.currentTimeMillis()
        if (Configure.WORKABLE) {
          PlugInManager.alert(p)
        }
        AlertSummary.add(p)
        AlertCache.put(p)
        AlertWR.add(p)
        if (conf.tagcnt_enabled) {
            AlertTagCount.add(p)
        }
    }
    def add(p: AlertPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S106", 10, "queue exceeded!!");
        }
    }
}
