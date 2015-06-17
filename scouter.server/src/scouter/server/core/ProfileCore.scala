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

import scouter.lang.pack.XLogProfilePack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.db.XLogProfileWR
import scouter.util.BytesUtil
import scouter.util.RequestQueue
import scouter.util.ThreadUtil
import scouter.server.db.XLogProfileWR
import scouter.server.util.ThreadScala
import scouter.server.plugin.PlugXLogProfileBuf

object ProfileCore {

    val queue = new RequestQueue[XLogProfilePack](CoreRun.MAX_QUE_SIZE);
    val plugin = PlugXLogProfileBuf.getInstance();

    val conf = Configure.getInstance();
    ThreadScala.startDaemon("scouter.server.core.ProfileCore", { CoreRun.running }) {
        val m = queue.get();
        if (BytesUtil.getLength(m.profile) > 0) {
            plugin.add(m)
            if (conf.xlog_profile_save_time_limit <= m.elapsed) {
                XLogProfileWR.add(m.time, m.txid, m.profile)
            }
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
