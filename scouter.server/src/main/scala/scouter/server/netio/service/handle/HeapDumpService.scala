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

import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler

class HeapDumpService {

    @ServiceHandler(RequestCmd.OBJECT_CALL_HEAP_DUMP)
    def callHepDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_CALL_HEAP_DUMP, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_LIST_HEAP_DUMP)
    def listHepDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_LIST_HEAP_DUMP, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_DOWNLOAD_HEAP_DUMP)
    def downloadHepDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        AgentCall.call(o, RequestCmd.OBJECT_DOWNLOAD_HEAP_DUMP, param, (proto: Int, inx: DataInputX, out: DataOutputX) =>
            while (inx.readByte() == TcpFlag.HasNEXT) {
                dout.writeByte(TcpFlag.HasNEXT)
                val buff = inx.readBlob()
                dout.writeBlob(buff)
            })

    }

    @ServiceHandler(RequestCmd.OBJECT_DELETE_HEAP_DUMP)
    def deleteHepDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_DELETE_HEAP_DUMP, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_CALL_BLOCK_PROFILE)
    def callBlockProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_CALL_BLOCK_PROFILE, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_CALL_MUTEX_PROFILE)
    def callMutexProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_CALL_MUTEX_PROFILE, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_CALL_CPU_PROFILE)
    def callCpuProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val mpack = AgentCall.call(o, RequestCmd.OBJECT_CALL_CPU_PROFILE, param);
        if (mpack != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

}
