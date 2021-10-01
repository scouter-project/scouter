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
 */
package scouter.server.netio.service.handle;

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import scouter.lang.CounterKey
import scouter.lang.DigestKey
import scouter.lang.TimeTypeEnum
import scouter.lang.constants.StatusConstants
import scouter.lang.pack.MapPack
import scouter.lang.pack.StatusPack
import scouter.lang.value.DoubleValue
import scouter.lang.value.FloatValue
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.core.cache.StatusCache
import scouter.server.db.DailyCounterRD
import scouter.server.db.StatusRD
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.ArrayUtil
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.net.RequestCmd
import scouter.server.Logger
import scouter.util.{CastUtil, DateUtil, IntKeyMap, StringUtil}
import scouter.server.util.TimedSeries
import scouter.server.db.{ObjectRD, RealtimeCounterRD}
import scouter.lang.counters.CounterConstants
import scala.collection.JavaConversions._
import scouter.util.HashUtil
import scouter.util.DataUtil

class CubridService {
  @ServiceHandler(RequestCmd.CUBRID_DB_REALTIME_DML)
  def realtimeDML(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    if (objHashLv.size() == 0) {
      return;
    }
    var dbname = "";
    var select = 0L;
    var update = 0L;
    var insert = 0L;
    var delete = 0L;
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val key1 = new CounterKey(objHash, "num_query_selects", TimeTypeEnum.REALTIME);
      val v1 = CounterCache.get(key1);
      if (v1 != null) {
        select += CastUtil.clong(v1);
      }
      val key2 = new CounterKey(objHash, "num_query_updates", TimeTypeEnum.REALTIME);
      val v2 = CounterCache.get(key2);
      if (v2 != null) {
        update += CastUtil.clong(v2);
      }
      val key3 = new CounterKey(objHash, "num_query_inserts", TimeTypeEnum.REALTIME);
      val v3 = CounterCache.get(key3);
      if (v3 != null) {
        insert += CastUtil.clong(v3);
      }
      val key4 = new CounterKey(objHash, "num_query_deletes", TimeTypeEnum.REALTIME);
      val v4 = CounterCache.get(key4);
      if (v4 != null) {
        delete += CastUtil.clong(v4);
      }
      val key5 = new CounterKey(objHash, "db_num", TimeTypeEnum.REALTIME);
      val v5 = CounterCache.get(key5);
      if (v5 != null) {
        dbname += CastUtil.clong(v5);
      }
    }

