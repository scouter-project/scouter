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

import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.counters.CounterEngine
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.value.DecimalValue
import scouter.lang.value.ListValue
import scouter.lang.value.Value
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.db.ObjectRD
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.DateUtil
import scala.collection.JavaConversions._
import java.util.ArrayList
import scouter.util.StringUtil
import java.util.Enumeration

class AgentInfo {

    @ServiceHandler(RequestCmd.OBJECT_REMOVE_INACTIVE)
    def agentRemoveInactive(din: DataInputX, dout: DataOutputX, login: Boolean) {
        AgentManager.clearInactive();
    }

    @ServiceHandler(RequestCmd.OBJECT_INFO)
    def getAgentInfo(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        var date = param.getText("date");
        val objHash = param.getInt("objHash");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }
        val objPack = ObjectRD.getObjectPack(date, objHash);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(objPack);
    }

    @ServiceHandler(RequestCmd.OBJECT_LIST_LOAD_DATE)
    def getAgentOldList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val date = param.getText("date");
        if (StringUtil.isEmpty(date))
            return ;

        val mpack = ObjectRD.getDailyAgent(date);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    /**
      * OBJECT_LIST_REAL_TIME
      * @param din none
      * @param dout ObjectPack[]
      * @param login
      */
    @ServiceHandler(RequestCmd.OBJECT_LIST_REAL_TIME)
    def agentList(din: DataInputX, dout: DataOutputX, login: Boolean) {

        val engine = scouter.server.CounterManager.getInstance().getCounterEngine();
        val en: java.util.Enumeration[ObjectPack] = AgentManager.getObjPacks();
        while (en.hasMoreElements()) {
            val p = en.nextElement()
            val ac = engine.getMasterCounter(p.objType);
            try {
                if (p.alive) {
                    val c = CounterCache.get(new CounterKey(p.objHash, ac, TimeTypeEnum.REALTIME));
                    if (c != null) {
                        p.tags.put("counter", c);
                    }
                }
            } catch {
                case e: Throwable => { e.printStackTrace() }
            }
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_ENV)
    def agentEnv(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val agent = AgentManager.getAgent(objHash);

        val p = AgentCall.call(agent, RequestCmd.OBJECT_ENV, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
    
    @ServiceHandler(RequestCmd.OBJECT_CLASS_DESC)
    def getClassDesc(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);

        val p = AgentCall.call(o, RequestCmd.OBJECT_CLASS_DESC, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_CLASS_LIST)
    def getLoadedClassList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        val p = AgentCall.call(o, RequestCmd.OBJECT_CLASS_LIST, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_RESET_CACHE)
    def resetAgentCache(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];

        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        AgentCall.call(o, RequestCmd.OBJECT_RESET_CACHE, param);
    }

    @ServiceHandler(RequestCmd.OBJECT_REMOVE)
    def agentRemove(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val objHashList = din.readValue().asInstanceOf[ListValue];
        val removeObjList = new ArrayList[Int](objHashList.size());
        for (i <- 0 to objHashList.size() - 1) {
            val objHash = objHashList.getLong(i).toInt
            removeObjList.add(objHash);
        }
        AgentManager.removeAgents(removeObjList, true);
        getFullAgentList(din, dout, login);

    }

    @ServiceHandler(RequestCmd.OBJECT_TODAY_FULL_LIST)
    def getFullAgentList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val date = DateUtil.yyyymmdd();
        val objectList = ObjectRD.getObjectList(date);
        for (pack <- objectList) {
            val inMemory = AgentManager.getAgent(pack.objHash);
            if (inMemory == null) {
                pack.tags.put("status", "dead");
                pack.alive = false;
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(pack);
                dout.flush();
            } else {
                if (inMemory.alive) {
                    inMemory.tags.put("status", "active");
                } else {
                    inMemory.tags.put("status", "inactive");
                }
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(inMemory);
                dout.flush();
            }
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_REMOVE_IN_MEMORY)
    def agentRemoveInMemory(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val objHashList = din.readValue().asInstanceOf[ListValue];
        val removeObjList = new ArrayList[Int](objHashList.size())
        for (i <- 0 to objHashList.size() - 1) {
            val objHash = objHashList.getLong(i).toInt
            removeObjList.add(objHash);
        }
        AgentManager.removeAgents(removeObjList, false);
        getFullAgentList(din, dout, login);

    }

    @ServiceHandler(RequestCmd.OBJECT_HEAPHISTO)
    def agentObjectHeapHisto(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        val p = AgentCall.call(o, RequestCmd.OBJECT_HEAPHISTO, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_FILE_SOCKET)
    def agentObjectFileSocket(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        val p = AgentCall.call(o, RequestCmd.OBJECT_FILE_SOCKET, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
    
    @ServiceHandler(RequestCmd.OBJECT_SOCKET)
    def agentObjectSocket(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);

        val p = AgentCall.call(o, RequestCmd.OBJECT_SOCKET, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_LOAD_CLASS_BY_STREAM)
    def loadClassStream(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.OBJECT_LOAD_CLASS_BY_STREAM, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.REDIS_INFO)
    def redisInfo(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.REDIS_INFO, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_CHECK_RESOURCE_FILE)
    def checkJarFile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.OBJECT_CHECK_RESOURCE_FILE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.OBJECT_DOWNLOAD_JAR)
    def downloadJar(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.OBJECT_DOWNLOAD_JAR, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
}