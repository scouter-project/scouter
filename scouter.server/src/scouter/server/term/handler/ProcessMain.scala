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
import scouter.lang.counters.CounterConstants
import scouter.lang.counters.CounterEngine

object ProcessMain {
    var loopProcess = 1

    def process(cmd: String): Unit = {
        loopProcess += 1

        StringUtil.firstWord(cmd, " ").toLowerCase() match {
            case "help" => return Help.help(cmd)
            case "quit" => return Help.quit()
            case "objtypes" => return objType()
            case "objects" => return objectList(cmd)
            case "counters" => return counterList(cmd)
            case "realtime" => return REALTIME.process(cmd.substring("realtime".length()).trim())
            case "xlog" => return XLOG.process(cmd.substring("xlog".length()).trim())
            case _ => return Help.help(cmd)
        }
    }

    val counterEng = scouter.server.CounterManager.getInstance().getCounterEngine();

    def objType(): Unit = {
        val objTypes = counterEng.getAllObjectType()
        EnumerScala.foreach(objTypes.iterator(), (o: String) => {
            println(o)
        })
    }
    def objectList(cmd: String): Unit = {
        var cmds = StringUtil.tokenizer(cmd, " ,")
        val objHashList = AgentManager.getLiveObjHashList()
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            val obj = AgentManager.getAgent(objHash)
            val text = obj.objType + "\t" + obj.objName + "\t" + (if (obj.alive) "alive" else "")
            if (cmds.length < 2 || text.indexOf(cmds(1)) >= 0) {
                println(text)
            }
        })
    }
    def counterList(cmd: String): Unit = {
        var cmds = StringUtil.tokenizer(cmd, " ,")
        if (cmds.length < 2)
            return

        val counters = counterEng.getAllCounterList(cmds(1))
        EnumerScala.foreach(counters.iterator(), (c: String) => {
            println(c)
        })
    }
}
