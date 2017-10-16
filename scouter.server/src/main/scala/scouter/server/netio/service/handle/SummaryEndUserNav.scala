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

class SummaryEndUserNav {

  class Temp1() {

    var id: Long = 0L;
    var uri: Int = 0;
    var ip: Int = 0;
    var count: Int = 0;
    //
    var navigationStart: Long = 0L;
    var unloadEventStart: Long = 0L;
    var unloadEventEnd: Long = 0L;
    var fetchStart: Long = 0L;
    var domainLookupStart: Long = 0L;
    var domainLookupEnd: Long = 0L;
    var connectStart: Long = 0L;
    var connectEnd: Long = 0L;
    var requestStart: Long = 0L;
    var responseStart: Long = 0L;
    var responseEnd: Long = 0L;
    var domLoading: Long = 0L;
    var domInteractive: Long = 0L;
    var domContentLoadedEventStart: Long = 0L;
    var domContentLoadedEventEnd: Long = 0L;
    var domComplete: Long = 0L;
    var loadEventStart: Long = 0L;
    var loadEventEnd: Long = 0L;

  }

  @ServiceHandler(RequestCmd.LOAD_ENDUSER_NAV_SUMMARY)
  def LOAD_ENDUSER_NAV_SUMMARY(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val objType = param.getText("objType");

    val tempMap = new LongKeyLinkedMap[Temp1]().setMax(50000)

    val handler = (time: Long, data: Array[Byte]) => {
      val p = new DataInputX(data).readPack().asInstanceOf[SummaryPack];
      if (p.stype == SummaryEnum.ENDUSER_NAVIGATION_TIME && (objType == null || objType == p.objType)) {
        val id = p.table.getList("id")

        var uri = p.table.getList("uri")
        var ip = p.table.getList("ip")
        var count = p.table.getList("count")
        //
        var navigationStart = p.table.getList("navigationStart")
        var unloadEventStart = p.table.getList("unloadEventStart")
        var unloadEventEnd = p.table.getList("unloadEventEnd")
        var fetchStart = p.table.getList("fetchStart")
        var domainLookupStart = p.table.getList("domainLookupStart")
        var domainLookupEnd = p.table.getList("domainLookupEnd")
        var connectStart = p.table.getList("connectStart")
        var connectEnd = p.table.getList("connectEnd")
        var requestStart = p.table.getList("requestStart")
        var responseStart = p.table.getList("responseStart")
        var responseEnd = p.table.getList("responseEnd")
        var domLoading = p.table.getList("domLoading")
        var domInteractive = p.table.getList("domInteractive")
        var domContentLoadedEventStart = p.table.getList("domContentLoadedEventStart")
        var domContentLoadedEventEnd = p.table.getList("domContentLoadedEventEnd")
        var domComplete = p.table.getList("domComplete")
        var loadEventStart = p.table.getList("loadEventStart")
        var loadEventEnd = p.table.getList("loadEventEnd")

        for (i <- 0 to id.size() - 1) {
          var tempObj = tempMap.get(id.getInt(i));
          if (tempObj == null) {
            tempObj = new Temp1();
            tempObj.id = id.getLong(i);
            tempObj.uri = uri.getInt(i);
            tempObj.ip = ip.getInt(i);
            tempMap.put(id.getLong(i), tempObj);
          }
          tempObj.count += count.getInt(i);
          tempObj.navigationStart += navigationStart.getLong(i)
          tempObj.unloadEventStart += unloadEventStart.getLong(i)
          tempObj.unloadEventEnd += unloadEventEnd.getLong(i)
          tempObj.fetchStart += fetchStart.getLong(i)
          tempObj.domainLookupStart += domainLookupStart.getLong(i)
          tempObj.domainLookupEnd += domainLookupEnd.getLong(i)
          tempObj.connectStart += connectStart.getLong(i)
          tempObj.connectEnd += connectEnd.getLong(i)
          tempObj.requestStart += requestStart.getLong(i)
          tempObj.responseStart += responseStart.getLong(i)
          tempObj.responseEnd += responseEnd.getLong(i)
          tempObj.domLoading += domLoading.getLong(i)
          tempObj.domInteractive += domInteractive.getLong(i)
          tempObj.domContentLoadedEventStart += domContentLoadedEventStart.getLong(i)
          tempObj.domContentLoadedEventEnd += domContentLoadedEventEnd.getLong(i)
          tempObj.domComplete += domComplete.getLong(i)
          tempObj.loadEventStart += loadEventStart.getLong(i)
          tempObj.loadEventEnd += loadEventEnd.getLong(i)

        }
      }
    }

    SummaryRD.readByTime(SummaryEnum.ENDUSER_NAVIGATION_TIME, date, stime, etime, handler)

    val map = new MapPack();
    var id = map.newList("id")
    var uri = map.newList("uri")
    var ip = map.newList("ip")
    var count = map.newList("count")
    //
    var navigationStart = map.newList("navigationStart")
    var unloadEventStart = map.newList("unloadEventStart")
    var unloadEventEnd = map.newList("unloadEventEnd")
    var fetchStart = map.newList("fetchStart")
    var domainLookupStart = map.newList("domainLookupStart")
    var domainLookupEnd = map.newList("domainLookupEnd")
    var connectStart = map.newList("connectStart")
    var connectEnd = map.newList("connectEnd")
    var requestStart = map.newList("requestStart")
    var responseStart = map.newList("responseStart")
    var responseEnd = map.newList("responseEnd")
    var domLoading = map.newList("domLoading")
    var domInteractive = map.newList("domInteractive")
    var domContentLoadedEventStart = map.newList("domContentLoadedEventStart")
    var domContentLoadedEventEnd = map.newList("domContentLoadedEventEnd")
    var domComplete = map.newList("domComplete")
    var loadEventStart = map.newList("loadEventStart")
    var loadEventEnd = map.newList("loadEventEnd")

    val itr = tempMap.keys();
    while (itr.hasMoreElements()) {
      val key = itr.nextLong();
      val obj = tempMap.get(key);
      
      id.add(obj.id)
      uri.add(obj.uri)
      ip.add(obj.ip)
      count.add(obj.count)

      navigationStart.add(obj.navigationStart)
      unloadEventStart.add(obj.unloadEventStart)
      unloadEventEnd.add(obj.unloadEventEnd)
      fetchStart.add(obj.fetchStart)
      domainLookupStart.add(obj.domainLookupStart)
      domainLookupEnd.add(obj.domainLookupEnd)
      connectStart.add(obj.connectStart)
      connectEnd.add(obj.connectEnd)
      requestStart.add(obj.requestStart)
      responseStart.add(obj.responseStart)
      responseEnd.add(obj.responseEnd)
      domLoading.add(obj.domLoading)
      domInteractive.add(obj.domInteractive)
      domContentLoadedEventStart.add(obj.domContentLoadedEventStart)
      domContentLoadedEventEnd.add(obj.domContentLoadedEventEnd)
      domComplete.add(obj.domComplete)
      loadEventStart.add(obj.loadEventStart)
      loadEventEnd.add(obj.loadEventEnd)

    }

    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(map);
  }

}