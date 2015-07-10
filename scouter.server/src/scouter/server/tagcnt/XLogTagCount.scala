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

package scouter.server.tagcnt;

import scouter.lang.pack.XLogPack
import scouter.lang.value.DecimalValue
import scouter.lang.value.IP4Value
import scouter.lang.value.TextHashValue
import scouter.lang.value.TextValue
import scouter.lang.value.NullValue;
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.AgentManager
import scouter.server.core.CoreRun
import scouter.server.util.ThreadScala
import scouter.util.IPUtil
import scouter.util.RequestQueue
import scouter.util.StringUtil

object XLogTagCount {

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("scouter.server.tagcnt.XLogTagCount") {
        val conf = Configure.getInstance();
        while (CoreRun.running) {
            val m = queue.get();
            try {
                val objInfo = AgentManager.getAgent(m.objHash)
                if (objInfo != null) {
                    process(objInfo.objType, m)
                }
            } catch {
                case e: Exception =>
                    Logger.println("S187", 10, "XLogTagCount", e)
            }
        }
    }

    def add(p: XLogPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S188", 10, "XLogTagCount queue exceeded!!");
        }
    }
    def process(objType: String, x: XLogPack) {
        TagCountProxy.add(x.endTime, objType, TagCountConfig.service.total, NullValue.value, 1)

        TagCountProxy.add(x.endTime, objType, TagCountConfig.service.objectName, new TextHashValue(x.objHash), 1)
        TagCountProxy.add(x.endTime, objType, TagCountConfig.service.service, new TextHashValue(x.service), 1)

        if (x.elapsed > 0) {
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.service_elapsed, new TextHashValue(x.service), x.elapsed)
        }
        if (x.bytes > 0) {
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.service_bytes, new TextHashValue(x.service), x.bytes)
        }
        if (x.error != 0) {
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.service_errors, new TextHashValue(x.service), 1)
        }

        if (x.group != 0)
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.group, new TextHashValue(x.group), 1)

        if (x.userAgent != 0)
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.userAgent, new TextHashValue(x.userAgent), 1)

        if (x.referer != 0)
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.referer, new TextHashValue(x.referer), 1)

        if (IPUtil.isNotLocal(x.ipaddr))
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.ip, new IP4Value(x.ipaddr), 1)

        if (x.city != 0)
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.city, new TextHashValue(x.city), 1)

        if (StringUtil.isNotEmpty(x.countryCode))
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.nation, new TextValue(x.countryCode), 1)

        if (x.error != 0) {
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.error, new TextHashValue(x.error), 1)
        }

        if (x.visitor != 0) {
            TagCountProxy.add(x.endTime, objType, TagCountConfig.service.visitor, new DecimalValue(x.visitor), 1)
        }

        val elapsed = x.elapsed / 1000
        if (elapsed > 0) {
            elapsed match {
                case 1 | 2 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.elapsed, new DecimalValue(1), 1)
                case 3 | 4 | 5 | 6 | 7 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.elapsed, new DecimalValue(3), 1)
                case _ => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.elapsed, new DecimalValue(8), 1)
            }
        }
        val sqlTime = x.sqlTime / 1000
        if (sqlTime > 0) {
            sqlTime match {
                case 1 | 2 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.sqltime, new DecimalValue(1), 1)
                case 3 | 4 | 5 | 6 | 7 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.sqltime, new DecimalValue(3), 1)
                case _ => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.sqltime, new DecimalValue(8), 1)
            }
        }
        val apicallTime = x.apicallTime / 1000
        if (apicallTime > 0) {
            apicallTime match {
                case 1 | 2 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.apitime, new DecimalValue(1), 1)
                case 3 | 4 | 5 | 6 | 7 => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.apitime, new DecimalValue(3), 1)
                case _ => TagCountProxy.add(x.endTime, objType, TagCountConfig.service.apitime, new DecimalValue(8), 1)
            }
        }
    }
}
