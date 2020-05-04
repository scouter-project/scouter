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

import java.util.function.Consumer

import scouter.lang.pack.{XLogDiscardTypes, XLogPack, XLogProfilePack, XLogTypes}
import scouter.server.core.ProfileCore.queue
import scouter.server.core.ProfilePreCore.canProcess
import scouter.server.core.XLogPreCore.processDelayingChildren
import scouter.server.core.cache.{ProfileDelayingCache, XLogDelayingCache}
import scouter.server.db.XLogProfileWR
import scouter.server.plugin.PlugInManager
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{BytesUtil, RequestQueue}

object ProfilePreCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[XLogProfilePack](conf.profile_queue_size);

    ThreadScala.startDaemon("scouter.server.core.ProfilePreCore", { CoreRun.running }) {
        val pack = queue.get();
        ServerStat.put("profile.core0.queue", queue.size());
        if (BytesUtil.getLength(pack.profile) > 0) {
            if (canProcess(pack)) {
                processOnCondition(pack);
            } else {
                waitOnMemory(pack);
            }
        }
    }

    def processOnCondition(pack: XLogProfilePack): Unit = {
        if (pack.ignoreGlobalConsequentSampling) {
            if (XLogDiscardTypes.isAliveProfile(pack.discardType)) {
                ProfileCore.add(pack);
            }
        } else {
            ProfileCore.add(pack);
        }
    }

    def canProcess(pack: XLogProfilePack): Boolean = {
        (pack.isDriving()
                || pack.ignoreGlobalConsequentSampling
                || (XLogTypes.isService(pack.xType) && XLogDiscardTypes.isAliveProfile(pack.discardType))
                || XLogTypes.isZipkin(pack.xType)
                || XLogDelayingCache.instance.isProcessedGxidWithProfile(pack.gxid)
                || pack.discardType == 0); //discardType 0 means intelligent sampling unsupported version.
    }

    def waitOnMemory(pack: XLogProfilePack): Unit = {
        ProfileDelayingCache.instance.addDelaying(pack);
    }

    def doNothing() {}

    def add(p: XLogProfilePack) {
        p.time = System.currentTimeMillis();

        val ok = queue.put(p)
        if (!ok) {
            Logger.println("S110-0", 10, "queue exceeded!!");
        }
    }
}
