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

import scouter.lang.pack.MapPack
import scouter.lang.value.DecimalValue
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.db.TextRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.DateUtil
import scouter.util.Hexa32
import scouter.util.ArrayUtil

class HashText {

  @ServiceHandler(RequestCmd.GET_TEXT)
  def getText(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    var date = param.getText("date");
    val _type = param.getText("type");
    val hash = param.getList("hash");
    if (hash == null)
      return ;

    if (date == null)
      date = DateUtil.yyyymmdd();

    val result = new MapPack();
    for (i <- 0 to ArrayUtil.len(hash) - 1) {
      val h = hash.getInt(i);
      val v = TextRD.getString(date, _type, h);
      if (v != null) {
        result.put(Hexa32.toString32(h), v);
      }

    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(result);
  }

  @ServiceHandler(RequestCmd.GET_TEXT_100)
  def getText100(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    var date = param.getText("date");
    val _type = param.getText("type");
    val hash = param.getList("hash");
    if (hash == null)
      return ;

    if (date == null)
      date = DateUtil.yyyymmdd();

    val result = new MapPack();
    for (i <- 0 to ArrayUtil.len(hash) - 1) {
      val h = hash.getInt(i);
      val v = TextRD.getString(date, _type, h);
      if (v != null) {
        result.put(Hexa32.toString32(h), v);
        if (result.size() == 100) {
          dout.writeByte(TcpFlag.HasNEXT);
          dout.writePack(result);
          result.clear();
        }
      }

    }
    if (result.size() > 0) {
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(result);
    }
  }
}