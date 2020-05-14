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

package scouter.server.core

import java.util
import java.util.function.Consumer

import scouter.lang.pack.{XLogDiscardTypes, XLogPack, XLogTypes}
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{LongKeyLinkedMap, RequestQueue}

object XLogDelayingsRecoverCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[LongKeyLinkedMap[util.List[XLogPack]]](5);

    ThreadScala.startDaemon("scouter.server.core.XLogDelayingRecoverCore", {
        CoreRun.running
    }) {
        val packsMap = queue.get();
        ServerStat.put("xlog.core-r.queue", queue.size());

        if (Configure.WORKABLE) {
            val enumeration = packsMap.values();
            while(enumeration.hasMoreElements) {
                val packs = enumeration.nextElement();
                packs.forEach(new Consumer[XLogPack] {
                    override def accept(pack: XLogPack): Unit = {
                        if (pack.discardType != XLogDiscardTypes.DISCARD_ALL) {
                            XLogCore.add(pack);
                        }
                    }
                });
            }
        }
    }

    def add(packsMap: LongKeyLinkedMap[util.List[XLogPack]]) {
        val ok = queue.put(packsMap);
        if (!ok) {
            Logger.println("S116-1", 10, "queue exceeded!!");
        }
    }

}
