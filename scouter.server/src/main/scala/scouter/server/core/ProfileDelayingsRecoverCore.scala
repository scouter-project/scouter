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

import scouter.lang.pack.{XLogDiscardTypes, XLogPack, XLogProfilePack, XLogProfilePack2}
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{LongKeyLinkedMap, RequestQueue}

object ProfileDelayingsRecoverCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[LongKeyLinkedMap[util.List[XLogProfilePack2]]](5);

    ThreadScala.startDaemon("scouter.server.core.ProfileDelayingRecoverCore", {CoreRun.running}) {
        val packsMap = queue.get();
        ServerStat.put("profile.core-r.queue", queue.size());

        if (Configure.WORKABLE) {
            val enumeration = packsMap.values();
            while(enumeration.hasMoreElements) {
                val packs = enumeration.nextElement();
                packs.forEach(new Consumer[XLogProfilePack2] {
                    override def accept(pack: XLogProfilePack2): Unit = {
                        if (pack.discardType == XLogDiscardTypes.DISCARD_NONE) {
                            ProfileCore.add(pack);
                        }
                    }
                });
            }
        }
    }

    def add(packsMap: LongKeyLinkedMap[util.List[XLogProfilePack2]]) {
        val ok = queue.put(packsMap);
        if (!ok) {
            Logger.println("S110-1", 10, "queue exceeded!!");
        }
    }

}
