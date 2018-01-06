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

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.SummaryEnum
import scouter.lang.pack.MapPack
import scouter.lang.pack.SummaryPack
import scouter.lang.value.ListValue
import scouter.net.TcpFlag
import scouter.server.db.SummaryRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.{HashUtil, IntKeyLinkedMap, LongKeyLinkedMap, StringKeyLinkedMap}
import scouter.net.RequestCmd

class SummaryService {

    class TempObject() {
        var hash: Int = 0;
        var count: Int = 0;
        var errorCnt: Int = 0;
        var elapsedSum: Long = 0;
        var cpuSum: Long = 0;
        var memSum: Long = 0;
    }

    class TempError() {
        var hash: Int = 0;
        var error: Int = 0;
        var service: Int = 0;
        var message: Int = 0;
        var count: Int = 0;
        var txid: Long = 0;
        var sql: Int = 0;
        var apicall: Int = 0;
        var fullstack: Int = 0;
    }

    class TempAlert() {
        var hash: Int = 0;
        var title: String = "";
        var count: Int = 0;
        var level: Byte = 0;
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
                && (objType == null || objType == "" || objType == p.objType)) {
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
    def loadIpAndUA(stype: Byte, din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
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

                for (i <- 0 to id.size() - 1) {
                    var tempObj = tempMap.get(id.getInt(i));
                    if (tempObj == null) {
                        tempObj = new TempObject();
                        tempObj.hash = id.getInt(i);
                        tempMap.put(id.getInt(i), tempObj);
                    }
                    tempObj.count += count.getInt(i);
                }
            }
        }

        SummaryRD.readByTime(stype, date, stime, etime, handler)

        val map = new MapPack();
        val newIdList = map.newList("id");
        val newCountList = map.newList("count");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val hash = itr.nextInt();
            val obj = tempMap.get(hash);
            newIdList.add(obj.hash);
            newCountList.add(obj.count);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }
    def loadServiceErrorSum(stype: Byte, din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val objType = param.getText("objType");
        val objHash = param.getInt("objHash");

        val tempMap = new LongKeyLinkedMap[TempError]().setMax(10000)

        val handler = (time: Long, data: Array[Byte]) => {
            val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
            if (p.stype == stype
                && (objHash == 0 || objHash == p.objHash)
                && (objType == null || objType == p.objType)) {
                val id = p.table.getList("id")
                val error = p.table.getList("error")
                val service = p.table.getList("service")
                val message = p.table.getList("message")
                val count = p.table.getList("count")
                val txid = p.table.getList("txid")
                val sql = p.table.getList("sql")
                val apicall = p.table.getList("apicall")
                val fullstack = p.table.getList("fullstack")

                for (i <- 0 to id.size() - 1) {
                    var tempObj = tempMap.get(id.getInt(i));
                    if (tempObj == null) {
                        tempObj = new TempError();
                        tempObj.hash = id.getInt(i);
                        tempMap.put(id.getInt(i), tempObj);
                        tempObj.error = error.getInt(i);
                        tempObj.service = service.getInt(i);
                        tempObj.txid = txid.getLong(i);
                    }
                    
                    tempObj.count += count.getInt(i);

                    if (tempObj.message == 0) {
                      tempObj.message = message.getInt(i);
                    }

                    if (tempObj.sql == 0) {
                        tempObj.sql = sql.getInt(i);
                    }
                    if (tempObj.apicall == 0) {
                        tempObj.apicall = apicall.getInt(i);
                    }
                    
                    if (tempObj.fullstack == 0) {
                        tempObj.fullstack = fullstack.getInt(i);
                    }
                }
            }
        }

        SummaryRD.readByTime(stype, date, stime, etime, handler)

        //summary의 id는 error+service이다.
        val map = new MapPack();
        val newIdList = map.newList("id");
        val newErrorList = map.newList("error");
        val newServiceList = map.newList("service");
        val newMessageList = map.newList("message");
        val newCountList = map.newList("count");
        val newTxidList = map.newList("txid");
        val newSqlList = map.newList("sql");
        val newApiCallList = map.newList("apicall");
        val newFullStackList = map.newList("fullstack");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val id = itr.nextLong();
            val obj = tempMap.get(id);

            newIdList.add(obj.hash);
            newErrorList.add(obj.error);
            newServiceList.add(obj.service);
            newMessageList.add(obj.message);
            newCountList.add(obj.count);
            newTxidList.add(obj.txid);
            newSqlList.add(obj.sql);
            newApiCallList.add(obj.apicall);
            newFullStackList.add(obj.fullstack);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(map);
    }

    def loadAlertSum(stype: Byte, din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val objType = param.getText("objType");
        val objHash = param.getInt("objHash");

        val tempMap = new StringKeyLinkedMap[TempAlert]().setMax(50000)

        val handler = (time: Long, data: Array[Byte]) => {
            val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
            if (p.stype == stype
                && (objHash == 0 || objHash == p.objHash)
                && (objType == null || objType == p.objType)) {
                val title = p.table.getList("title")
                val count = p.table.getList("count")
                val level = p.table.getList("level")

                for (i <- 0 to title.size() - 1) {
                    var tempObj = tempMap.get(title.getString(i));
                    if (tempObj == null) {
                        tempObj = new TempAlert();
                        tempObj.hash = HashUtil.hash(title.getString(i))
                        tempObj.title = title.getString(i);
                        tempObj.level = level.getInt(i).toByte;
                        tempMap.put(title.getString(i), tempObj);
                    }
                    tempObj.count += count.getInt(i);
                }
            }
        }

        SummaryRD.readByTime(stype, date, stime, etime, handler)

        val map = new MapPack();
        val newIdList = map.newList("id");
        val newTitleList = map.newList("title");
        val newCountList = map.newList("count");
        val newLevelList = map.newList("level");

        val itr = tempMap.keys();
        while (itr.hasMoreElements()) {
            val title = itr.nextString();
            val obj = tempMap.get(title);
            newIdList.add(obj.hash);
            newTitleList.add(obj.title);
            newCountList.add(obj.count);
            newLevelList.add(obj.level);
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
    @ServiceHandler(RequestCmd.LOAD_IP_SUMMARY)
    def LOAD_IP_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        loadIpAndUA(SummaryEnum.IP, din, dout, login);
    }
    @ServiceHandler(RequestCmd.LOAD_UA_SUMMARY)
    def LOAD_UA_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        loadIpAndUA(SummaryEnum.USER_AGENT, din, dout, login);
    }
    @ServiceHandler(RequestCmd.LOAD_SERVICE_ERROR_SUMMARY)
    def LOAD_ERROR_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        loadServiceErrorSum(SummaryEnum.SERVICE_ERROR, din, dout, login);
    }
    @ServiceHandler(RequestCmd.LOAD_ALERT_SUMMARY)
    def LOAD_ALERT_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        loadAlertSum(SummaryEnum.ALERT, din, dout, login);
    }
}