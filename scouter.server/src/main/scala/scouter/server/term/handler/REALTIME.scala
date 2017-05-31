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
object REALTIME {

    def process(cmd: String): Unit = {

        val cmdTokens = StringUtil.tokenizer(cmd, " ");
        if (cmdTokens.length < 2)
            return

        val counterName = cmdTokens(1)
        val mode = if (cmdTokens.length > 2) cmdTokens(2) else null

        ThreadScala.startDaemon("scouter.server.term.handler.REALTIME") {
            val loopNum = ProcessMain.loopProcess
            while (loopNum == ProcessMain.loopProcess) {
                try {
                    process(AgentManager.filter(cmdTokens(0)), counterName, mode)
                } catch {
                    case e: Throwable => e.printStackTrace()
                }
                Thread.sleep(2000)
            }
        }
    }

    private def find(cmds: Array[String], word: String): String = {
        for (target <- cmds) {
            if (target.toUpperCase().indexOf(word) >= 0) {
                return target;
            }
        }
        return null
    }

    val format = "#,##0"
    def process(objHashList: List[Int], counterName: String, mode: String) {
        val tm = DateUtil.getLogTime(System.currentTimeMillis())
        if (objHashList.size() == 0) {
            println(tm + " ...")
            return
        }
        if (mode == null) {
            EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
                val c = CounterCache.get(new CounterKey(objHash, counterName, TimeTypeEnum.REALTIME));
                val objName = AgentManager.getAgentName(objHash)
                println(tm + " " + AnsiPrint.blue(objName) + " " + (if (c == null) "null" else FormatUtil.print(c, format)))
            })
            return
        }
        var sum = 0.0
        var cnt = 0;
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            val c = CounterCache.get(new CounterKey(objHash, counterName, TimeTypeEnum.REALTIME));
            sum += c.asInstanceOf[Number].doubleValue()
            cnt += 1
        })

        mode.toUpperCase() match {
            case "SUM" => println(tm + " " + counterName + " " + FormatUtil.print(sum, format))
            case "AVG" => println(tm + " " + counterName + " " + FormatUtil.print(sum / cnt, format))
            case _ => println(tm + " " + counterName + " " + FormatUtil.print(sum, format))
        }
    }

    private def getFormat(fm: String): String = {
        if (fm == null) return "#,##0"
        try {
            return StringUtil.tokenizer(fm, " ")(1)
        } catch {
            case _: Throwable => return "#,##0"
        }
    }
    def main(args: Array[String]) {
        process("REALTIME tomcat TPS")
    }
}
