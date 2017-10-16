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

import scouter.lang.pack.AlertPack
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.value.DecimalValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.db.AlertRD
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.DateUtil

class ServerDebug {

    @ServiceHandler(RequestCmd.DEBUG_AGENT)
    def debugAgent(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");

        val agent = AgentManager.getAgent(objHash);

        val p = AgentCall.call(agent, RequestCmd.DEBUG_AGENT, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DEBUG_SERVER)
    def debugServer(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();

        var cnt = new DecimalValue();

        val handler = (time: Long, data: Array[Byte]) => {
            val p = new DataInputX(data).readPack().asInstanceOf[AlertPack];
            cnt.value += 1;
        }
        AlertRD.readByTime(DateUtil.yyyymmdd(), handler)
        val p = new MapPack();
        p.put("msg", "processed " + cnt);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
    }
}