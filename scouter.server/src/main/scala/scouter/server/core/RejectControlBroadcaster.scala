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
 */
package scouter.server.core

import java.util.concurrent.Executors

import scouter.lang.pack.{MapPack, ObjectPack}
import scouter.lang.value.MapValue
import scouter.net.RequestCmd
import scouter.server.netio.AgentCall
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{IntKeyLinkedMap, StringUtil}

/**
 * Pushes the central reject policy / block list down to every live agent and,
 * in the response of the very same call, collects the violations each agent
 * detected locally.
 *
 * Enforcement stays inside the agent, so a collector outage costs nothing on the
 * request path - agents keep applying the last payload they received.
 *
 * To keep steady-state traffic small the full payload is sent only when the agent
 * reports a version different from the current one; otherwise just two longs go out.
 */
object RejectControlBroadcaster {

  private val conf = Configure.getInstance()

  /**
   * objHash -&gt; packed (policyVersion, blockVersion) the agent last acknowledged.
   * OPTED_OUT marks an agent that answered with control_reject_central_enabled=false,
   * so we keep probing it cheaply instead of re-sending a full payload it would discard.
   */
  private val acked = new IntKeyLinkedMap[Array[Long]]().setMax(10000)
  private val OPTED_OUT: Long = -1L

  private lazy val pool = Executors.newFixedThreadPool(conf.reject_control_sync_thread_count)

  ThreadScala.startDaemon("scouter.server.core.RejectControlBroadcaster", {
    true
  }, conf.reject_control_sync_interval_ms) {
    if (conf.reject_control_enabled) {
      syncAll()
    }
  }

  private def syncAll() {
    val objTypes = StringUtil.split(conf.reject_control_target_obj_types, ',')
    if (objTypes == null) return
    var i = 0
    while (i < objTypes.length) {
      val objType = StringUtil.trimToEmpty(objTypes(i))
      if (objType.length > 0) {
        val hashes = AgentManager.getLiveObjHashList(objType)
        val itr = hashes.iterator()
        while (itr.hasNext) {
          val objHash = itr.next()
          pool.execute(new Runnable() {
            override def run() {
              try {
                syncOne(objHash)
              } catch {
                case t: Throwable => Logger.println("S925", 10, "reject control sync failed", t)
              }
            }
          })
        }
      }
      i += 1
    }
  }

  private def syncOne(objHash: Int) {
    val o: ObjectPack = AgentManager.getAgent(objHash)
    if (o == null) return

    val policyVersion = RejectControlStore.policyVersion
    val blockVersion = RejectControlStore.blockVersion

    val last = acked.get(objHash)
    val optedOut = last != null && last(0) == OPTED_OUT
    val full = !optedOut && (last == null || last(0) != policyVersion || last(1) != blockVersion)

    val param = new MapPack()
    param.put("policyVersion", policyVersion)
    param.put("blockVersion", blockVersion)
    param.put("full", if (full) "true" else "false")

    if (full) {
      val mv = new MapValue()
      val pItr = RejectControlStore.getPolicy().entrySet().iterator()
      while (pItr.hasNext) {
        val e = pItr.next()
        mv.put(e.getKey, e.getValue)
      }
      param.put("policy", mv)

      val (ips, urls) = RejectControlStore.blockArrays()
      val ipLv = param.newList("blockIp")
      val urlLv = param.newList("blockUrl")
      var i = 0
      while (i < ips.length) {
        ipLv.add(ips(i))
        urlLv.add(urls(i))
        i += 1
      }
    }

    val res = AgentCall.call(o, RequestCmd.REJECT_CONTROL_SYNC, param)
    if (res == null) {
      // agent unreachable - forget the ack so the next round re-sends everything
      acked.remove(objHash)
    } else {
      if ("true".equals(res.getText("centralEnabled"))) {
        acked.put(objHash, Array(res.getLong("policyVersion"), res.getLong("blockVersion")))
        collectDetections(res, o.objName)
      } else {
        // the agent opted out of central control - probe cheaply until it comes back
        acked.put(objHash, Array(OPTED_OUT, OPTED_OUT))
      }
    }
  }

  private def collectDetections(res: MapPack, objName: String) {
    val ips = res.getList("detectIp")
    if (ips == null || ips.size() == 0) return
    val urls = res.getList("detectUrl")
    val reasons = res.getList("detectReason")
    val times = res.getList("detectTime")

    var i = 0
    while (i < ips.size()) {
      val ip = ips.getString(i)
      val url = if (urls == null || i >= urls.size()) "" else urls.getString(i)
      val reason = if (reasons == null || i >= reasons.size()) "" else reasons.getString(i)
      val time = if (times == null || i >= times.size()) System.currentTimeMillis() else times.getLong(i)
      RejectControlStore.addDetection(ip, url, reason, time, objName)
      i += 1
    }
  }

  /** touched from ServerStarter to make sure the daemon is initialized */
  def initialize() {
    Logger.println("S926", "reject control broadcaster initialized. enabled=" + conf.reject_control_enabled)
  }
}
