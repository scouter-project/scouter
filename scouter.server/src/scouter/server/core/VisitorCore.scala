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

import scouter.lang.pack.XLogPack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
import scouter.util.StringKeyLinkedMap
import scouter.server.util.cardinality.HyperLogLog
import scouter.util.DateUtil
import scouter.util.ThreadUtil
import scouter.util.IntKeyLinkedMap
import scouter.server.db.DBCtr
import java.io.File
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.Hexa32
import scouter.server.util.EnumerScala
import scouter.server.db.VisitorDB

object VisitorCore {

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("scouter.server.core.VisitorCore") {
        val conf = Configure.getInstance();
        while (CoreRun.running) {
            val m = queue.get();
            try {
                val objInfo = AgentManager.getAgent(m.objHash)
                if (objInfo != null) {
                    process(objInfo.objType, m)
                }
            } catch {
                case e: Exception =>
                    Logger.println("S199", 10, "VisitDay", e)
            }
        }
    }

    def add(p: XLogPack) {
        if (p.userid == 0) {
            return
        }
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S200", 10, "VisitDay queue exceeded!!");
        }
    }
    def process(objType: String, x: XLogPack) {
        VisitorDB.getNewObjType(objType).offer(x.userid)
        VisitorDB.getNewObject(x.objHash).offer(x.userid)
    }
}
  
