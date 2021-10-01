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
import scouter.lang.pack.Pack
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler

class DumpService {

    @ServiceHandler(RequestCmd.TRIGGER_ACTIVE_SERVICE_LIST)
    def triggerActiveServiceList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_ACTIVE_SERVICE_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TRIGGER_HEAPHISTO)
    def triggerHeapHisto(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_HEAPHISTO, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TRIGGER_THREAD_DUMP)
    def triggerThreadDump(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_THREAD_DUMP, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TRIGGER_THREAD_LIST)
    def triggerThreadList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_THREAD_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TRIGGER_BLOCK_PROFILE)
    def triggerBlockProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_BLOCK_PROFILE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TRIGGER_MUTEX_PROFILE)
    def triggerMutexrofile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TRIGGER_MUTEX_PROFILE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_DUMP_FILE_LIST)
    def getDumpFileList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.OBJECT_DUMP_FILE_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_DUMP_FILE_DETAIL)
    def getDumpFileDetail(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);

        val handler = (proto: Int, _in: DataInputX, _out: DataOutputX) => {
            while (_in.readByte() == TcpFlag.HasNEXT) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writeBlob(_in.readBlob());
            }
        }
        AgentCall.call(o, RequestCmd.OBJECT_DUMP_FILE_DETAIL, param, handler)
    }

    @ServiceHandler(RequestCmd.OBJECT_SYSTEM_GC)
    def systemGc(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        AgentCall.call(o, RequestCmd.OBJECT_SYSTEM_GC, param);
    }

    @ServiceHandler(RequestCmd.DUMP_APACHE_STATUS)
    def dumpApacheStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DUMP_APACHE_STATUS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
}