    val value = new MapValue();
    value.put("dbname", dbname);
    value.put("select", select);
    value.put("update", update);
    value.put("insert", insert);
    value.put("delete", delete);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(value);
  }
 
  @ServiceHandler(RequestCmd.CUBRID_ACTIVE_DB_LIST)
  def realtimeActiveDBList(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {

    val param = din.readPack().asInstanceOf[MapPack];
    val objHashLv = param.getList("objHash");
    val key = param.getText("key");
    val time = param.getLong("time");
    
    val date = DateUtil.yyyymmdd(time);
    
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val status = StatusCache.get(objHash, key);
       if(status != null) {
         dout.writeByte(TcpFlag.HasNEXT);
         dout.writePack(status);
       }
    }
  }
  
  @ServiceHandler(RequestCmd.CUBRID_DB_REALTIME_STATUS)
  def realtimeDMLStatus(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readPack().asInstanceOf[MapPack];
    val objHashLv = param.getList("objHash");
    val key = param.getText("key");
    val date = param.getText("date");
    val etime = param.getLong("etime");
    val stime = param.getLong("stime");
    val time = param.getLong("time");
    val objArray = new ArrayList[Int]();
    
    for (i <- 0 to objHashLv.size() - 1) {
      val p = new MapPack();
      val objHash = objHashLv.getInt(i);
      objArray.add(objHash);
    }
    
    val handler = (time: Long, data: Array[Byte]) => {
      val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
      
        if (objArray.contains(pk.objHash) && pk.key == key) {
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(pk);
          return
        }
    }

    for (i <- 0 to objArray.size() - 1) {
      StatusRD.readFromEndTime(date, stime, etime, handler);
    }
   
  }
  
  @ServiceHandler(RequestCmd.CUBRID_DB_SERVER_INFO)
  def DbServerInfo(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readPack().asInstanceOf[MapPack];
    val objHashLv = param.getList("objHash");
    val key = param.getText("key");
    val date = param.getText("date");
    val etime = param.getLong("etime");
    val stime = param.getLong("stime");
    val time = param.getLong("time");
    val objArray = new ArrayList[Int]();
    
    for (i <- 0 to objHashLv.size() - 1) {
      val p = new MapPack();
      val objHash = objHashLv.getInt(i);
      objArray.add(objHash);
    }
    
    val handler = (time: Long, data: Array[Byte]) => {
      val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
      
        if (objArray.contains(pk.objHash) && pk.key == key) {
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(pk);
          return
        }
    }
    
    for (i <- 0 to objHashLv.size() - 1) {
      StatusRD.readFromEndTime(date, stime, etime, handler);
    }
   
  }

   @ServiceHandler(RequestCmd.CUBRID_DB_PERIOD_MULTI_DATA)
  def realtimeCouterTest(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
   val param = din.readPack().asInstanceOf[MapPack];
        val objHashLv = param.getList("objHash");
        val counter = param.getText("counter");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val date = DateUtil.yyyymmdd(stime);
        val objectName = param.getText("objName");

        val mapPackMap = new IntKeyMap[MapPack]();
        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = objHashLv.getInt(i);
            val mapPack = new MapPack();
            mapPack.put("objHash", objHash);
            val timeLv = mapPack.newList("time");
            val valueLv = mapPack.newList("value");
            mapPackMap.put(objHash, mapPack);
        }

        val handler = (mapValue: MapValue) => {
            if (mapValue != null) {
                val objHash = mapValue.getInt(CounterConstants.COMMON_OBJHASH)
                val time = mapValue.getLong(CounterConstants.COMMON_TIME)
                val value = mapValue.get(counter)
                if(value != null) {
                    val curMapPack = mapPackMap.get(objHash)
                    if(curMapPack != null) {
                        curMapPack.getList("time").add(time)
                        curMapPack.getList("value").add(value)
                    }
                }
            }
        }

        RealtimeCounterRD.readBulk(date, stime, etime, handler)

        for (i <- 0 to objHashLv.size() - 1) {
            dout.writeByte(TcpFlag.HasNEXT);
            var mpack = mapPackMap.get(objHashLv.getInt(i))
            dout.writePack(mpack)
            dout.flush()
        }
   }
   
   
   @ServiceHandler(RequestCmd.CUBRID_DB_REALTIME_MULTI_DATA)
  def realtimeMultiData(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val counter = param.getText("counter");
    
    if (objHashLv.size() == 0) {
      return;
    }
    var counterData = 0L;
    
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val key1 = new CounterKey(objHash, counter, TimeTypeEnum.REALTIME);
      val v1 = CounterCache.get(key1);
      if (v1 != null) {
        counterData += CastUtil.clong(v1);
      }
    }
    
    val value = new MapValue();
    value.put(counter, counterData);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(value);
  }
   
     @ServiceHandler(RequestCmd.CUBRID_DB_LONG_PERIOD_MULTI_DATA)
    def getPastLongDateAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val sDate = param.getText("sDate");
        val eDate = param.getText("eDate");
        val objHashLv = param.getList("objHash");

        var stime = DateUtil.yyyymmdd(sDate);
        var etime = DateUtil.yyyymmdd(eDate) + DateUtil.MILLIS_PER_DAY;

        var date = stime;
        while (date <= (etime - DateUtil.MILLIS_PER_DAY)) {
            val d = DateUtil.yyyymmdd(date);
            for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
                val objHash = objHashLv.getInt(i);

                try {
                  val mpack = new MapPack();
                  mpack.put("objHash", objHash);
                  val timeLv = mpack.newList("time");
                  val valueLv = mpack.newList("value");
  
                  val v = DailyCounterRD.getValues(d, new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN));
  
                  for (j <- 0 to ArrayUtil.len(v) - 1) {
                      val time = date + DateUtil.MILLIS_PER_MINUTE * 5 * j;
                      timeLv.add(time);
                      valueLv.add(v(j));
                  }
                  dout.writeByte(TcpFlag.HasNEXT);
                  dout.writePack(mpack);
                  dout.flush();
                } catch {
                   case e: Throwable =>
                    val op = AgentManager.getAgent(objHash);
                    println(op.objName + " invalid data : " + e.getMessage())
                    e.printStackTrace()
                }
            }
            date += DateUtil.MILLIS_PER_DAY;
        };

    }
   
  def getObjName(date: String, objHash: Int): String = {
      return ObjectRD.getObjName(date, objHash);
  }
    
  @ServiceHandler(RequestCmd.CUBRID_DB_LONG_TRANSACTION_DATA)
  def DbLongTransactionInfo(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readPack().asInstanceOf[MapPack];
    val objHashLv = param.getList("objHash");
    val key = param.getText("key");
    val date = param.getText("date");
    val etime = param.getLong("etime");
    val stime = param.getLong("stime");
    val time = param.getLong("time");
    val objArray = new ArrayList[Int]();
    
    for (i <- 0 to objHashLv.size() - 1) {
      val p = new MapPack();
      val objHash = objHashLv.getInt(i);
      objArray.add(objHash);
    }
    
    val handler = (time: Long, data: Array[Byte]) => {
      val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
      
        if (objArray.contains(pk.objHash) && pk.key == key) {
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(pk);
          return
        }
    }
    
    for (i <- 0 to objHashLv.size() - 1) {
      StatusRD.readFromEndTime(date, stime, etime, handler);
    }
   
  }
  
   @ServiceHandler(RequestCmd.CUBRID_GET_ALERT_CONFIGURE)
    def setConfigureAgent(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.CUBRID_GET_ALERT_CONFIGURE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.CUBRID_SET_ALERT_CONFIGURE)
    def listConfigureWas(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.CUBRID_SET_ALERT_CONFIGURE, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }
  
  
}

