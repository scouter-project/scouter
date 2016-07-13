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

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.value.DecimalValue
import scouter.net.TcpFlag
import scouter.server.db.VisitorDB
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.net.RequestCmd
import scouter.util.DateUtil
import scouter.lang.pack.MapPack
import scouter.server.db.VisitorHourlyDB

class VisitorService {

  @ServiceHandler(RequestCmd.VISITOR_REALTIME)
  def visitorRealtime(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objHash = m.getInt("objHash");
    val value = VisitorDB.getVisitorObject(objHash);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new DecimalValue(value));
  }

  @ServiceHandler(RequestCmd.VISITOR_REALTIME_TOTAL)
  def visitorRealtimeTotal(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objType = m.getText("objType");
    val value = VisitorDB.getVisitorObjType(objType);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new DecimalValue(value));
  }
  
  @ServiceHandler(RequestCmd.VISITOR_REALTIME_GROUP)
  def visitorRealtimeGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objHashLv = m.getList("objHash");
    val value = VisitorDB.getMergedVisitorObject(objHashLv)
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new DecimalValue(value));
  }

  @ServiceHandler(RequestCmd.VISITOR_LOADDATE)
  def visitorLoaddate(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objHash = m.getInt("objHash");
    val date = m.getText("date");
    val value = VisitorDB.getVisitorObject(date, objHash);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new DecimalValue(value));
  }

  @ServiceHandler(RequestCmd.VISITOR_LOADDATE_TOTAL)
  def visitorLoaddateTotal(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objType = m.getText("objType");
    val date = m.getText("date");
    val value = VisitorDB.getVisitorObjType(date, objType);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new DecimalValue(value));
  }
  
  @ServiceHandler(RequestCmd.VISITOR_LOADDATE_GROUP)
  def visitorLoaddateGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objHashLv = m.getList("objHash");
    val startDate = m.getText("startDate");
    val endDate = m.getText("endDate");
    var time = DateUtil.yyyymmdd(startDate)
    var etime = DateUtil.yyyymmdd(endDate)
    val resultPack = new MapPack()
    while (time <= etime) {
      var date = DateUtil.yyyymmdd(time)
      var value = VisitorDB.getMergedVisitorObject(date, objHashLv)
      resultPack.put("date", date)
      resultPack.put("value", value)
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(resultPack);
      time = time + DateUtil.MILLIS_PER_DAY
    }
  }
  
  @ServiceHandler(RequestCmd.VISITOR_LOADHOUR_GROUP)
  def visitorLoadhourGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val objHashLv = m.getList("objHash");
    val stime = m.getLong("stime");
    val etime = m.getLong("etime");
    val resultPack = new MapPack()
    var timeLv = resultPack.newList("time")
    var valueLv = resultPack.newList("value")
    var time = stime
    var date = DateUtil.yyyymmdd(time)
    while (time <= etime) {
      var dt = DateUtil.yyyymmdd(time)
      if (date != dt) {
        date = dt
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(resultPack);
        timeLv = resultPack.newList("time")
        valueLv = resultPack.newList("value")
      }
      var value = VisitorHourlyDB.getMergedVisitorObject(date, time, objHashLv)
      timeLv.add(time)
      valueLv.add(value)
      time = time + DateUtil.MILLIS_PER_HOUR
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(resultPack);
  }
}