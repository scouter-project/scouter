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
import scouter.server.tagcnt.TagCountConfig
import scouter.server.tagcnt.TagCountProxy
import scouter.server.tagcnt.core.ValueCount
import scouter.lang.value.ValueEnum
import scouter.server.db.TextRD
import scouter.lang.value.TextHashValue
import scouter.lang.TextTypes
import scala.util.control.BreakControl
import scala.util.control.Breaks
object TAGCNT {

    def process(cmd: String): Unit = {

        val cmdTokens = StringUtil.tokenizer(cmd, " ")
        if (cmdTokens.length < 1)
            return

        if ("group".equals(cmdTokens(0))) {
            taggroups
        }
        if (cmdTokens.length > 1 && "tag".equals(cmdTokens(0))) {
            tagnames(cmdTokens(1))
        }
        if (cmdTokens.length > 3 && "top100".equals(cmdTokens(0))) {
            top100(cmdTokens(1), cmdTokens(2), cmdTokens(3))
        }
        if (cmdTokens.length > 3 && "data".equals(cmdTokens(0))) {
            getCount(cmdTokens(1), cmdTokens(2), cmdTokens(3), if (cmdTokens.length > 4) cmdTokens(4).toInt else -1)
        }
    }

    private def taggroups(): Unit = {
        val tagGroups = TagCountConfig.getTagGroups();
        while (tagGroups.hasMoreElements()) {
            val str = tagGroups.nextString()
            println("\t" + str)
        }
    }

    private def tagnames(tagGroup: String): Unit = {
        val tagNames = TagCountConfig.getTagNames(tagGroup);
        while (tagNames.hasMoreElements()) {
            println("\t" + tagNames.nextString())
        }
    }

    private def top100(objType: String, tagGroup: String, tagName: String): Unit = {
        val date = DateUtil.yyyymmdd()
        val valueCountTotal = TagCountProxy.getTagValueCountWithCache(date, objType, tagGroup, tagName, 100);
        if (valueCountTotal != null) {

            var inx = 1
            EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                if (vc.tagValue.getValueType() == ValueEnum.TEXT_HASH) {
                    val valueText = TextRD.getString(date, TextTypes.ERROR, vc.tagValue.asInstanceOf[TextHashValue].value)
                    println("\t" + inx + " " + valueText + "(" + vc.tagValue + ") \t= " + vc.valueCount);
                } else {
                    println("\t" + inx + " " + vc.tagValue + " \t= " + vc.valueCount);
                }
                inx += 1
            })
        }
    }
    private def getCount(objType: String, tagGroup: String, tagName: String, x: Int): Unit = {
        if (x == 0)
            return
        val date = DateUtil.yyyymmdd()
        val valueCountTotal = TagCountProxy.getTagValueCountWithCache(date, objType, tagGroup, tagName, 100);
        if (valueCountTotal != null) {
            var inx = 1
            Breaks.breakable {
                EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                    if (inx == x || x <0) {
                        val values = TagCountProxy.getTagValueCountData(date, objType, tagGroup, tagName, vc.tagValue);
                        printTable(values)
                        Breaks.break
                    }
                    inx += 1
                })
            }
        }
    }
    private def printTable(values: Array[Float]) {
        for (h <- 0 to 23) {
            print(StringUtil.leftPad("H" + h, 5) + " ")
            for (m <- 0 to 59) {
                if (m > 0)
                    print(", ")
                print(FormatUtil.print(values(h * 60 + m), "#,##0.0"))
            }
            println("")
        }
    }
}
