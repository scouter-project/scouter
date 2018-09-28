/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.server.netio.service.handle

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.counters.CounterConstants
import scouter.lang.pack.{InteractionPerfCounterPack, MapPack}
import scouter.lang.value.{DoubleValue, MapValue}
import scouter.lang.{CounterKey, TimeTypeEnum}
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.core.AgentManager
import scouter.server.core.cache.{CounterCache, InteractionCounterCache}
import scouter.server.db.{KeyValueStoreRW, ObjectRD, RealtimeCounterRD}
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.{EnumerScala, TimedSeries}
import scouter.util.{CastUtil, DateUtil, IntKeyMap, StringUtil}

import scala.collection.JavaConversions._

class InteractionCounterService {

    /**
      * get latest several counter's values for specific object type
      * @param din MapPack{counter[], (objType or objHashLv)}
      * @param dout MapPack{objHash[], counter[], value[]}
      * @param login
      */
    @ServiceHandler(RequestCmd.INTR_COUNTER_REAL_TIME_BY_OBJ)
    def getRealTimeByObj(din: DataInputX, dout: DataOutputX, login: Boolean) {
      val param = din.readPack().asInstanceOf[MapPack]
      val objType = param.getText("objType")
      val objHashLv = param.getList("objHash")

      if (StringUtil.isEmpty(objType) && (objHashLv == null || objHashLv.size() == 0)) {
          System.out.println("please check.. INTR_COUNTER_REAL_TIME_BY_OBJ objType is null")
          return
      }
      var insts = AgentManager.getLiveObjHashList(objType)
      if(objHashLv != null && objHashLv.size() > 0) {
        insts.clear()
        for( i <- 0 until objHashLv.size()) {
          insts.add(objHashLv.getInt(i))
        }
      }

      for(objHash <- insts) {
          val cacheTable = InteractionCounterCache.getCacheTable(objHash)
          EnumerScala.foreach(cacheTable.values(), (pack: InteractionPerfCounterPack) => {
              dout.writeByte(TcpFlag.HasNEXT)
              dout.writePack(pack)
          })
      }
    }
}