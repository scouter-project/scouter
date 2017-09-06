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
import scouter.lang.counters.CounterConstants
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.value.DecimalValue
import scouter.lang.value.FloatValue
import scouter.lang.value.ListValue
import scouter.lang.value.NumberValue
import scouter.lang.value.Value
import scouter.lang.value.ValueEnum
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.CounterManager
import scouter.server.core.AgentManager
import scouter.server.core.cache.CounterCache
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.CastUtil
import scala.collection.JavaConversions._
import scouter.server.util.EnumerScala

class SpecialCounterService {

    class ActiveSpeedData {
        var act1: Int = 0;
        var act2: Int = 0;
        var act3: Int = 0;
        var tps: Float = 0;
    }

    /**
      * search realtime running time-stepped active service count by objType
      */
    @ServiceHandler(RequestCmd.ACTIVESPEED_REAL_TIME)
    def getActiveSpeedAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objType = param.getText("objType");

        EnumerScala.forward(AgentManager.getObjList(objType), (spec: ObjectPack) => {
            val dat = new ActiveSpeedData();
            val v = CounterCache.get(
                new CounterKey(spec.objHash, CounterConstants.WAS_ACTIVE_SPEED, TimeTypeEnum.REALTIME));
            if (v != null) {
                val arrv = v.asInstanceOf[ListValue];
                if (arrv.size() >= 3) {
                    dat.act1 = arrv.getInt(0);
                    dat.act2 = arrv.getInt(1);
                    dat.act3 = arrv.getInt(2);
                }
            }
            val map = new MapPack();
            map.put("objHash", spec.objHash);
            map.put("act1", dat.act1);
            map.put("act2", dat.act2);
            map.put("act3", dat.act3);
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(map);
        })
    }

    @ServiceHandler(RequestCmd.ACTIVESPEED_GROUP_REAL_TIME)
    def getActiveSpeedGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHashLv = param.getList("objHash");

        EnumerScala.foreach[Value](objHashLv, (objHash: Value) => {
            val dat = new ActiveSpeedData();
            val v = CounterCache.get(
                new CounterKey(CastUtil.cint(objHash), CounterConstants.WAS_ACTIVE_SPEED, TimeTypeEnum.REALTIME));
            if (v != null) {
                val arrv = v.asInstanceOf[ListValue]
                if (arrv.size() >= 3) {
                    dat.act1 = arrv.getInt(0);
                    dat.act2 = arrv.getInt(1);
                    dat.act3 = arrv.getInt(2);
                }
            }
            val map = new MapPack();
            map.put("objHash", objHash);
            map.put("act1", dat.act1);
            map.put("act2", dat.act2);
            map.put("act3", dat.act3);
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(map);
        })
    }

    @ServiceHandler(RequestCmd.ACTIVESPEED_REAL_TIME_GROUP)
    def getActiveSpeed(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objType = param.getText("objType");

        val dat = new ActiveSpeedData();

        EnumerScala.forward(AgentManager.getObjList(objType), (spec: ObjectPack) => {
            if (spec.alive == true) {
                var v = CounterCache.get(
                    new CounterKey(spec.objHash, CounterConstants.WAS_TPS, TimeTypeEnum.REALTIME));
                if (v != null) {
                    dat.tps += CastUtil.cfloat(v);
                }
                v = CounterCache.get(
                    new CounterKey(spec.objHash, CounterConstants.WAS_ACTIVE_SPEED, TimeTypeEnum.REALTIME));
                if (v != null) {
                    val arrv = v.asInstanceOf[ListValue]
                    if (arrv.size() >= 3) {
                        dat.act1 += arrv.getInt(0);
                        dat.act2 += arrv.getInt(1);
                        dat.act3 += arrv.getInt(2);
                    }

                }
            }
        })
        val map = new MapPack();
        map.put("act1", dat.act1);
        map.put("act2", dat.act2);
        map.put("act3", dat.act3);
        map.put("tps", new FloatValue(dat.tps));

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.ACTIVESPEED_GROUP_REAL_TIME_GROUP)
    def getGroupActiveSpeed(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHashLv = param.getList("objHash");

        val dat = new ActiveSpeedData();

        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = CastUtil.cint(objHashLv.get(i));
            val spec = AgentManager.getAgent(objHash);
            if (spec != null && spec.alive == true) {
                var v = CounterCache.get(
                    new CounterKey(spec.objHash, CounterConstants.WAS_TPS, TimeTypeEnum.REALTIME));
                if (v != null) {
                    dat.tps += CastUtil.cfloat(v);
                }
                v = CounterCache.get(
                    new CounterKey(spec.objHash, CounterConstants.WAS_ACTIVE_SPEED, TimeTypeEnum.REALTIME));
                if (v != null) {
                    val arrv = v.asInstanceOf[ListValue]
                    if (arrv.size() >= 3) {
                        dat.act1 += arrv.getInt(0);
                        dat.act2 += arrv.getInt(1);
                        dat.act3 += arrv.getInt(2);
                    }
                }
            }
        }
        val map = new MapPack();
        map.put("act1", dat.act1);
        map.put("act2", dat.act2);
        map.put("act3", dat.act3);
        map.put("tps", new FloatValue(dat.tps));

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.SHOW_REAL_TIME_STRING)
    def showRealtimeString(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objType = param.getText("objType");
        val counter = param.getText("counter");

        val map = new MapPack();
        var desc = "";

        var v: NumberValue = null;
        val insList = AgentManager.getLiveObjHashList(objType);
        for (ins <- insList) {
            val ck = new CounterKey(ins, counter, TimeTypeEnum.REALTIME);
            if (v == null) {
                v = CounterCache.get(ck).asInstanceOf[NumberValue];
            } else {
                v.add(CounterCache.get(ck).asInstanceOf[NumberValue]);
            }
        }
        if (v == null) {
            v = new DecimalValue(0);
        }
        desc = counter + " of " + CounterManager.getInstance().getCounterEngine().getDisplayNameObjectType(objType);
        map.put("desc", desc);
        map.put("result", v);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.COUNTER_MAP_REAL_TIME)
    def counterMapRealtime(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHashLv = param.getList("objHash");
        val counter = param.getText("counter");
        for (i <- 0 to objHashLv.size() - 1) {
            val objHash = objHashLv.getInt(i);
            val key = new CounterKey(objHash, counter, TimeTypeEnum.REALTIME);
            val v = CounterCache.get(key);
            if (v.getValueType() == ValueEnum.MAP) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writeValue(v);
            }
        }
    }
}