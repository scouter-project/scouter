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
import scouter.lang.pack.StatusPack
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.db.StatusRD
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler

import scouter.server.db.StatusRD
import scouter.server.db.StatusRD
import scouter.server.db.StatusRD

class DatabaseService {

    @ServiceHandler(RequestCmd.ACTIVE_QUERY_LIST)
    def activeQueryList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.ACTIVE_QUERY_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.EXIST_QUERY_LIST)
    def existQueryList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val yyyymmdd = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val pack = new MapPack();
        val timeLv = pack.newList("time");
        val handler = (time: Long, data: Array[Byte]) => {
            val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
            if (pk.objHash == objHash) {
                timeLv.add(time);
            }
        }

        StatusRD.readByTime(yyyymmdd, stime, etime, handler)

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(pack);
    }

    @ServiceHandler(RequestCmd.LOAD_QUERY_LIST)
    def loadQueryList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val yyyymmdd = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val handler = (time: Long, data: Array[Byte]) => {
            val pack = new DataInputX(data).readPack().asInstanceOf[StatusPack];
            if (pack.objHash == objHash) {
                dout.writeByte(TcpFlag.HasNEXT)
                dout.writePack(pack.toMapPack())
            }
        }

        StatusRD.readByTime(yyyymmdd, stime, etime, handler)
    }

    @ServiceHandler(RequestCmd.LOCK_LIST)
    def lockList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.LOCK_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DB_PROCESS_DETAIL)
    def processDetail(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DB_PROCESS_DETAIL, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DB_EXPLAIN_PLAN)
    def explainPlan(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DB_EXPLAIN_PLAN, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DB_PROCESS_LIST)
    def processList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DB_PROCESS_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DB_VARIABLES)
    def variables(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DB_VARIABLES, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.DB_KILL_PROCESS)
    def killProcess(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.DB_KILL_PROCESS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.GET_QUERY_INTERVAL)
    def getIntervalStatusTask(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.GET_QUERY_INTERVAL, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.SET_QUERY_INTERVAL)
    def changeIntervalStatusTask(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.SET_QUERY_INTERVAL, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.SCHEMA_SIZE_STATUS)
    def schemaSizeStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.SCHEMA_SIZE_STATUS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.TABLE_SIZE_STATUS)
    def tableSizeStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.TABLE_SIZE_STATUS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.INNODB_STATUS)
    def innodbStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.INNODB_STATUS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.SLAVE_STATUS)
    def slaveStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.SLAVE_STATUS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.EXPLAIN_PLAN_FOR_THREAD)
    def explainPlanForThread(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.EXPLAIN_PLAN_FOR_THREAD, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.USE_DATABASE)
    def useDatabase(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.USE_DATABASE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
}