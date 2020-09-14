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

import scouter.lang.TextTypes
import scouter.lang.pack.{PackEnum, XLogDiscardTypes, XLogPack, XLogProfilePack, XLogProfilePack2, XLogTypes}
import scouter.server.core.ProfileCore.queue
import scouter.server.core.ProfilePreCore.canProcess
import scouter.server.core.XLogPreCore.processDelayingChildren
import scouter.server.core.cache.{ProfileDelayingCache, XLogDelayingCache}
import scouter.server.db.{TextRD, XLogProfileWR}
import scouter.server.plugin.PlugInManager
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{BytesUtil, DateUtil, RequestQueue}

import scala.util.Random

object ProfilePreCore {

    val conf = Configure.getInstance();
    val queue = new RequestQueue[XLogProfilePack](conf.profile_queue_size);

    ThreadScala.startDaemon("scouter.server.core.ProfilePreCore", {
        CoreRun.running
    }) {
        val pack = queue.get();
        ServerStat.put("profile.core0.queue", queue.size());

        val pack2 = pack match {
            case _pack: XLogProfilePack2 => _pack
            case _ => null
        };

        //Drop profile Pack by txid
        if (pack2 != null && pack2.isForDrop) {
            ProfileDelayingCache.instance.removeDelayingChildren(pack2);

        //Process delaying profile Pack by txid
        } else if (pack2 != null && pack2.isForProcessDelayingChildren) {
            processDelayingProfiles(pack2)

        } else if (BytesUtil.getLength(pack.profile) > 0) {
            if (canProcess(pack)) {
                processOnCondition(pack);
            } else {
                //it must be XLogProfilePack2 type. (by canProcess() results false)
                waitOnMemory(pack.asInstanceOf[XLogProfilePack2]);
            }
        }
    }

    def add(p: XLogProfilePack) {
        p.time = System.currentTimeMillis();

        val ok = queue.put(p)
        if (!ok) {
            Logger.println("S110-0", 10, "queue exceeded!!");
        }
    }

    def addAsDropped(p: XLogPack): Unit = {
        val profilePack = XLogProfilePack2.forInternalDropProcessing(p);
        val ok = queue.put(profilePack)
        if (!ok) {
            Logger.println("S110-0-1", 10, "queue exceeded!!");
        }
    }

    def addAsProcessDelayingChildren(p: XLogPack): Unit = {
        val profilePack = XLogProfilePack2.forInternalDelayingChildrenProcessing(p);
        val ok = queue.put(profilePack)
        if (!ok) {
            Logger.println("S110-0-2", 10, "queue exceeded!!");
        }
    }

    val rnd = new Random

    private def processDelayingProfiles(pack2: XLogProfilePack2) = {
        val profileList = ProfileDelayingCache.instance.popDelayingChildren(pack2);
        val delayingCount = profileList.size()
        var cnt = 0

        profileList.forEach(new Consumer[XLogProfilePack2] {
            override def accept(delayingPack: XLogProfilePack2): Unit = {
                if (pack2.discardType != XLogDiscardTypes.DISCARD_ALL) { //this condition is correct
                    //TODO remove logging
                    if (conf.log_test_rate > 0 && rnd.nextInt(100) < conf.log_test_rate) {
                        val serviceName = TextRD.getString(DateUtil.yyyymmdd(delayingPack.time), TextTypes.SERVICE, delayingPack.service)
                        val profileSize = if (delayingPack.profile == null) 0 else delayingPack.profile.length
                        Logger.println(s"${profileSize}\t[processDelaying][${cnt}/${delayingCount}] ${delayingPack.toDebugString(serviceName)}")
                        cnt += 1
                    }
                    ProfileCore.add(delayingPack)
                }
            }
        });
    }

    private def canProcess(pack: XLogProfilePack): Boolean = {
        if (pack.getPackType() == PackEnum.XLOG_PROFILE2) {
            val pack2 = pack.asInstanceOf[XLogProfilePack2];

            val result = (pack2.isDriving()
                    || pack2.ignoreGlobalConsequentSampling
                    || (XLogTypes.isService(pack2.xType) && XLogDiscardTypes.isAliveProfile(pack2.discardType))
                    || XLogTypes.isZipkin(pack2.xType)
                    || XLogDelayingCache.instance.isProcessedGxidWithProfile(pack2.gxid)
                    || pack2.discardType == 0)

            return result
        }
        return true;
    }

    private def processOnCondition(pack: XLogProfilePack): Unit = {
        if (pack.getPackType() == PackEnum.XLOG_PROFILE2) {
            val pack2 = pack.asInstanceOf[XLogProfilePack2];
            if (pack2.ignoreGlobalConsequentSampling) {
                //TODO remove logging
                if (XLogDiscardTypes.isAliveProfile(pack2.discardType)) {
                    if (conf.log_test_rate > 0 && rnd.nextInt(100) < conf.log_test_rate) {
                        val serviceName = TextRD.getString(DateUtil.yyyymmdd(pack.time), TextTypes.SERVICE, pack.service)
                        val profileSize = if (pack.profile == null) 0 else pack.profile.length
                        Logger.println(s"${profileSize}\t[ProfileCoreCase1(IgnoreCons)] ${pack2.toDebugString(serviceName)}")
                    }
                    ProfileCore.add(pack);
                }
            } else {
                //TODO remove logging
                if (conf.log_test_rate > 0 && rnd.nextInt(100) < conf.log_test_rate) {
                    val serviceName = TextRD.getString(DateUtil.yyyymmdd(pack.time), TextTypes.SERVICE, pack.service)
                    val profileSize = if (pack.profile == null) 0 else pack.profile.length
                    Logger.println(s"${profileSize}\t[ProfileCoreCase2] ${pack2.toDebugString(serviceName)}")
                }
                ProfileCore.add(pack);
            }
        } else {
            //TODO remove logging
            if (conf.log_test_rate > 0 && rnd.nextInt(100) < conf.log_test_rate) {
                val serviceName = TextRD.getString(DateUtil.yyyymmdd(pack.time), TextTypes.SERVICE, pack.service)
                val profileSize = if (pack.profile == null) 0 else pack.profile.length
                Logger.println(s"${profileSize}\t[ProfileCoreCase3(Old)] ${pack.toDebugString(serviceName)}")
            }
            ProfileCore.add(pack);
        }
    }

    private def waitOnMemory(pack2: XLogProfilePack2): Unit = {
        ProfileDelayingCache.instance.addDelaying(pack2);
    }

    private def doNothing() {}
}
