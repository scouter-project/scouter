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
import scouter.lang.counters.CounterConstants
import scouter.lang.counters.CounterEngine
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.net.RequestCmd
import scouter.server.netio.AgentCall
import scouter.server.netio.AgentCall
import scouter.util.DateUtil
import scouter.util.ThreadUtil
import scouter.server.{ Logger, CounterManager }
import scouter.server.util.ThreadScala
import scouter.server.util.EnumerScala
/*
 * 날짜가 바뀌면 해야할 것들...
 */
object TextCacheReset {
    val engine = scouter.server.CounterManager.getInstance().getCounterEngine()
    var oldunit = 0L;
    ThreadScala.startDaemon("scouter.server.core.TextCacheReset", { CoreRun.running }, 2000) {
        var dateUnit = DateUtil.getDateUnit();
        if (dateUnit != oldunit) {
            oldunit = dateUnit;
            EnumerScala.foreach(AgentManager.getLiveObjHashList().iterator(), (oid: Int) => {
                try {
                    var agent = AgentManager.getAgent(oid);
                    AgentCall.call(agent, RequestCmd.OBJECT_RESET_CACHE, null);
                } catch {
                    case t: Throwable => Logger.println("S114", "Thread \n" + t)
                }
            })
        }
    }
}
