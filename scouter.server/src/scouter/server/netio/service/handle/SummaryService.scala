/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.server.netio.service.handle;

import java.io.IOException
import scouter.server.db.XLogRD
import scouter.util.DateUtil
import scouter.util.IntKeyLinkedMap
import scouter.util.LongKeyLinkedMap
import scouter.lang.pack.XLogPack
import scouter.util.CastUtil
import scouter.server.db.SummaryRD
import scouter.io.DataInputX
import scouter.lang.pack.MapPack
import scouter.util.BitUtil
import scouter.io.DataOutputX
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.lang.pack.SummaryPack
import scouter.lang.SummaryEnum
import scouter.lang.pack.SummaryPack
import scouter.lang.value.ListValue
import scouter.util.ArrayUtil
import scouter.server.util.EnumerScala

class SummaryService {

    class TempObject() {
        var hash: Int = 0;
        var count: Int = 0;
        var errorCnt: Int = 0;
        var elapsedSum: Long = 0;
        var cpuSum: Long = 0;
        var memSum: Long = 0;
    }

    def load(stype: Byte, din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val objType = param.getText("objType");
        val objHash = param.getInt("objHash");

        val tempMap = new IntKeyLinkedMap[TempObject]().setMax(50000)

        val handler = (time: Long, data: Array[Byte]) => {
            val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
            if (p.stype == stype
                && (objHash == 0 || objHash == p.objHash)
                && (objType == null || objType == p.objType)) {
                val id = p.table.getList("id")
                val count = p.table.getList("count")
                val error = p.table.getList("error")
                val elapsed = p.table.getList("elapsed")
                val cpu = p.table.getList("cpu")
                val mem = p.table.getList("mem");

                for (i <- 0 to id.size() - 1) {
                    var tempObj = tempMap.get(id.getInt(i));
                    if (tempObj == null) {
                        tempObj = new TempObject();
                        tempObj.hash = id.getInt(i);
                        tempMap.put(id.getInt(i), tempObj);
                    }
                    tempObj.count += count.getInt(i);
                    tempObj.errorCnt += error.getInt(i);
                    tempObj.elapsedSum += elapsed.getLong(i);
                    if (cpu != null && mem != null) {
                        tempObj.cpuSum += cpu.getLong(i);
                        tempObj.memSum += mem.getLong(i);
                    }
                }
            }
        }

        SummaryRD.readByTime(stype, date, stime, etime, handler)

        val map = new MapPack();
        val newIdList = map.newList("id");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("error"); //count
        val newElapsedSumList = map.newList("elapsed"); //elapsed Time Sum

        var newCpuSumList: ListValue = null;
        var newMemSumList: ListValue = null;
        if (stype == SummaryEnum.APP) {
            newCpuSumList = map.newList("cpu"); // cpu time sum
            newMemSumList = map.newList("mem"); // mem sum
        }
        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newIdList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            if (stype == SummaryEnum.APP) {
                newCpuSumList.add(obj.cpuSum)
                newMemSumList.add(obj.memSum)
            }
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_SERVICE_SUMMARY)
    def LOAD_SERVICE_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        load(SummaryEnum.APP, din, dout, login);
    }
    @ServiceHandler(RequestCmd.LOAD_SQL_SUMMARY)
    def LOAD_SQL_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        load(SummaryEnum.SQL, din, dout, login);
    }
    @ServiceHandler(RequestCmd.LOAD_APICALL_SUMMARY)
    def LOAD_APICALL_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        load(SummaryEnum.APICALL, din, dout, login);
    }
}