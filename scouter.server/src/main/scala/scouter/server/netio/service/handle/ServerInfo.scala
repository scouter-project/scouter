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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import scouter.Version;
import scouter.util.StringUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.TextValue;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.server.Configure;
import scouter.server.netio.service.anotation.ServiceHandler;
import scouter.util.FileUtil;
import scala.collection.JavaConversions._
class ServerInfo {

  @ServiceHandler(RequestCmd.SERVER_STATUS)
  def getServerStatus(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val totalMemory = Runtime.getRuntime().totalMemory();
    val freeMemory = Runtime.getRuntime().freeMemory();
    val usedMemory = totalMemory - freeMemory;
    val serverPack = new MapPack();
    serverPack.put("used", usedMemory);
    serverPack.put("total", totalMemory);
    serverPack.put("time", System.currentTimeMillis());
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(serverPack);
  }

  @ServiceHandler(RequestCmd.SERVER_VERSION)
  def getServerVersion(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack(); // FOR MORE INFORMATION

    val serverPack = new MapPack();
    serverPack.put("version", Version.getServerFullVersion());
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(serverPack);
  }

  @ServiceHandler(RequestCmd.SERVER_ENV)
  def getAgentEnv(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = new MapPack();
    val p = System.getProperties();
    for (key <- p.keySet()) {
      val value = p.getProperty(key.toString());
      m.put(key.toString(), value);
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(m);
  }

  @ServiceHandler(RequestCmd.SERVER_TIME)
  def getServerTime(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val serverPack = new MapPack();
    serverPack.put("time", System.currentTimeMillis());
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(serverPack);
  }

  @ServiceHandler(RequestCmd.SERVER_LOG_LIST)
  def getServerLogs(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val out = new MapPack();
    val logDir = new File(Configure.getInstance().log_dir);
    if (logDir.exists() == false) {
      return ;
    }
    val nameLv = out.newList("name");
    val sizeLv = out.newList("size");
    val lastModifiedLv = out.newList("lastModified");
    logDir.listFiles().foreach { f =>
      if (f.isFile() && f.getName().endsWith(".log")) {
        nameLv.add(f.getName());
        sizeLv.add(f.length());
        lastModifiedLv.add(f.lastModified());
      }
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(out);
  }

  @ServiceHandler(RequestCmd.SERVER_LOG_DETAIL)
  def getServerLogDetail(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readMapPack();
    val name = param.getText("name");
    if (StringUtil.isEmpty(name)) return ;
    val logFile = new File(Configure.getInstance().log_dir, name);
    if (logFile.canRead() == false) return ;
    val content = FileUtil.readAll(logFile);
    if (content == null) return ;
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new TextValue(new String(content)));
  }
}