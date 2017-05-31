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
package scouter.server.account;

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.Enumeration
import java.util.List
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import scouter.server.Configure
import scouter.server.Logger
import scouter.lang.Account
import scouter.lang.pack.MapPack
import scouter.lang.value.MapValue
import scouter.lang.value.Value
import scouter.util.CipherUtil
import scouter.util.FileUtil
import scouter.util.StringKeyLinkedMap
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scouter.server.core.CoreRun

object AccountManager {
    val ACCOUNT_FILENAME = "account.xml";
    val GROUP_FILENAME = "account_group.xml";
    var accountMap = new StringKeyLinkedMap[Account]();
    var groupPolicyMap = new StringKeyLinkedMap[MapValue]();
    val confPath = Configure.CONF_DIR;  
    var conf = Configure.getInstance();
    FileUtil.mkdirs(confPath);
    val groupFile = new File(confPath + GROUP_FILENAME);
    val accountFile = new File(confPath + ACCOUNT_FILENAME);
    loadGroupFile();
    loadAccountFile();
    var lastModifiedAccountFile = 0L
    var lastModifiedGroupFile = 0L
    ThreadScala.startDaemon("scouter.server.account.AccountManager", { CoreRun.running }, 5000) {
        if (groupFile.lastModified() != lastModifiedGroupFile) {
            loadGroupFile();
        }
        if (accountFile.lastModified() != lastModifiedAccountFile) {
            loadAccountFile();
        }
    }
    private def loadGroupFile() {
        this.synchronized {
            try {
                Logger.println("Load Account Group File");
                if (groupFile.canRead() == false) {
                    var in: InputStream = null;
                    var fos: FileOutputStream = null;
                    var copyFailed = true;
                    var tryCnt = 0;
                    while (copyFailed) {
                        try {
                            tryCnt += 1;
                            in = AccountManager.getClass().getResourceAsStream("/scouter/server/account/" + GROUP_FILENAME);
                            fos = new FileOutputStream(groupFile);
                            fos.write(FileUtil.readAll(in));
                            copyFailed = false;
                        } catch {
                            case e: Exception =>
                                e.printStackTrace();
                                if (tryCnt > 3) {
                                    throw new RuntimeException("Cannot copy account_group.xml");
                                }
                                ThreadUtil.sleep(2000);
                        } finally {
                            FileUtil.close(fos);
                            FileUtil.close(in);
                        }
                    }
                }
                groupPolicyMap = GroupFileHandler.parse(groupFile);
                lastModifiedGroupFile = groupFile.lastModified();
            } catch {
                case e: Exception => e.printStackTrace();
                
            }
        }
    }
    private def loadAccountFile() {
        this.synchronized {
            try {
                Logger.println("Load Account File");
                if (accountFile.canRead() == false) {
                    var in: InputStream = null;
                    var fos: FileOutputStream = null;
                    var copyFailed = true;
                    var tryCnt = 0;
                    while (copyFailed) {
                        try {
                            tryCnt += 1;
                            in = AccountManager.getClass().getResourceAsStream("/scouter/server/account/" + ACCOUNT_FILENAME);
                            fos = new FileOutputStream(accountFile);
                            fos.write(FileUtil.readAll(in));
                            copyFailed = false;
                        } catch {
                            case e: Exception =>
                                e.printStackTrace();
                                if (tryCnt > 3) {
                                    throw new RuntimeException("Cannot copy account.xml");
                                }
                                ThreadUtil.sleep(2000);
                        } finally {
                            FileUtil.close(fos);
                            FileUtil.close(in);
                        }
                    }
                }
                accountMap = AccountFileHandler.parse(accountFile);
                lastModifiedAccountFile = accountFile.lastModified();
            } catch {
                case e: Exception => e.printStackTrace();
            }
        }
    }
    def authorizeAccount(id: String, pass: String): Account = {
        if(conf.getBoolean("account_use_ldap",false)){
          var ctx : DirContext = null;
          var props : Properties = new Properties();
          
          props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
          props.setProperty(Context.PROVIDER_URL, conf.getValue("account_ldap_provider_url"));
          props.setProperty(Context.SECURITY_AUTHENTICATION, conf.getValue("account_ldap_auth=","simple"));
          props.setProperty(Context.SECURITY_PRINCIPAL, id+conf.getValue("account_ldap_principal_domain"));
          props.setProperty(Context.SECURITY_CREDENTIALS, pass);
          
          try{            
            ctx = new InitialDirContext(props);
            var cons : SearchControls = new SearchControls();
            cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
            var searchFilter : String = "(cn="+id+")";
            
            if(conf.getBoolean("account_ldap_debug", false)){
                Logger.println("ldap id : "+id);
                Logger.println("ldap pass : "+pass);
                Logger.println("ldap properties : "+props.toString());
              }
            
            var result = ctx.search(conf.getValue("account_ldap_basedn"), searchFilter, cons);
            var nextEntry : SearchResult = null;
            if(result.hasMore()){
                  var attrs = result.next().getAttributes();
                  var nmEnum = attrs.getIDs();
                  while(nmEnum.hasMore()){
                    var _id = nmEnum.next();
                    
                    if( id.equals( attrs.get(_id).get().toString()) ){
                      var account : Account = new Account();
                      account.id = id
                      try{
                        account.email = attrs.get(conf.getValue("account_ldap_email_id","")).get().toString();
                        account.group = attrs.get(conf.getValue("account_ldap_group_id","")).get().toString();
                      }catch{
                        case ne : NullPointerException => ne.printStackTrace()
                      }
                      return account;
                    }
                  }
             }
            }catch{
              case e: Exception => Logger.println("Ldap Account Error : "+e.toString()); e.printStackTrace();
            }finally{              
              if(null != ctx) ctx.close();
            }
        }else{
          val account = accountMap.get(id);
          if (account == null) {
              return null;
          }
          if (account.password.equals(pass)) {
              return account;
          }
        }
        return null;
    }
    def getAccountList(): List[Account] = {
        val list = new ArrayList[Account]();
        val accEnu = accountMap.values();
        while (accEnu.hasMoreElements()) {
            list.add(accEnu.nextElement());
        }
        return list;
    }
    def getGroupList(): Array[String] = {
        return groupPolicyMap.keyArray();
    }
    def addAccount(account: Account): Boolean = {
        this.synchronized {
            if (accountMap.get(account.id) != null) {
                return false;
            }
            try {
                AccountFileHandler.addAccount(accountFile, account);
                accountMap.put(account.id, account);
                lastModifiedAccountFile = accountFile.lastModified();
                return true;
            } catch {
                case e: Exception => e.printStackTrace();
            }
            return false;
        }
    }
    def editAccount(account: Account): Boolean = {
        this.synchronized {
            if (accountMap.get(account.id) == null) {
                return false;
            }
            try {
                AccountFileHandler.editAccount(accountFile, account);
                accountMap.put(account.id, account);
                lastModifiedAccountFile = accountFile.lastModified();
                return true;
            } catch {
                case e: Exception => e.printStackTrace();
            }
            return false;
        }
    }
    def addAccountGroup(pack: MapPack): Boolean = {
        this.synchronized {
            val name = pack.getText("name");
            val v = pack.get("policy");
            if (name == null || v == null) {
                return false;
            }
            val result = GroupFileHandler.addAccountGroup(groupFile, name, v.asInstanceOf[MapValue]);
            if (result) {
                groupPolicyMap.put(name, v.asInstanceOf[MapValue]);
                lastModifiedGroupFile = groupFile.lastModified();
            }
            return result;
        }
    }
    def editGroupPolicy(pack: MapPack): Boolean = {
        this.synchronized {
            val result = GroupFileHandler.editGroupPolicy(groupFile, pack);
            if (result) {
                loadGroupFile();
            }
            return result;
        }
    }
    def avaliableId(id: String): Boolean = {
        return accountMap.containsKey(id) == false;
    }
    def getAccount(id: String): Account = {
        return accountMap.get(id);
    }
    def getGroupPolicy(groupName: String): MapValue = {
        return groupPolicyMap.get(groupName);
    }
    def readAccountGroup(): Array[Byte] = {
        return FileUtil.readAll(groupFile);
    }
}
