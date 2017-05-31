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

import scouter.lang.Account
import scouter.lang.pack.MapPack
import scouter.lang.value.BlobValue
import scouter.lang.value.BooleanValue
import scouter.lang.value.ListValue
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.account.AccountManager
import scouter.server.netio.service.anotation.ServiceHandler
import scala.collection.JavaConversions._

class AccountService {

  @ServiceHandler(RequestCmd.ADD_ACCOUNT)
  def addAccount(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readPack().asInstanceOf[MapPack];
    val id = m.getText("id");
    val passwd = m.getText("pass");
    val email = m.getText("email");
    val group = m.getText("group");
    val account = new Account();
    account.id = id;
    account.password = passwd;
    account.email = email;
    account.group = group;
    val result = AccountManager.addAccount(account);
    val pack = new MapPack();
    pack.put("result", new BooleanValue(result));
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }

  @ServiceHandler(RequestCmd.EDIT_ACCOUNT)
  def editAccount(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readPack().asInstanceOf[MapPack];
    val id = m.getText("id");
    val passwd = m.getText("pass");
    val email = m.getText("email");
    val group = m.getText("group");
    val account = new Account();
    account.id = id;
    account.password = passwd;
    account.email = email;
    account.group = group;
    val result = AccountManager.editAccount(account);
    val pack = new MapPack();
    pack.put("result", new BooleanValue(result));
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }

  @ServiceHandler(RequestCmd.CHECK_ACCOUNT_ID)
  def checkAccountId(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val m = din.readPack().asInstanceOf[MapPack];
    val id = m.getText("id");
    val result = AccountManager.avaliableId(id);
    val pack = new MapPack();
    pack.put("result", new BooleanValue(result));
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }

  @ServiceHandler(RequestCmd.LIST_ACCOUNT)
  def listAccount(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val accountList = AccountManager.getAccountList();
    accountList.toList.foreach { x =>
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writeValue(new BlobValue(x.toBytes()));
    }
  }

  @ServiceHandler(RequestCmd.LIST_ACCOUNT_GROUP)
  def listAccountGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val groupList = AccountManager.getGroupList()
    val result = new MapPack();
    val lv = result.newList("group_list");
    for (group <- groupList) {
      lv.add(group);
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(result);
  }

  @ServiceHandler(RequestCmd.GET_GROUP_POLICY_ALL)
  def getGroupPolicyAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val groupList = AccountManager.getGroupList();
    val result = new MapPack();
    for (name <- groupList) {
      val mv = AccountManager.getGroupPolicy(name);
      result.put(name, mv);
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(result);
  }

  @ServiceHandler(RequestCmd.EDIT_GROUP_POLICY)
  def editGroupPolicy(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack];
    val result = AccountManager.editGroupPolicy(param);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new BooleanValue(result));
  }

  @ServiceHandler(RequestCmd.ADD_ACCOUNT_GROUP)
  def addAccountGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
    val param = din.readPack().asInstanceOf[MapPack];
    val result = AccountManager.addAccountGroup(param);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(new BooleanValue(result));
  }
}