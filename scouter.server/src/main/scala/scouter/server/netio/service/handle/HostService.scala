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

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler

class HostService {
  @ServiceHandler(RequestCmd.HOST_TOP)
  def hostTop(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");
    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_TOP, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.HOST_PROCESS_DETAIL)
  def processDetail(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");
    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_PROCESS_DETAIL, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.HOST_DISK_USAGE)
  def diskUsage(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");
    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_DISK_USAGE, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.HOST_NET_STAT)
  def netStat(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");

    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_NET_STAT, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.HOST_WHO)
  def who(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");
    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_WHO, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }

  @ServiceHandler(RequestCmd.HOST_MEMINFO)
  def memInfo(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHash = param.getInt("objHash");
    val agent = AgentManager.getAgent(objHash);

    val p = AgentCall.call(agent, RequestCmd.HOST_MEMINFO, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
  }
}