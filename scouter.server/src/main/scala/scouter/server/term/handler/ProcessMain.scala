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
import scouter.lang.counters.CounterConstants
import scouter.lang.counters.CounterEngine
import scouter.util.SortUtil
import scouter.lang.pack.ObjectPack
import java.util.TreeSet

object ProcessMain {
    var loopProcess = 1

    def process(cmd: String): Unit = {
        loopProcess += 1

        StringUtil.firstWord(cmd, " ").toLowerCase() match {
            case "help" => Help.help(cmd.trim())
            case "q" | "quit" => Help.quit()
            case "objtype" => objType()
            case "object" => objectList(cmd.trim())
            case "counter" => counterList(cmd.trim())
            case "dashboard" => Dashboard.process(cut(cmd, "dashboard"))
            case "realtime" => REALTIME.process(cut(cmd, "realtime"))
            case "xlog" => XLOG.process(cut(cmd, "xlog"))
            case "xlist" => XLIST.process(cut(cmd, "xlist"))
            case "tagcnt" => TAGCNT.process(cut(cmd, "tagcnt"))
            case "visitor" => VISITOR.process(cut(cmd, "visitor"))
            case "debug" => DEBUG.process(cut(cmd, "debug"))
            case "" =>
            case _ => Help.help(cmd)
        }
    }

    val counterEng = scouter.server.CounterManager.getInstance().getCounterEngine();

    def objType(): Unit = {
        val objTypes = new TreeSet[String]();
        EnumerScala.foreach(AgentManager.getObjPacks(), (o: ObjectPack) => {
            objTypes.add(o.objType)
        })
        val sorted = SortUtil.sort_string(objTypes.iterator(), objTypes.size(), true)
        EnumerScala.foreach(sorted, (o: String) => {
            println("\t" + AnsiPrint.green(o))
        })
    }
    def objectList(cmd: String): Unit = {
        var cmds = StringUtil.tokenizer(cmd, " ,")
        val objHashList = AgentManager.getLiveObjHashList()
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            val obj = AgentManager.getAgent(objHash)
            val text = "\t" + StringUtil.rpad(obj.objType, 20) + "\t" + StringUtil.rpad(obj.objName, 30) + "\t" + (if (obj.alive) "alive" else "")
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
        val sorted = SortUtil.sort_string(counters.iterator(), counters.size(), true)
        EnumerScala.foreach(sorted, (c: String) => {
            println("\t" + AnsiPrint.green(c))
        })
    }

    private def cut(cmd: String, word: String): String = {
        cmd.trim().substring(word.length()).trim()
    }
}
