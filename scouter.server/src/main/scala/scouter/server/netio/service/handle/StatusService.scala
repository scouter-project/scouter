/*
 *  Copyright 2015 Scouter Project.
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
 */
package scouter.server.netio.service.handle;

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.MapPack
import scouter.lang.pack.StatusPack
import scouter.net.TcpFlag
import scouter.server.db.StatusRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.DateUtil
import scouter.net.RequestCmd

class StatusService {

  @ServiceHandler(RequestCmd.STATUS_AROUND_VALUE)
  def statusAroundValue(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readPack().asInstanceOf[MapPack];
    val objHash = param.getInt("objHash");
    val key = param.getText("key");
    val time = param.getLong("time");
    
    val date = DateUtil.yyyymmdd(time);
    
    val handler = (time: Long, data: Array[Byte]) => {
      val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
        if (pk.key == key && pk.objHash == objHash) {
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(pk);
          return
        }
    }
    StatusRD.readByTime(date, time, time + DateUtil.MILLIS_PER_MINUTE, handler);
  }
}