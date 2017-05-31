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
object XLOG {

    def process(cmd: String): Unit = {

        val cmdTokens = StringUtil.tokenizer(cmd, " ");
        val maxTime = if (cmdTokens.length > 1) CastUtil.cint(cmdTokens(1)) else 10

        ThreadScala.startDaemon("scouter.server.term.handler.XLOG") {
            val loopNum = ProcessMain.loopProcess
            while (loopNum == ProcessMain.loopProcess) {
                try {
                    process(AgentManager.filter(cmdTokens(0)), maxTime)
                } catch {
                    case e: Throwable => e.printStackTrace()
                }
                Thread.sleep(2000)
            }
        }
    }

    var loop = 0L
    var index = 0
    val limitTime = 0
    val bucket = 60

    def process(objHashList: List[Int], mxTime: Int = 10) {
        if (objHashList.size() == 0) {
            println(DateUtil.getLogTime(System.currentTimeMillis()) + " ...")
            return
        }
        val intSet = new IntSet(objHashList.size(), 1.0f);
        EnumerScala.foreach(objHashList.iterator(), (h: Int) => {
            intSet.add(h);
        })
        val d = XLogCache.get(intSet, loop, index, limitTime);
        if (d == null)
            return ;
        var cnt = 0
        loop = d.loop
        index = d.index
        val timeTable = Array.fill(bucket) { '_' }
        var tmStr: String = null
        EnumerScala.foreach(d.data.iterator(), (b: Array[Byte]) => {
            val p = new DataInputX(b).readPack().asInstanceOf[XLogPack]
            val bk = p.elapsed * bucket / (mxTime * 1000)
            val bk2 = if (bk >= bucket) bucket - 1 else bk
            if (timeTable(bk2) != 'E') {
                timeTable(bk2) = if (p.error != 0) 'E' else '#'
            }
            if (tmStr == null) {
                tmStr = DateUtil.getLogTime(p.endTime)
            }
            cnt += 1
        })
        if (tmStr == null) {
            tmStr = DateUtil.getLogTime(System.currentTimeMillis())
        }
        val str = new String(timeTable)
        println(tmStr + " " + AnsiPrint.green(str) + " " + mxTime + "sec " + FormatUtil.print(cnt, "#,##0"))
    }

    def main(args: Array[String]) {
        process("XLOG  tomcat")
    }
}
