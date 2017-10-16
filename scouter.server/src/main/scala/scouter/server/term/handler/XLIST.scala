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
import scouter.server.db.xlog.XLogDataReader
import scouter.server.db.XLogRD
import scouter.server.db.TextRD
import scouter.lang.TextTypes
import scouter.util.IPUtil
import scouter.util.Hexa32
object XLIST {

    def process(cmd: String): Unit = {
        
        val cmdTokens = StringUtil.tokenizer(cmd, " ");

        val objType = cmdTokens(0)
        val time = DateUtil.getTime(DateUtil.yyyymmdd() + cmdTokens(1), "yyyyMMddHH:mm:ss.SSS");
        val maxCount = if (cmdTokens.length > 2) CastUtil.cint(cmdTokens(2)) else 10000
        val minElapsed = if (cmdTokens.length > 3) CastUtil.cint(cmdTokens(3)) else 0

        process(AgentManager.filter(cmdTokens(0)), time, maxCount, minElapsed)

    }

    def process(objHashList: List[Int], time: Long, maxCount: Int, minElapsed: Int): Unit = {
        var loadCount = 0
        val handler = (time: Long, data: Array[Byte]) => {
            if (loadCount >= maxCount) {
                return ;
            }
            loadCount += 1

            val x = new DataInputX(data).readPack().asInstanceOf[XLogPack];
            var serviceName = TextRD.getString(DateUtil.yyyymmdd(time), TextTypes.SERVICE, x.service);
            val sb = new StringBuffer()
            sb.append(StringUtil.leftPad(FormatUtil.print(loadCount, "#,##0"),5)).append(' ')
            sb.append(DateUtil.getLogTime(x.endTime)).append(' ')
            sb.append(Hexa32.toString32(x.txid)).append(' ')
            sb.append(AgentManager.getAgentName(x.objHash)).append(' ')
            sb.append(IPUtil.toString(x.ipaddr)).append(' ')
            sb.append(serviceName).append(' ')
            sb.append(FormatUtil.print(x.elapsed, "#,##0")).append("ms ")
            if (x.sqlCount > 0) {
                sb.append("sql:").append(FormatUtil.print(x.sqlCount, "#,##0")).append("/")
                sb.append(FormatUtil.print(x.sqlTime, "#,##0")).append("ms ")
            }
            if (x.apicallCount > 0) {
                sb.append("api:").append(FormatUtil.print(x.apicallCount, "#,##0")).append("/")
                sb.append(FormatUtil.print(x.apicallTime, "#,##0")).append("ms ")
            }
            if (x.userAgent != 0) {
                var userAgent = TextRD.getString(DateUtil.yyyymmdd(time), TextTypes.USER_AGENT, x.userAgent);
                sb.append(userAgent).append(' ')
            }
            println(sb.toString())
        }
        XLogRD.readByTime(DateUtil.yyyymmdd(), time, time + 2000, handler)
    }

}
