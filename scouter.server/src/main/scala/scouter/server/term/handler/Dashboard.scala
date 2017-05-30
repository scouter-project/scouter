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
import scouter.lang.counters.CounterEngine
import scouter.server.CounterManager
import java.util.TreeMap
import java.util.Set
import java.util.TreeSet
object Dashboard {

    def process(cmd: String): Unit = {
        val objType = CounterManager.getInstance().getCounterEngine().getObjectType(StringUtil.firstWord(cmd.trim(), " "))
        if (objType == null) {
            println("no object for " + cmd.trim())
            return
        }
        ThreadScala.startDaemon("scouter.server.term.handler.Dashboard") {
            val loopNum = ProcessMain.loopProcess
            while (loopNum == ProcessMain.loopProcess) {
                try {
                    val objHashList = AgentManager.getLiveObjHashList(objType.getName())
                    objType.getFamily().getName() match {
                        case "javaee" => javaee(objHashList)
                        case "host" => host(objHashList, StringUtil.lastWord(cmd, " "))
                        case _ =>
                            println("not supported : " + objType.getName())
                            return
                    }
                } catch {
                    case e: Exception => e.printStackTrace()
                    case x: Throwable =>
                }
                Thread.sleep(2000)
            }
        }

    }

    var lines = 0
    def javaee(objHashList: List[Int]) {
        val inst = StringUtil.leftPad(FormatUtil.print(getCnt(objHashList, "ActiveService", (sum: Double, cnt: Int) => cnt), "#,##0"), 5)
        val ac = StringUtil.leftPad(FormatUtil.print(getCnt(objHashList, "ActiveService", (sum: Double, cnt: Int) => sum), "#,##0"), 5)
        val tps = StringUtil.leftPad(FormatUtil.print(getCnt(objHashList, "TPS", (sum: Double, cnt: Int) => sum), "#,##0"), 5)
        val elapsed = StringUtil.leftPad(FormatUtil.print(getCnt(objHashList, "ElapsedTime", (sum: Double, cnt: Int) => sum / cnt), "#,##0"), 5)
        val gc = StringUtil.leftPad(FormatUtil.print(getCnt(objHashList, "GcCount", (sum: Double, cnt: Int) => sum), "#,##0"), 5)
        val tm = DateUtil.getLogTime(System.currentTimeMillis())
        if (lines == 0) {
            println("            " + "  INST" + "   ACT" + "   TPS" + " RTIME" + " GCCNT")
        }
        lines = if (lines == 10) 0 else lines + 1
        println(tm + " " + inst + " " + ac + " " + tps + " " + elapsed + " " + gc)
    }

    def getCnt(objHashList: List[Int], counter: String, f: (Double, Int) => Double): Double = {
        var sum = 0.0
        var cnt = 0;
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            val c = CounterCache.get(new CounterKey(objHash, counter, TimeTypeEnum.REALTIME));
            if (c != null) {
                sum += c.asInstanceOf[Number].doubleValue()
                cnt += 1
            }

        })
        return if (cnt == 0) 0 else f(sum, cnt)
    }

    var oldTitle: Set[String] = new TreeSet[String]()
    def host(objHashList: List[Int], counter: String) {
        if (counter == "")
            return

        val map = new TreeMap[String, Double]()
        var maxHead = 5;
        EnumerScala.foreach(objHashList.iterator(), (objHash: Int) => {
            var objName = AgentManager.getAgentName(objHash);
            val c = CounterCache.get(new CounterKey(objHash, counter, TimeTypeEnum.REALTIME));
            if (c != null) {
                objName=objName.substring(1)
                val value = c.asInstanceOf[Number].doubleValue()
                map.put(objName, value)
                maxHead = if (objName.length() > maxHead) objName.length() else maxHead
            }
        })
        val head = map.keySet();
        if (lines == 0 || head.size() != oldTitle.size()) {
            oldTitle = head
            val sb = new StringBuffer();
            sb.append("            ")
            EnumerScala.foreach(head.iterator(), (o: String) => {
                sb.append(" ").append(StringUtil.leftPad(o, maxHead))
            })
            println(sb)
        }
        lines = if (lines == 10) 0 else lines + 1
        val sb = new StringBuffer();
        sb.append(DateUtil.getLogTime(System.currentTimeMillis()))
        EnumerScala.foreach(head.iterator(), (o: String) => {
            val value = map.get(o)
            sb.append(" ").append(StringUtil.leftPad(FormatUtil.print(value, "#,##0"), maxHead))
        })

        println(sb)
    }

}
