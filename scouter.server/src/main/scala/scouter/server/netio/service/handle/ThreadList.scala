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

package scouter.server.netio.service.handle;

import java.io.IOException;
import java.util.List;

import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.server.core.AgentManager;
import scouter.server.netio.AgentCall;
import scouter.server.netio.service.anotation.ServiceHandler;
import scouter.util.ThreadUtil;
import scala.collection.JavaConversions._

class ThreadList {

  @ServiceHandler(RequestCmd.SERVER_THREAD_DETAIL)
  def serverThreadDetail(din: DataInputX, dout: DataOutputX, login: Boolean) {

    val param = din.readMapPack();
    val p = ThreadUtil.getThreadDetail(param.getLong("id"));
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.SERVER_THREAD_LIST)
  def serverThreadList(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val result = ThreadUtil.getThreadList();
    if (result != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(result);
    }

  }

  @ServiceHandler(RequestCmd.OBJECT_THREAD_DETAIL)
  def agentThreadDetail(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");

    val o = AgentManager.getAgent(objHash);
    if (param.containsKey("id")) {
      val p = AgentCall.call(o, RequestCmd.OBJECT_THREAD_DETAIL, param);
      if (p != null) {
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
      }
    }
  }

  @ServiceHandler(RequestCmd.OBJECT_THREAD_LIST)
  def agentThreadList(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");

    val o = AgentManager.getAgent(objHash);
    val p = AgentCall.call(o, RequestCmd.OBJECT_THREAD_LIST, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.OBJECT_ACTIVE_SERVICE_LIST)
  def agentActiveServiceList(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objType = param.getText("objType");
    val objHash = param.getInt("objHash");
    if (objHash == 0) {
      if (objType == null) {
        return ;
      }
      val agentList = AgentManager.getLiveObjHashList(objType);
      for (agent <- agentList) {
        val o = AgentManager.getAgent(agent);
        val p = AgentCall.call(o, RequestCmd.OBJECT_ACTIVE_SERVICE_LIST, param);
        if (p == null) {
          val emptyPack = new MapPack();
          emptyPack.put("objHash", agent);
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(emptyPack);
        } else {
          p.put("objHash", agent);
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(p);
        }
      }
    } else {
      val o = AgentManager.getAgent(objHash);
      val p = AgentCall.call(o, RequestCmd.OBJECT_ACTIVE_SERVICE_LIST, param);
      if (p == null) {
		val emptyPack = new MapPack();
		emptyPack.put("objHash", objHash);
		dout.writeByte(TcpFlag.HasNEXT);
		dout.writePack(emptyPack);
      } else {
        p.put("objHash", objHash);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
      }
    }
  }
  
  @ServiceHandler(RequestCmd.OBJECT_ACTIVE_SERVICE_LIST_GROUP)
  def agentActiveServiceListGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    for (i <- 0 to objHashLv.size() - 1) {
      val objHash = objHashLv.getInt(i)
      val o = AgentManager.getAgent(objHash);
      val p = AgentCall.call(o, RequestCmd.OBJECT_ACTIVE_SERVICE_LIST, param);
      if (p == null) {
    		val emptyPack = new MapPack();
    		emptyPack.put("objHash", objHash);
    		dout.writeByte(TcpFlag.HasNEXT);
    		dout.writePack(emptyPack);
      } else {
        p.put("objHash", objHash);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
      }
    }
  }

  @ServiceHandler(RequestCmd.OBJECT_THREAD_DUMP)
  def agentThreadDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");

    val o = AgentManager.getAgent(objHash);
    val p = AgentCall.call(o, RequestCmd.OBJECT_THREAD_DUMP, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.OBJECT_THREAD_CONTROL)
  def agentThreadControl(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");

    val o = AgentManager.getAgent(objHash);
    val p = AgentCall.call(o, RequestCmd.OBJECT_THREAD_CONTROL, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }
}