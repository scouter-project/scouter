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
package scouter.server.term.handler

import java.util.List
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.term.AnsiPrint
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala
import scouter.util.DateUtil
import scouter.util.FormatUtil
import scouter.util.StringUtil
import scala.collection.JavaConversions._
import scouter.util.IntSet
import scouter.server.core.cache.XLogCache
import scouter.io.DataInputX
import scouter.lang.pack.XLogPack
import scouter.util.CastUtil
import scouter.server.db.VisitorDB
import scouter.lang.ObjectType
object VISITOR {

    def process(cmd: String): Unit = {

        if (cmd == null)
            return
        val cmdFirstToken = StringUtil.firstWord(cmd, " ")
        if (AgentManager.isObjType(cmdFirstToken)) {
            val v = VisitorDB.getVisitorObjType(cmdFirstToken)
            println("\t" + FormatUtil.print(v, "#,##0"));
        } else {
            val hashList = AgentManager.filter(cmdFirstToken)
            EnumerScala.foreach(hashList.iterator(), (h: Int) => {
                val v = VisitorDB.getVisitorObject(h)
                if (v > 0) {
                    println("\t" + AgentManager.getAgentName(h) + " = " + FormatUtil.print(v, "#,##0"));
                }
            })
        }
    }

}
