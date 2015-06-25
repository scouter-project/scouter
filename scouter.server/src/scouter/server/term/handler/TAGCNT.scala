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
import scala.collection.JavaConversions._
import scouter.util.IntSet
import scouter.server.core.cache.XLogCache
import scouter.io.DataInputX
import scouter.lang.pack.XLogPack
import scouter.util.CastUtil
import scouter.server.tagcnt.TagCountConfig
import scouter.server.tagcnt.TagCountProxy
import scouter.server.tagcnt.core.ValueCount
object TAGCNT {

    def process(cmd: String): Unit = {

        val cmdArr = StringUtil.tokenizer(cmd, " ")
        if ("group".equals(cmdArr(0))) {
            taggroups
        }
        if (cmdArr.length > 1 && "tag".equals(cmdArr(0))) {
            tagnames(cmdArr(1))
        }
        if (cmdArr.length > 3 && "top100".equals(cmdArr(0))) {
            top100(cmdArr(1), cmdArr(2), cmdArr(3))
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

            EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                println("\t" + vc.tagValue + " \t= " + vc.valueCount);
            })
        }
    }
}
