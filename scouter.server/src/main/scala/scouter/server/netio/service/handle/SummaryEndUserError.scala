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
import scouter.util.LongKeyLinkedMap
import scouter.net.RequestCmd

class SummaryEndUserError {

  class Temp1() {

    var id: Long = 0L;
    var host: Int = 0
    var stacktrace: Int = 0
    var userAgent: Int = 0
    var count: Int = 0
    var uri: Int = 0
    var message: Int = 0
    var file: Int = 0
    var lineNumber: Int = 0
    var columnNumber: Int = 0
    var payloadVersion: Int = 0
  }

  @ServiceHandler(RequestCmd.LOAD_ENDUSER_ERROR_SUMMARY)
  def LOAD_ENDUSER_ERROR_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val objType = param.getText("objType");

    val tempMap = new LongKeyLinkedMap[Temp1]().setMax(50000)

    val handler = (time: Long, data: Array[Byte]) => {
      val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
      if (p.stype == SummaryEnum.ENDUSER_SCRIPT_ERROR && (objType == null || objType == p.objType)) {
        val id = p.table.getList("id")

        var host = p.table.getList("host")
        var stacktrace = p.table.getList("stacktrace")
        var userAgent = p.table.getList("userAgent")
        var count = p.table.getList("count")
        //
        var uri = p.table.getList("uri")
        var message = p.table.getList("message")
        var file = p.table.getList("file")
        var lineNumber = p.table.getList("lineNumber")
        var columnNumber = p.table.getList("columnNumber")

        for (i <- 0 to id.size() - 1) {
          var tempObj = tempMap.get(id.getInt(i));
          if (tempObj == null) {
            tempObj = new Temp1();
            tempObj.id = id.getLong(i);
            tempObj.host = stacktrace.getInt(i);
            tempObj.stacktrace = stacktrace.getInt(i);
            tempObj.userAgent = userAgent.getInt(i);
            tempObj.uri = uri.getInt(i)
            tempObj.message = message.getInt(i)
            tempObj.file = file.getInt(i)
            tempObj.lineNumber = lineNumber.getInt(i)
            tempObj.columnNumber = columnNumber.getInt(i)

            tempMap.put(tempObj.id, tempObj);
          }
          tempObj.count += count.getInt(i);
        }
      }
    }

    SummaryRD.readByTime(SummaryEnum.ENDUSER_SCRIPT_ERROR, date, stime, etime, handler)

    val map = new MapPack()
    var id = map.newList("id")
    var host = map.newList("host")
    var stacktrace = map.newList("stacktrace")
    var userAgent = map.newList("userAgent")
    var uri = map.newList("uri")
    var message = map.newList("message")
    var file = map.newList("file")
    var lineNumber = map.newList("lineNumber")
    var columnNumber = map.newList("columnNumber")

    val itr = tempMap.keys()
    while (itr.hasMoreElements()) {
      val key = itr.nextLong()
      val obj = tempMap.get(key)

      id.add(obj.id)
      host.add(obj.host);
      stacktrace.add(obj.stacktrace)
      userAgent.add(obj.userAgent)
      uri.add(obj.uri)
      message.add(obj.message)
      file.add(obj.file)
      lineNumber.add(obj.lineNumber)
      columnNumber.add(obj.columnNumber)
    }

    dout.writeByte(TcpFlag.HasNEXT)
    dout.writePack(map)
  }

}