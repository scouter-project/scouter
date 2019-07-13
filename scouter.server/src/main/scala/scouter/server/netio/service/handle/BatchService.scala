/*
*  Copyright 2016 the original author or authors. 
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

package scouter.server.netio.service.handle

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.MapPack
import scouter.lang.pack.BatchPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.value.BlobValue
import scouter.lang.value.BooleanValue
import scouter.lang.value.MapValue
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.CounterManager
import scouter.server.core.AgentManager
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.StringKeyLinkedMap.StringKeyLinkedEntry
import scouter.server.util.EnumerScala
import scouter.server.db.BatchDB
import scouter.server.db.BatchZipDB
import scala.collection.JavaConversions._

class BatchService {
    @ServiceHandler(RequestCmd.BATCH_HISTORY_LIST)
    def read(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack]
        val objHash = param.getInt("objHash")
        val objInfo = AgentManager.getAgent(objHash)
        if(objInfo == null){
          return;
        }
        val objName = objInfo.objName
        val filter = param.getText("filter")
        var response = param.getLong("response")
        var from = param.getLong("from")
        var to = param.getLong("to")
        
        val handler = (time: Long, data: BatchPack) => {
            dout.writeByte(TcpFlag.HasNEXT);
            data.writeSimple(dout);
        }

        if (from > 0 && to > from) {
           BatchDB.read(objName, from, to, filter, response, handler)
        }
    }

    @ServiceHandler(RequestCmd.BATCH_HISTORY_DETAIL)
    def readDetail(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack]
        val objHash = param.getInt("objHash")
        val objInfo = AgentManager.getAgent(objHash)
        if(objInfo == null){
          return;
        }
        val objName = objInfo.objName

        val data = BatchDB.read(objName, (param.getLong("startTime") + param.getLong("elapsedTime")), param.getLong("position"))
        if(data != null){
        	val ins = new DataInputX(data)
        	val pack = ins.readPack();
        	dout.writeByte(TcpFlag.HasNEXT);
        	dout.writePack(pack);
        }
    } 

    @ServiceHandler(RequestCmd.BATCH_HISTORY_STACK)
    def readStack(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack]
        val objHash = param.getInt("objHash")
        val objInfo = AgentManager.getAgent(objHash)
        if(objInfo == null){
          return;
        }
        val objName = objInfo.objName
        
        BatchZipDB.read(objName, param.getLong("time"), param.getText("filename"), dout) 
	}
    
  @ServiceHandler(RequestCmd.OBJECT_BATCH_ACTIVE_LIST)
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
        val p = AgentCall.call(o, RequestCmd.OBJECT_BATCH_ACTIVE_LIST, param);
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
      val p = AgentCall.call(o, RequestCmd.OBJECT_BATCH_ACTIVE_LIST, param);
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
  
   @ServiceHandler(RequestCmd.BATCH_ACTIVE_STACK)
   def readActiveStack(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readPack().asInstanceOf[MapPack];
    val objHash = param.getInt("objHash");
    val o = AgentManager.getAgent(objHash);
    val p = AgentCall.call(o, RequestCmd.BATCH_ACTIVE_STACK, param);
    if (p != null) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(p);
    }
	} 
}