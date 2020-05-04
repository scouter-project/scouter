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

import scouter.lang.pack.{PackEnum, XLogDiscardTypes, XLogPack, XLogProfilePack, XLogProfilePack2, XLogTypes}
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

    ThreadScala.startDaemon("scouter.server.core.ProfilePreCore", {
        CoreRun.running
    }) {
        val pack = queue.get();
        ServerStat.put("profile.core0.queue", queue.size());
        if (BytesUtil.getLength(pack.profile) > 0) {
            if (canProcess(pack)) {
                processOnCondition(pack);
            } else {
                waitOnMemory(pack.asInstanceOf[XLogProfilePack2]);
            }
        }
    }

    def canProcess(pack: XLogProfilePack): Boolean = {
        if (pack.getPackType() == PackEnum.XLOG_PROFILE2) {
            val pack2 = pack.asInstanceOf[XLogProfilePack2];
            return (pack2.isDriving()
                    || pack2.ignoreGlobalConsequentSampling
                    || (XLogTypes.isService(pack2.xType) && XLogDiscardTypes.isAliveProfile(pack2.discardType))
                    || XLogTypes.isZipkin(pack2.xType)
                    || XLogDelayingCache.instance.isProcessedGxidWithProfile(pack2.gxid)
                    || pack2.discardType == 0)
        }
        return true;
    }

    def processOnCondition(pack: XLogProfilePack): Unit = {
        if (pack.getPackType() == PackEnum.XLOG_PROFILE2) {
            val pack2 = pack.asInstanceOf[XLogProfilePack2];
            if (pack2.ignoreGlobalConsequentSampling) {
                if (XLogDiscardTypes.isAliveProfile(pack2.discardType)) {
                    ProfileCore.add(pack);
                }
            } else {
                ProfileCore.add(pack);
            }
        } else {
            ProfileCore.add(pack);
        }
    }

    def waitOnMemory(pack2: XLogProfilePack2): Unit = {
        ProfileDelayingCache.instance.addDelaying(pack2);
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
