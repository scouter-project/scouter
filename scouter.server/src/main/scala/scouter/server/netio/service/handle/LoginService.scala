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

import java.io.IOException
import java.util.TimeZone
import scouter.Version
import scouter.lang.Account
import scouter.lang.pack.MapPack
import scouter.lang.value.ListValue
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.LoginManager
import scouter.server.LoginUser
import scouter.server.account.AccountManager
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.ArrayUtil
import scouter.lang.value.BooleanValue

class LoginService {

  @ServiceHandler(RequestCmd.LOGIN)
  def login(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val id = m.getText("id");
    val passwd = m.getText("pass");
    val ip = m.getText("ip");
    val name = m.getText("hostname");
    val clientVer = m.getText("version");
    val internal = m.getText("internal");
    val internalMode = if (internal != null && internal.equalsIgnoreCase("true")) true else false;

    val session = LoginManager.login(id, passwd, ip, internalMode);

    m.put("session", session);
    if (session == 0) {
      m.put("error", "login fail");
    } else {
      val user = LoginManager.getUser(session);
      user.hostname = name;
      user.version = clientVer;
      m.put("time", System.currentTimeMillis());
      m.put("server_id", getServerId());
      m.put("type", user.group);
      m.put("version", Version.getServerFullVersion());
      m.put("client_version", Version.getServerRecommendedClientVersion());

      val acc = AccountManager.getAccount(id);
      if (acc != null) {
        m.put("email", acc.email);
      }
      m.put("timezone", TimeZone.getDefault().getDisplayName());
      val mv = AccountManager.getGroupPolicy(user.group);
      if (mv != null) {
        m.put("policy", mv);
      }
      val menuMv = new MapValue();
      m.put("menu", menuMv);
      menuMv.put("tag_count", new BooleanValue(Configure.getInstance().tagcnt_enabled));
      m.put("so_time_out", Configure.getInstance().net_tcp_client_so_timeout_ms);
      m.put("ext_link_name", Configure.getInstance().ext_link_name);
      m.put("ext_link_url_pattern", Configure.getInstance().ext_link_url_pattern);
      
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(m);
  }

  @ServiceHandler(RequestCmd.GET_LOGIN_LIST)
  def getLoginList(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val users = LoginManager.getLoginUserList()
    if (ArrayUtil.len(users) > 0) {
      val result = new MapPack();
      val sessionLv = result.newList("session");
      val userLv = result.newList("user");
      val ipLv = result.newList("ip");
      val loginTimeLv = result.newList("logintime");
      val versioneLv = result.newList("ver");
      val hostnameLv = result.newList("host");
      for (usr <- users) {
        sessionLv.add(usr.session);
        userLv.add(usr.id);
        ipLv.add(usr.ip);
        loginTimeLv.add((System.currentTimeMillis() - usr.logintime) / 1000L);
        versioneLv.add(usr.version);
        hostnameLv.add(usr.hostname);
      }
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(result);
    }
  }

  @ServiceHandler(RequestCmd.CHECK_SESSION)
  def getSessionCheck(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readMapPack();
    val session = m.getLong("session");
    val validSession = LoginManager.validSession(session);
    m.put("validSession", validSession);
    if (validSession == 0) {
      m.put("error", "login fail");
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(m);
  }

  def getServerId(): String = {
    Configure.getInstance().server_id;
  }

  @ServiceHandler(RequestCmd.CHECK_LOGIN)
  def checkLogin(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val mapPack = din.readMapPack()
    val id = mapPack.getText("id")
    val password = mapPack.getText("pass")

    val account = AccountManager.authorizeAccount(id, password);
    var booleanValue =
      if (account == null) new BooleanValue(false)
      else new BooleanValue(true)

    dout.writeByte(TcpFlag.HasNEXT)
    dout.writeValue(booleanValue)
  }

}
