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
package scouter.server.netio.service.handle

import java.util.LinkedHashMap

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.pack.MapPack
import scouter.lang.value.MapValue
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.core.RejectControlStore
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.StringUtil

/**
 * Management API of the central reject(block) control.
 *
 * There is no TTL on a block - {@code REJECT_CONTROL_REMOVE_BLOCK} is the only way
 * an entry is released, which is why the listing carries enough context
 * (reason, time, who blocked it, how often it was detected) for an operator to judge.
 */
class RejectControlService {

  @ServiceHandler(RequestCmd.REJECT_CONTROL_GET_POLICY)
  def getPolicy(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val policy = RejectControlStore.getPolicy()
    val result = new MapPack()
    val mv = new MapValue()
    val itr = policy.entrySet().iterator()
    while (itr.hasNext) {
      val e = itr.next()
      mv.put(e.getKey, e.getValue)
    }
    result.put("policy", mv)
    result.put("policyVersion", RejectControlStore.policyVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }

  @ServiceHandler(RequestCmd.REJECT_CONTROL_SET_POLICY)
  def setPolicy(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack]
    val mv = param.get("policy").asInstanceOf[MapValue]

    val map = new LinkedHashMap[String, String]()
    if (mv != null) {
      val keys = mv.keySet().iterator()
      while (keys.hasNext) {
        val k = keys.next()
        map.put(k, mv.getText(k))
      }
    }
    RejectControlStore.setPolicy(map)

    val result = new MapPack()
    result.put("result", "true")
    result.put("policyVersion", RejectControlStore.policyVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }

  @ServiceHandler(RequestCmd.REJECT_CONTROL_LIST_BLOCK)
  def listBlock(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val result = new MapPack()

    val ip = result.newList("ip")
    val url = result.newList("url")
    val reason = result.newList("reason")
    val time = result.newList("blockedTime")
    val by = result.newList("blockedBy")
    val cnt = result.newList("detectCount")

    val blocks = RejectControlStore.listBlocks()
    var i = 0
    while (i < blocks.size()) {
      val b = blocks.get(i)
      ip.add(b.ip)
      url.add(b.url)
      reason.add(b.reason)
      time.add(b.blockedTime)
      by.add(b.blockedBy)
      cnt.add(b.detectCount)
      i += 1
    }

    // detection candidates awaiting an operator decision
    val dIp = result.newList("detectIp")
    val dUrl = result.newList("detectUrl")
    val dReason = result.newList("detectReason")
    val dFirst = result.newList("detectFirstTime")
    val dLast = result.newList("detectLastTime")
    val dCount = result.newList("detectCount2")
    val dObj = result.newList("detectObjNames")

    val detections = RejectControlStore.listDetections()
    var j = 0
    while (j < detections.size()) {
      val d = detections.get(j)
      dIp.add(d.ip)
      dUrl.add(d.url)
      dReason.add(d.reason)
      dFirst.add(d.firstTime)
      dLast.add(d.lastTime)
      dCount.add(d.count)
      dObj.add(d.objNames)
      j += 1
    }

    result.put("blockVersion", RejectControlStore.blockVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }

  @ServiceHandler(RequestCmd.REJECT_CONTROL_ADD_BLOCK)
  def addBlock(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack]
    val ip = StringUtil.trimToEmpty(param.getText("ip"))
    val url = StringUtil.trimToEmpty(param.getText("url"))
    val reason = StringUtil.trimToEmpty(param.getText("reason"))
    val by = StringUtil.trimToEmpty(param.getText("by"))

    val ok = RejectControlStore.addBlock(ip, url, if (reason.length == 0) "MANUAL" else reason,
      if (by.length == 0) "operator" else by)

    val result = new MapPack()
    result.put("result", String.valueOf(ok))
    result.put("blockVersion", RejectControlStore.blockVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }

  @ServiceHandler(RequestCmd.REJECT_CONTROL_REMOVE_BLOCK)
  def removeBlock(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack]
    val ip = StringUtil.trimToEmpty(param.getText("ip"))
    val url = StringUtil.trimToEmpty(param.getText("url"))

    val ok = RejectControlStore.removeBlock(ip, url)

    val result = new MapPack()
    result.put("result", String.valueOf(ok))
    result.put("blockVersion", RejectControlStore.blockVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }

  @ServiceHandler(RequestCmd.REJECT_CONTROL_CLEAR_BLOCK)
  def clearBlock(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack]
    val target = StringUtil.trimToEmpty(param.getText("target"))

    val result = new MapPack()
    if ("detection".equals(target)) {
      result.put("removed", RejectControlStore.clearDetections())
    } else {
      result.put("removed", RejectControlStore.clearBlocks())
    }
    result.put("blockVersion", RejectControlStore.blockVersion)
    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(result)
  }
}
