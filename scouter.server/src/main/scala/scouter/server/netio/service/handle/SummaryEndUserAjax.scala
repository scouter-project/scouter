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

class SummaryEndUserAjax {

  class Temp1() {

    var id: Long = 0L;
    var uri: Int = 0;
    var ip: Int = 0;
    var count: Int = 0;
 
    var duration: Long = 0L;
    var userAgent: Int = 0;
  }

  @ServiceHandler(RequestCmd.LOAD_ENDUSER_AJAX_SUMMARY)
  def LOAD_ENDUSER_AJAX_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val objType = param.getText("objType");

    val tempMap = new LongKeyLinkedMap[Temp1]().setMax(50000)

    val handler = (time: Long, data: Array[Byte]) => {
      val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
      if (p.stype == SummaryEnum.ENDUSER_AJAX_TIME && (objType == null || objType == p.objType)) {
        val id = p.table.getList("id")

        var uri = p.table.getList("uri")
        var ip = p.table.getList("ip")
        var count = p.table.getList("count")
        //
        var duration = p.table.getList("duration")
        var userAgent = p.table.getList("userAgent")
       

        for (i <- 0 to id.size() - 1) {
          var tempObj = tempMap.get(id.getInt(i));
          if (tempObj == null) {
            tempObj = new Temp1();
            tempObj.id = id.getLong(i);
            tempObj.uri = uri.getInt(i);
            tempObj.ip = ip.getInt(i);
            tempObj.userAgent = userAgent.getInt(i);
            
            tempMap.put(tempObj.id , tempObj);
          }
          tempObj.count += count.getInt(i);
          tempObj.duration += duration.getLong(i)
      
        }
      }
    }

    SummaryRD.readByTime(SummaryEnum.ENDUSER_AJAX_TIME, date, stime, etime, handler)

    val map = new MapPack();
    var id = map.newList("id")
    var uri = map.newList("uri")
    var ip = map.newList("ip")
    var count = map.newList("count")
    //
    var duration = map.newList("duration")
    var userAgent = map.newList("userAgent")
   
    val itr = tempMap.keys();
    while (itr.hasMoreElements()) {
      val key = itr.nextLong();
      val obj = tempMap.get(key);
      
      id.add(obj.id)
      uri.add(obj.uri)
      ip.add(obj.ip)
      count.add(obj.count)

      duration.add(obj.duration)
      userAgent.add(obj.userAgent)
    }

    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(map);
  }

}