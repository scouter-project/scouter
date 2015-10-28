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

import scala.collection.JavaConversions._
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.MapPack
import scouter.lang.value.DoubleValue
import scouter.lang.value.ListValue
import scouter.lang.value.MapValue
import scouter.lang.value.Value
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.db.ObjectRD
import scouter.server.db.RealtimeCounterRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.TimedSeries
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.StringUtil
import scouter.net.RequestCmd

class CounterService {

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME)
    def getRealTime(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val key = CounterKey.toCounterKey(param);
        val v = CounterCache.get(key);

        if (v != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeValue(v);
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_ALL)
    def getRealTimeAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val counter = param.getText("counter");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_REAL_TIME_ALL objType is null");
            return ;
        }
        val insts = AgentManager.getLiveObjHashList(objType);

        val mpack = new MapPack();
        val instList = mpack.newList("objHash");
        val values = mpack.newList("value");

        for (objHash <- insts) {
            val key = new CounterKey(objHash.intValue(), counter, TimeTypeEnum.REALTIME);
            val v = CounterCache.get(key);
            if (v != null) {
                instList.add(objHash.intValue());
                values.add(v);
            }
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_MULTI)
    def getRealTimeMulti(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
	    val objHash = param.getInt("objHash");
	    val counterLv = param.getList("counter");
	
	    val out = new MapPack();
	    val counterOut = out.newList("counter");
	    val valueOut = out.newList("value");
	
	    for (i <- 0 to counterLv.size() - 1) {
	      val key = new CounterKey(objHash, counterLv.getString(i), TimeTypeEnum.REALTIME);
	      val v = CounterCache.get(key);
	      if (v != null) {
	        counterOut.add(counterLv.get(i));
	        valueOut.add(v);
	      }
	    }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(out);
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_OBJECT_ALL)
    def getRealTimeObjectAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val cntMap = CounterCache.getObjectCounters(objHash);

        val mpack = new MapPack();
        val counters = mpack.newList("counter");
        val values = mpack.newList("value");

        for (counter <- cntMap.keySet()) {
            val value = cntMap.get(counter);
            counters.add(counter);
            values.add(value);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_OBJECT_TYPE_ALL)
    def getRealTimeObjectTypeAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");

        val liveObjectList = AgentManager.getLiveObjHashList(objType);
        val mpack = new MapPack();
        for (objHash <- liveObjectList) {
            val mapValue = new MapValue();
            val cntMap = CounterCache.getObjectCounters(objHash.intValue());
            for (counter <- cntMap.keySet()) {
                val value = cntMap.get(counter);
                mapValue.put(counter, value);
                mpack.put(AgentManager.getAgentName(objHash.intValue()), mapValue);
            }
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_TOT)
    def getRealTimeTotal(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val inout = din.readPack().asInstanceOf[MapPack];
        val objType = inout.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_REAL_TIME_TOT objType is null");
            return ;
        }
        val counter = inout.getText("counter");
        val mode = inout.getText("mode");

        var vvv = 0.0;
        var cnt = 0;
        val insList = AgentManager.getLiveObjHashList(objType);
        for (objHash <- insList) {
            val key = new CounterKey(objHash, counter, TimeTypeEnum.REALTIME);
            val v = CounterCache.get(key);
            if (v != null) {
                vvv += CastUtil.cdouble(v);
                cnt += 1;
            }
        }
        if (cnt > 0 && "avg".equalsIgnoreCase(mode)) {
            vvv = vvv / cnt;
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new DoubleValue(vvv));
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_TIME)
    def getPastTime(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        
        val objHash = param.getInt("objHash");
        val counter = param.getText("counter");

        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        val date = DateUtil.yyyymmdd(stime);
        val objName = getObjName(date, objHash);
        /////////////////RealtimeCounterRD////////////////////////

        val handler = (time: Long, data: MapValue) => {
            val value = data.get(counter);
            if (value != null) {
                timeLv.add(time);
                valueLv.add(value);
            }
        }

        RealtimeCounterRD.read(objName, date, stime, etime, handler)

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_TIME_ALL)
    def getPastTimeAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val counter = param.getText("counter");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_LOAD_TIME_ALL objType is null");
            return ;
        }
        val date = DateUtil.yyyymmdd(stime);
        val agentGrp = AgentManager.getDailyObjects(date, objType);

        val objHashLv = agentGrp.getList("objHash");

        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = objHashLv.getInt(i);
            val mpack = new MapPack();
            mpack.put("objHash", objHash);
            val timeLv = mpack.newList("time");
            val valueLv = mpack.newList("value");

            val objName = getObjName(date, objHash);

            /////////////////RealtimeCounterRD////////////////////////
            val handler = (time: Long, data: MapValue) => {
                val value = data.get(counter);
                if (value != null) {
                    timeLv.add(time);
                    valueLv.add(value);
                }
                true;
            }

            RealtimeCounterRD.read(objName, date, stime, etime, handler)
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
            dout.flush();
        }
    }

    def getObjName(date: String, objHash: Int): String = {
        return ObjectRD.getObjName(date, objHash);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_TIME_TOT)
    def getPastTimeTotal(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val counter = param.getText("counter");
        val objType = param.getText("objType");

        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_PAST_TIME_TOT objType is null");
            return ;
        }

        val mode = param.getText("mode");
        val date = DateUtil.yyyymmdd(stime);

        val agentGrp = AgentManager.getDailyObjects(date, objType);
        val objHashLv = agentGrp.getList("objHash");

        val series = new TimedSeries[Integer, Double]();

        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = objHashLv.getInt(i);
            val objName = getObjName(date, objHash);
            /////////////////RealtimeCounterRD////////////////////////
            val handler = (time: Long, data: MapValue) => {
                val value = data.get(counter);
                if (value != null) {
                    series.add(objHash, time, CastUtil.cdouble(value));
                }
                true
            }

            RealtimeCounterRD.read(objName, date, stime, etime, handler)
        }
        series.addEnd();

        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        var minTime = series.getMinTime();
        val maxTime = series.getMaxTime();
        while (minTime <= maxTime) {
            var sum = 0.0d;
            val list = series.getInTimeList(minTime, 10000);
            for (i <- list) {
                sum += i;
            }
            timeLv.add(minTime);
            if ("avg".equals(mode)) {
                if (list.size() > 0) {
                    valueLv.add(sum / list.size());
                } else {
                    valueLv.add(0);
                }
            } else {
                valueLv.add(sum);
            }
            minTime += (DateUtil.MILLIS_PER_SECOND * 2);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
        dout.flush();
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_TIME_GROUP)
    def getPastTimeGroupAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHashLv = param.getList("objHash");
        val counter = param.getText("counter");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val date = DateUtil.yyyymmdd(stime);
        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = objHashLv.getInt(i);
            val objName = getObjName(date, objHash);
            val mpack = new MapPack();
            mpack.put("objHash", objHash);
            val timeLv = mpack.newList("time");
            val valueLv = mpack.newList("value");

            val handler = (time: Long, data: MapValue) => {
                val value = data.get(counter);
                if (value != null) {
                    timeLv.add(time);
                    valueLv.add(value);
                }
                true;
            }

            RealtimeCounterRD.read(objName, date, stime, etime, handler)

            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_REAL_TIME_GROUP)
    def getRealTimeGroupAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val counter = param.getText("counter");
        val objHashLv = param.getList("objHash");

        val mpack = new MapPack();
        val instList = mpack.newList("objHash");
        val values = mpack.newList("value");

        for (i <- 0 to objHashLv.size() - 1) {
            val key = new CounterKey(objHashLv.getInt(i), counter, TimeTypeEnum.REALTIME);
            val v = CounterCache.get(key);
            if (v != null) {
                instList.add(objHashLv.get(i));
                values.add(v);
            }
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }
}