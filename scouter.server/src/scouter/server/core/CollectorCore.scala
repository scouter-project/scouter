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

package scouter.server.core

import java.util.ArrayList
import scouter.lang.counters.CounterConstants
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.CounterManager
import scouter.util.DateUtil
import scouter.server.util.ThreadScala
import scouter.server.Logger
import scouter.server.netio.AgentCall
import scala.collection.JavaConversions._
import scouter.server.util.EnumerScala
object CollectorCore {

    val conf = Configure.getInstance()

    var lastUnit = DateUtil.getMinUnit(System.currentTimeMillis()) / 5
    ThreadScala.startDaemon("CollectorCore", { CoreRun.running }, 1000) {
        val unit = DateUtil.getMinUnit(System.currentTimeMillis()) / 5
        if (unit != lastUnit) {
            if (conf.stat_pull_enabled) {
                try {
                    process();
                } catch {
                    case e: Throwable => println("CollectorCore : " + e);
                }
            }
        }
        lastUnit = unit
    }

    def process() {

        val engine = CounterManager.getInstance().getCounterEngine();
        val handler = (din: DataInputX, dout: DataOutputX) => {
            while (TcpFlag.HasNEXT == din.readByte()) {
                val p = din.readPack().asInstanceOf[MapPack];
                MapPackCore.add(p);
            }
        }
        EnumerScala.foreach(AgentManager.getObjPacks(), (o: ObjectPack) => {
            if (o.alive && o.address != null && o.address.length() >= 0) {
                if (engine.isChildOf(o.objType, CounterConstants.FAMILY_JAVAEE)) {
                    AgentCall.call(o, RequestCmd.STAT_SERVICE, null, handler)
                }
            }
        })
    }
}