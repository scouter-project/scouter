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

import scouter.lang.pack.XLogProfilePack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.db.XLogProfileWR
import scouter.server.db.XLogProfileWR
import scouter.server.plugin.PlugInManager
import scouter.server.util.ThreadScala
import scouter.util.BytesUtil
import scouter.util.RequestQueue

object ProfileCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[XLogProfilePack](conf.profile_queue_size);

    ThreadScala.startDaemon("scouter.server.core.ProfileCore", { CoreRun.running }) {
        val m = queue.get();
        ServerStat.put("profile.core.queue",queue.size());
        if (BytesUtil.getLength(m.profile) > 0) {
            PlugInManager.profile(m)
            //if (conf.xlog_profile_save_lower_bound_ms <= m.elapsed) {
            XLogProfileWR.add(m.time, m.txid, m.profile)
            //}
        }
    }

    def add(p: XLogProfilePack) {
        p.time = System.currentTimeMillis();

        val ok = queue.put(p)
        if (ok == false) {
            Logger.println("S110", 10, "queue exceeded!!");
        }
    }

}
