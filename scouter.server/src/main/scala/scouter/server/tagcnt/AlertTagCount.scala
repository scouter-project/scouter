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

package scouter.server.tagcnt;

import scouter.lang.pack.AlertPack
import scouter.lang.value.NullValue
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.AgentManager
import scouter.server.core.CoreRun
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
import scouter.lang.value.TextHashValue
import scouter.lang.value.DecimalValue
import scouter.lang.value.TextValue

object AlertTagCount {

    val queue = new RequestQueue[AlertPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("scouter.server.tagcnt.AlertTagCount") {
        val conf = Configure.getInstance();
        while (CoreRun.running) {
            val m = queue.get();
            try {
                val objInfo = AgentManager.getAgent(m.objHash)
                if (objInfo != null) {
                    process(objInfo.objType, m)
                }
            } catch {
                case e: Exception => Logger.println("S180", e.toString())
            }
        }
    }

    def add(p: AlertPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S181", 10, "AlertTagCount queue exceeded!!");
        }
    }
    def process(objType: String, x: AlertPack) {
        TagCountProxy.add(x.time, objType, TagCountConfig.alert.total, NullValue.value, 1)
        TagCountProxy.add(x.time, objType, TagCountConfig.alert.objectName, new TextHashValue(x.objHash), 1)
        TagCountProxy.add(x.time, objType, TagCountConfig.alert.level, new DecimalValue(x.level), 1)
        TagCountProxy.add(x.time, objType, TagCountConfig.alert.title, new TextValue(x.title), 1)
    }
}
