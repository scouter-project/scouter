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
package scouter.client.net;

import scouter.Version;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CipherUtil;
import scouter.util.SysJMX;

public class LoginMgr{
	public static boolean login(int serverId, String user, String password){
		try {
			MapPack param = new MapPack();
			param.put("id", user);
			String encrypted = CipherUtil.md5(password);
			param.put("pass", encrypted);
			param.put("version", Version.getClientFullVersion());
			param.put("hostname", SysJMX.getHostName());
			MapPack out = TcpProxy.loginProxy(serverId, param);
			if (out != null) {
				long session = out.getLong("session");
				String error = out.getText("error");
				if(error != null && session == 0L){
					return false;
				}
				long time = out.getLong("time");
				String hostname = out.getText("hostname");
				String type = out.getText("type");
				String version = out.getText("version");
				String email = out.getText("email");
				String timezone = out.getText("timezone");
				
				Server server = ServerManager.getInstance().getServer(serverId);
				server.setOpen(true);
				server.setSession(session);
				server.setName(hostname + "_" + server.getPort());
				server.setDelta(time);
				server.setUserId(user);
				server.setPassword(encrypted);
				server.setGroup(type);
				server.setVersion(version);
				server.setEmail(email);
				server.setTimezone(timezone);
				Value value = out.get("policy");
				if (value != null) {
					MapValue mv = (MapValue) value;
					server.setGroupPolicy(mv);
				}
				Value menuV = out.get("menu");
				if (menuV != null) {
					MapValue mv = (MapValue) menuV;
					server.setMenuEnableMap(mv);
				}
				CounterEngine counterEngine = server.getCounterEngine();
				MapPack m = getCounterXmlServer(serverId);
				if (m != null) {
					counterEngine.clear();
					Value v1 = m.get("default");
					counterEngine.parse(((BlobValue)v1).value);
					v1 = m.get("custom");
					if (v1 != null) {
						counterEngine.parse(((BlobValue)v1).value);
					}
				}
				return true;
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean silentLogin(Server server, String user, String encryptedPwd){
		try {
			MapPack param = new MapPack();
			param.put("id", user);
			param.put("pass", encryptedPwd);
			param.put("version", Version.getClientFullVersion());
			param.put("hostname", SysJMX.getHostName());
			
			MapPack out = TcpProxy.loginProxy(server.getId(), param);
			
			if (out != null) {
				long session = out.getLong("session");
				String error = out.getText("error");
				if(error != null && session == 0L){
					return false;
				}
				server.setOpen(true);
				long time = out.getLong("time");
				String hostname = out.getText("hostname");
				String type = out.getText("type");
				String version = out.getText("version");
				String email = out.getText("email");
				String timezone = out.getText("timezone");
				
				server.setSession(session);
				server.setName(hostname + "_" + server.getPort());
				server.setDelta(time);
				server.setUserId(user);
				server.setPassword(encryptedPwd);
				server.setGroup(type);
				server.setVersion(version);
				server.setEmail(email);
				server.setTimezone(timezone);
				Value value = out.get("policy");
				if (value != null) {
					MapValue mv = (MapValue) value;
					server.setGroupPolicy(mv);
				}
				Value menuV = out.get("menu");
				if (menuV != null) {
					MapValue mv = (MapValue) menuV;
					server.setMenuEnableMap(mv);
				}
				CounterEngine counterEngine = server.getCounterEngine();
				MapPack m = getCounterXmlServer(server.getId());
				if (m != null) {
					counterEngine.clear();
					Value v1 = m.get("default");
					counterEngine.parse(((BlobValue)v1).value);
					v1 = m.get("custom");
					if (v1 != null) {
						counterEngine.parse(((BlobValue)v1).value);
					}
				}
				return true;
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public static MapPack getCounterXmlServer(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		try {
			p = tcp.getSingle(RequestCmd.GET_XML_COUNTER, null);
			ConsoleProxy.infoSafe("- counter.xml is successfully");
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
			return null;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return (MapPack) p;
	}
}
