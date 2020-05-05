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
import scouter.server.core.cache.{ProfileDelayingCache, XLogDelayingCache}
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.RequestQueue

object XLogPreCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[XLogPack](conf.xlog_queue_size);

    ThreadScala.startDaemon("scouter.server.core.XLogPreCore", {
        CoreRun.running
    }) {
        val pack = queue.get();
        ServerStat.put("xlog.core0.queue", queue.size());
        if (Configure.WORKABLE) {
            if (pack.isDropped) {
                //discard dropped in delaying
                XLogDelayingCache.instance.removeDelayingChildren(pack);
                ProfilePreCore.addAsDropped(pack);

            } else {
                if (canProcess(pack)) {
                    processOnCondition(pack);
                } else {
                    waitOnMemory(pack);
                }
            }
        }
    }

    def add(pack: XLogPack) {
        if (pack.endTime == 0) {
            pack.endTime = System.currentTimeMillis();
        }

        val ok = queue.put(pack);
        if (!ok) {
            Logger.println("S116-0", 10, "queue exceeded!!");
        }
    }

    private def processOnCondition(pack: XLogPack): Unit = {
        if (pack.ignoreGlobalConsequentSampling) {
            if (XLogDiscardTypes.isAliveXLog(pack.discardType)) {
                process0(pack);
            }
        } else {
            process0(pack);
        }
    }

    private def process0(pack: XLogPack) {
        XLogDelayingCache.instance.addProcessed(pack);
        processDelayingChildren(pack);
        XLogCore.add(pack);
    }

    private def canProcess(pack: XLogPack): Boolean = {
        if (pack.isDriving()
                || pack.ignoreGlobalConsequentSampling
                || pack.discardType == 0
                || (XLogTypes.isService(pack.xType) && XLogDiscardTypes.isAliveXLog(pack.discardType))
                || XLogTypes.isZipkin(pack.xType)
                || XLogDelayingCache.instance.isProcessedGxid(pack.gxid)
        ) { //discardType 0 means intelligent sampling unsupported version.
            true;

        } else {
            false;
        }
    }

    private def processDelayingChildren(pack: XLogPack): Unit = {
        //for profile
        ProfilePreCore.addAsProcessDelayingChildren(pack);

        //for xlog
        val xLogList = XLogDelayingCache.instance.popDelayingChildren(pack);
        xLogList.forEach(new Consumer[XLogPack] {
            override def accept(delayingPack: XLogPack): Unit = {
                if (pack.discardType == XLogDiscardTypes.DISCARD_NONE) {
                    XLogCore.add(delayingPack)
                }
            }
        });
    }

    private def waitOnMemory(pack: XLogPack): Unit = {
        XLogDelayingCache.instance.addDelaying(pack);
    }

}
