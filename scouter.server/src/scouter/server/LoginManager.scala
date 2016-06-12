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
 */
package scouter.server;
import scouter.server.account.AccountManager
import scouter.server.util.{EnumerScala, ThreadScala}
import scouter.util.{CacheTable, DateUtil, KeyGen, ThreadUtil}
object LoginManager {
    val sessionTable = new CacheTable[Long, LoginUser]().setDefaultKeepTime(DateUtil.MILLIS_PER_HOUR * 3);
    ThreadScala.startDaemon("scouter.server.LoginManager") {
        while (true) {
            sessionTable.clearExpiredItems();
            ThreadUtil.sleep(5000);
        }
    }
    def login(id: String, pwd: String, ip: String): Long = {
        val account = AccountManager.authorizeAccount(id, pwd);
        if (account == null) {
            return 0;
        }
        val u = new LoginUser();
        u.id = id;
        u.ip = ip;
        u.passwd = pwd;
        u.group = account.group;
        u.logintime = System.currentTimeMillis();
        val key = KeyGen.next();
        u.session = key;
        sessionTable.put(key, u);
        return key;
    }
    def getUser(session: Long): LoginUser = {
        return sessionTable.get(session);
    }
    def okSession(key: Long): Boolean = {
        return sessionTable.getKeepAlive(key) != null
    }
    def validSession(key: Long): Int = {
        val u = sessionTable.getKeepAlive(key);
        return if (u == null) 0 else 1
    }
    def getLoginUserList(): Array[LoginUser] = {
        val loginUsers = new Array[LoginUser](sessionTable.size());
        var index = 0;
        EnumerScala.foreach(sessionTable.keys(), (session: Long) => {
            loginUsers(index) = sessionTable.get(session);
            index += 1
        })
        return loginUsers;
    }
}
