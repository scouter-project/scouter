/*
*  Copyright 2015 LG CNS.
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

import scouter.lang.SummaryEnum
import scouter.lang.pack.MapPack
import scouter.lang.pack.XLogPack
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.db.SummaryRD
import scouter.server.db.XLogRD
import scouter.server.db.summary.SummaryDataReader
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.BitUtil
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.IntEnumer
import scouter.util.IntKeyLinkedMap
import scouter.util.LongEnumer
import scouter.util.LongKeyLinkedMap

import java.io.IOException

class SummaryService {

    @ServiceHandler(RequestCmd.LOAD_SERVICE_SUMMARY)
    def loadServiceSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject]().setMax(50000)

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {
            val data = new DataInputX(reader.read(pos)).readMapPack();
            val serviceList = data.getList("service");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");
            val cpuSumList = data.getList("cpuSum");

            for (i <- 0 to serviceList.size() - 1) {
                val serviceHash = CastUtil.cint(serviceList.get(i));
                val cnt = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                val cpuSum = CastUtil.clong(cpuSumList.get(i));
                if (tempMap.containsKey(serviceHash)) {
                    val serviceObj = tempMap.get(serviceHash);
                    serviceObj.count += cnt;
                    serviceObj.errorCnt += errorCnt;
                    serviceObj.elapsedSum += elapsedSum;
                    serviceObj.cpuSum += cpuSum;
                } else {
                    tempMap.put(serviceHash, new TempObject(serviceHash, cnt, errorCnt, elapsedSum, cpuSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.APP, handler)

        val map = new MapPack();
        val newServiceList = map.newList("service");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");
        val newCpuSumList = map.newList("cpuSum");
        val newCpuAvgList = map.newList("cpuAvg");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newServiceList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
            newCpuSumList.add(obj.cpuSum);
            newCpuAvgList.add(obj.cpuSum * 1.0 / obj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_SERVICE_SHORTTIME_SUMMARY)
    def loadServiceShortTimeSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject]().setMax(50000);

        val handler = (time: Long, data: Array[Byte]) => {
            try {
                val p = new DataInputX(data).readPack().asInstanceOf[XLogPack];
                var tempObj = tempMap.get(p.service);
                val cpuTime = p.cpu;
                if (tempObj == null) {
                    tempObj = new TempObject(p.service, 1, if (p.error != 0) 1 else 0, p.elapsed, cpuTime);
                    tempMap.put(p.service, tempObj);
                }
                tempObj.count += 1;
                if (p.error != 0) {
                    tempObj.errorCnt += 1;
                }
                tempObj.elapsedSum += p.elapsed;
                tempObj.cpuSum += cpuTime;
            } catch {
                case e: IOException => e.printStackTrace();
            }
        }

        XLogRD.readByTime(date, stime, etime, handler)

        val map = new MapPack();
        val newServiceList = map.newList("service");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");
        val newCpuSumList = map.newList("cpuSum");
        val newCpuAvgList = map.newList("cpuAvg");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newServiceList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
            newCpuSumList.add(obj.cpuSum);
            newCpuAvgList.add(obj.cpuSum * 1.0 / obj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    class TempObject(_hash: Int, _count: Int, _errorCnt: Int, _elapsedSum: Long, _cpuSum: Long = 0L) {
        var hash: Int = _hash;
        var count: Int = _count;
        var errorCnt: Int = _errorCnt;
        var elapsedSum: Long = _elapsedSum;
        var cpuSum: Long = _cpuSum;
    }

    @ServiceHandler(RequestCmd.LOAD_SQL_SUMMARY)
    def loadSqlSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject2]().setMax(50000);

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {
            val data = new DataInputX(reader.read(pos)).readMapPack();
            val sqlList = data.getList("sql");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");

            for (i <- 0 to sqlList.size() - 1) {
                val sqlHash = CastUtil.cint(sqlList.get(i));
                val count = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                if (tempMap.containsKey(sqlHash)) {
                    val sqlObj = tempMap.get(sqlHash);
                    sqlObj.count += count;
                    sqlObj.errorCnt += errorCnt;
                    sqlObj.elapsedSum += elapsedSum;
                } else {
                    tempMap.put(sqlHash, new TempObject2(sqlHash, count, errorCnt, elapsedSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.SQL, handler)

        val map = new MapPack();
        val newSqlList = map.newList("sql");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val sqlObj = tempMap.get(hash);
            newSqlList.add(sqlObj.hash);
            newCountList.add(sqlObj.count);
            newErrorCntList.add(sqlObj.errorCnt);
            newElapsedSumList.add(sqlObj.elapsedSum);
            newElapsedAvgList.add(sqlObj.elapsedSum / sqlObj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_APICALL_SUMMARY)
    def loadApiCallSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject2]().setMax(50000);

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {
            val data = new DataInputX(reader.read(pos)).readMapPack();
            val apiList = data.getList("api");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");

            for (i <- 0 to apiList.size() - 1) {
                val apiHash = CastUtil.cint(apiList.get(i));
                val count = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                if (tempMap.containsKey(apiHash)) {
                    val apiObj = tempMap.get(apiHash);
                    apiObj.count += count;
                    apiObj.errorCnt += errorCnt;
                    apiObj.elapsedSum += elapsedSum;
                } else {
                    tempMap.put(apiHash, new TempObject2(apiHash, count, errorCnt, elapsedSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.APICALL, handler)

        val map = new MapPack();
        val newApiList = map.newList("api");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val sqlObj = tempMap.get(hash);
            newApiList.add(sqlObj.hash);
            newCountList.add(sqlObj.count);
            newErrorCntList.add(sqlObj.errorCnt);
            newElapsedSumList.add(sqlObj.elapsedSum);
            newElapsedAvgList.add(sqlObj.elapsedSum / sqlObj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    class TempObject2(_sql: Int, _count: Int, _errorCnt: Int, _elapsedSum: Long) {
        var hash: Int = _sql;
        var count: Int = _count;
        var errorCnt: Int = _errorCnt;
        var elapsedSum: Long = _elapsedSum;
    }

    @ServiceHandler(RequestCmd.LOAD_APP_SQL_SUMMARY)
    def loadAppSqlSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new LongKeyLinkedMap[TempObject2]().setMax(50000);

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {
            val data = new DataInputX(reader.read(pos)).readMapPack();
            val appList = data.getList("app");
            val sqlList = data.getList("sql");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");

            for (i <- 0 to appList.size() - 1) {
                val serviceHash = CastUtil.cint(appList.get(i));
                val sqlHash = CastUtil.cint(sqlList.get(i));
                val count = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                val key = BitUtil.compsite(serviceHash, sqlHash);
                if (tempMap.containsKey(key)) {
                    val obj = tempMap.get(key);
                    obj.count += count;
                    obj.errorCnt += errorCnt;
                    obj.elapsedSum += elapsedSum;
                } else {
                    tempMap.put(key, new TempObject2(sqlHash, count, errorCnt, elapsedSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.APP_SQL, handler)

        val map = new MapPack();
        val newAppList = map.newList("app");
        val newSqlList = map.newList("sql");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");

        var itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextLong();
            val obj = tempMap.get(key);
            newAppList.add(BitUtil.getHigh(key));
            newSqlList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_APP_APICALL_SUMMARY)
    def loadAppApicallSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new LongKeyLinkedMap[TempObject2]().setMax(50000);

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {

            val data = new DataInputX(reader.read(pos)).readMapPack();
            val appList = data.getList("app");
            val apiList = data.getList("api");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");

            for (i <- 0 to appList.size() - 1) {
                val serviceHash = CastUtil.cint(appList.get(i));
                val apiHash = CastUtil.cint(apiList.get(i));
                val count = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                val key = BitUtil.compsite(serviceHash, apiHash);
                if (tempMap.containsKey(key)) {
                    val obj = tempMap.get(key);
                    obj.count += count;
                    obj.errorCnt += errorCnt;
                    obj.elapsedSum += elapsedSum;
                } else {
                    tempMap.put(key, new TempObject2(apiHash, count, errorCnt, elapsedSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.APP_APICALL, handler)
        val map = new MapPack();
        val newAppList = map.newList("app");
        val newapiList = map.newList("api");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");

        var itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextLong();
            val obj = tempMap.get(key);
            newAppList.add(BitUtil.getHigh(key));
            newapiList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_IP_SUMMARY)
    def loadIpSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");

        val tempMap = new IntKeyLinkedMap[TempObject]().setMax(50000);

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {

            val data = new DataInputX(reader.read(pos)).readMapPack();
            val ipList = data.getList("ip");
            val countList = data.getList("count");
            val errorCntList = data.getList("errorCnt");
            val elapsedSumList = data.getList("elapsedSum");
            val cpuSumList = data.getList("cpuSum");
            for (i <- 0 to ipList.size() - 1) {
                val ipHash = CastUtil.cint(ipList.get(i));
                val count = CastUtil.cint(countList.get(i));
                val errorCnt = CastUtil.cint(errorCntList.get(i));
                val elapsedSum = CastUtil.clong(elapsedSumList.get(i));
                val cpuSum = CastUtil.clong(cpuSumList.get(i));
                if (tempMap.containsKey(ipHash)) {
                    val sqlObj = tempMap.get(ipHash);
                    sqlObj.count += count;
                    sqlObj.errorCnt += errorCnt;
                    sqlObj.elapsedSum += elapsedSum;
                    sqlObj.cpuSum += cpuSum;
                } else {
                    tempMap.put(ipHash, new TempObject(ipHash, count, errorCnt, elapsedSum, cpuSum));
                }
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.IP, handler)

        val map = new MapPack();
        val newIpList = map.newList("ip");
        val newCountList = map.newList("count");
        val newErrorCntList = map.newList("errorCnt");
        val newElapsedAvgList = map.newList("elapsedAvg");
        val newElapsedSumList = map.newList("elapsedSum");
        val newCpuSumList = map.newList("cpuSum");
        val newCpuAvgList = map.newList("cpuAvg");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newIpList.add(obj.hash);
            newCountList.add(obj.count);
            newErrorCntList.add(obj.errorCnt);
            newElapsedSumList.add(obj.elapsedSum);
            newElapsedAvgList.add(obj.elapsedSum / obj.count);
            newCpuSumList.add(obj.cpuSum);
            newCpuAvgList.add(obj.cpuSum * 1.0 / obj.count);
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    @ServiceHandler(RequestCmd.LOAD_DAILY_IP_SUMMARY)
    def loadDailyIpSummary(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = DateUtil.yyyymmdd(date);
        val etime = stime + DateUtil.MILLIS_PER_DAY - 1;

        val handler = (time: Long, objHash: Int, _type: Byte, pos: Long, reader: SummaryDataReader) => {
            val data = new DataInputX(reader.read(pos)).readMapPack();
            val ipLv = data.getList("ip");
            if (ipLv != null) {
                val result = new MapPack();
                result.put("time", time);
                result.put("ip", ipLv);
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(result);
                dout.flush();
            }
        }

        SummaryRD.read(date, stime, etime, SummaryEnum.IP, handler)
    }
}