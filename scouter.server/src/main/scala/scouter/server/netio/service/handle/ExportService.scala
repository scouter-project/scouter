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
import scouter.lang.SummaryEnum
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.value.ListValue
import scouter.lang.value.MapValue
import scouter.lang.value.NullValue
import scouter.lang.value.Value
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.CounterManager
import scouter.server.Logger
import scouter.server.db.DailyCounterRD
import scouter.server.db.RealtimeCounterRD
import scouter.server.db.ObjectRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.SortUtil
import scouter.lang.value.TextValue

class ExportService {

    @ServiceHandler(RequestCmd.EXPORT_OBJECT_TIME_COUNTER)
    def exportObjectTimeCounter(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val date = param.getText("date");
        var stime = param.getLong("stime");
        var etime = param.getLong("etime");
        if (stime == 0) {
            stime = DateUtil.yyyymmdd(date);
            etime = stime + DateUtil.MILLIS_PER_DAY;
        }

        val objPack = ObjectRD.getObjectPack(date, objHash);
        val counterSet = CounterManager.getInstance().getCounterEngine().getCounterSet(objPack.objType);

        val objName = ObjectRD.getObjName(date, objHash);

        val counterArray = SortUtil.sort_string(counterSet.iterator(), counterSet.size());
        val pack = new MapPack();
        val titleLv = pack.newList("values");
        titleLv.add("time");
        for (key <- counterArray) {
            titleLv.add(key);
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(pack);
        Logger.println("EXPORT_OBJECT_TIME_COUNTER " + objName + "  " + date + " stime=" + stime + "  etime="
            + etime);

        val handler = (time: Long, data: MapValue) => {
            val pack1 = new MapPack();
            val valueLv = pack1.newList("values");
            valueLv.add(DateUtil.hhmmss(time));
            for (key <- counterArray) {
                val v = data.get(key);
                valueLv.add((if (v == null) new TextValue("") else v));
            }
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(pack1);
            true;
        }

        RealtimeCounterRD.read(objName, date, stime, etime, handler)

    }

    @ServiceHandler(RequestCmd.EXPORT_OBJECT_REGULAR_COUNTER)
    def exportObjectDateCounter(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val date = param.getText("date");
        val timeType = param.getInt("timeType").asInstanceOf[Byte];

        val objPack = ObjectRD.getObjectPack(date, objHash);
        val counterSet = CounterManager.getInstance().getCounterEngine().getCounterSet(objPack.objType);

        val handler = (key: Array[Byte]) => {
            val ck = CounterKey.toCounterKey(key);
            if (ck.objHash == objHash) {
                counterSet.add(ck.counter);
            }

        }

        DailyCounterRD.readKey(date, handler)

        val counterArray = SortUtil.sort_string(counterSet.iterator(), counterSet.size());
        val pack = new MapPack();
        val titleLv = pack.newList("values");
        titleLv.add("time");
        for (key <- counterArray) {
            titleLv.add(key);
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(pack);

        val stime = DateUtil.yyyymmdd(date);
        val etime = stime + DateUtil.MILLIS_PER_DAY;
        val delta = TimeTypeEnum.getTime(timeType);

        var time = stime
        while (time < etime) {
            val HHmm = DateUtil.hhmm(time);
            val hhmm = CastUtil.cint(HHmm);
            val pack = new MapPack();
            val valueLv = pack.newList("values");
            valueLv.add(HHmm.substring(0, 2) + ":" + HHmm.substring(2, 4));
            counterArray.foreach((key: String) => {
                val ck = new CounterKey(objHash, key, timeType);
                val v = DailyCounterRD.getValue(date, ck, hhmm);
                if (v == null) {
                    valueLv.add(new NullValue());
                } else {
                    valueLv.add(v);
                }
            })

            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(pack);

            time += delta
        }
    }

}
