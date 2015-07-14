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

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.value.DecimalValue
import scouter.net.TcpFlag
import scouter.server.db.VisitorDB
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.net.RequestCmd

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
}