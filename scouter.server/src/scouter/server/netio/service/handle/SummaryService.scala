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

class SummaryService {

    class TempObject(_hash: Int, _count: Int, _errorCnt: Int, _elapsedSum: Long) {
        var hash: Int = _hash;
        var count: Int = _count;
        var errorCnt: Int = _errorCnt;
        var elapsedSum: Long = _elapsedSum;
    }

    def load(stype: Byte, din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject]().setMax(50000)

        val handler = (time: Long, data: Array[Byte]) => {
            val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
            if (p.stype == stype) {
                for (i <- 0 to p.id.length - 1) {
                    if (tempMap.containsKey(p.id(i))) {
                        val serviceObj = tempMap.get(p.id(i));
                        serviceObj.count += p.count(1);
                        serviceObj.errorCnt += p.errorCnt(i);
                        serviceObj.elapsedSum += p.elapsedSum(i);
                    } else {
                        tempMap.put(p.id(i), new TempObject(p.id(i), p.count(i), p.errorCnt(i), p.elapsedSum(i)));
                    }
                }
            }
        }

        SummaryRD.readByTime(stype, date, stime, etime, handler)

        val map = new MapPack();
        val newServiceList = map.newList("id");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newServiceList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
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