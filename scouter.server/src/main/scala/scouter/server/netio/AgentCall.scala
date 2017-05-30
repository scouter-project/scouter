/*
  Copyright 2015 the original author or authors. 
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

package scouter.server.netio;

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.pack.PackEnum
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.SocketAddr
import scouter.net.TcpFlag
import scouter.server.Logger
import scouter.server.logs.RequestLogger
import scouter.util.FileUtil
import scouter.server.netio.service.net.TcpAgentManager

object AgentCall {

    def call(o: ObjectPack, cmd: String, param: MapPack): MapPack = {
        if (o == null) {
            Logger.println("S502", "Agent Call error. Object pack is null");
            return null;
        }

        val tcpAgent = TcpAgentManager.get(o.objHash);
        if (tcpAgent != null) {
            try {
                RequestLogger.getInstance().registerCmd(cmd);
                tcpAgent.write(cmd, if (param != null) param else new MapPack())
                var p: Pack = null;
                while (tcpAgent.readByte() == TcpFlag.HasNEXT) {
                    p = tcpAgent.readPack();
                }
                if (p == null) {
                    return null;
                }
                p.getPackType() match {
                    case PackEnum.MAP => return p.asInstanceOf[MapPack];
                    case _ => Logger.println("S149", "not allowed return : " + p);
                }
                return null

            } finally {
                TcpAgentManager.add(o.objHash, tcpAgent)
            }
        } else {
            Logger.println("S501", "Cannot find a tcp agent for " + o.objName);
        }

        return null;
    }

    def call(o: ObjectPack, cmd: String, param: MapPack, handler: (Int, DataInputX, DataOutputX) => Unit) {
        if (o == null) {
            Logger.println("S503", "Agent Call error. Object pack is null");
            return null;
        }
        val tcpAgent = TcpAgentManager.get(o.objHash);
        if (tcpAgent != null) {
            try {
            	  RequestLogger.getInstance().registerCmd(cmd);
                tcpAgent.write(cmd, if (param != null) param else new MapPack())
                tcpAgent.read(handler)
            } finally {
                TcpAgentManager.add(o.objHash, tcpAgent)
            }
        }else {
            Logger.println("S504", "Cannot find a tcp agent for " + o.objName);
        }
    }

}
