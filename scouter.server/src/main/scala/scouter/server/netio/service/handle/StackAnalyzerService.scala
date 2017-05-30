/*
 *  Copyright 2015 Scouter Project.
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
package scouter.server.netio.service.handle;

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.MapPack
import scouter.net.TcpFlag
import scouter.server.db.StackAnalyzerDB
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.net.RequestCmd
import scouter.util.DateUtil
import scouter.util.CastUtil
import scouter.server.netio.AgentCall
import scouter.server.core.AgentManager

class StackAnalyzerService {

    @ServiceHandler(RequestCmd.GET_STACK_ANALYZER)
    def read(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack];
        val objName = param.getText("objName");

        var from = param.getLong("from");
        var to = param.getLong("to");
        val handler = (time: Long, data: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.write(data);
        }

        if (from > 0 && to > from) {
            StackAnalyzerDB.read(objName, from, to, handler)
            return
        }
        val date = param.getText("date");
        val hour = CastUtil.cint(param.get("hour"))
        if (date != null) {
            from = DateUtil.yyyymmdd(date) + hour * 3600 * 1000
            to = from + 3600 * 1000
            StackAnalyzerDB.read(objName, from, to, handler)
            return
        }

    }
    
    @ServiceHandler(RequestCmd.GET_STACK_INDEX)
    def readIndex(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack];
        
        val objName = param.getText("objName");

        var from = param.getLong("from");
        var to = param.getLong("to");
        val handler = (time: Long) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeLong(time);
        }

        if (from > 0 && to > from) {
            StackAnalyzerDB.read(objName, from, to, handler)
            return
        }
        val date = param.getText("date");
        val hour = CastUtil.cint(param.get("hour"))
        if (date != null) {
            from = DateUtil.yyyymmdd(date) + hour * 3600 * 1000
            to = from + 3600 * 1000
            StackAnalyzerDB.read(objName, from, to, handler)
            return
        }

    }
    
     @ServiceHandler(RequestCmd.PSTACK_ON)
    def turnOnStack(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.PSTACK_ON, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
}
