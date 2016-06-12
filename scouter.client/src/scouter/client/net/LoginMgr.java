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
	public static LoginResult login(int serverId, String user, String password){
		Server server = ServerManager.getInstance().getServer(serverId);
		String encrypted = CipherUtil.sha256(password);
		return silentLogin(server, user, encrypted);
	}
	
	public static LoginResult login(int serverId, String user, String password, boolean secureLogin){
		Server server = ServerManager.getInstance().getServer(serverId);
		if(secureLogin) {
			password = CipherUtil.sha256(password);
		}
		server.setSecureMode(secureLogin);
		return silentLogin(server, user, password);
	}
	
	public static LoginResult silentLogin(Server server, String user, String password){
		LoginResult result = new LoginResult();
		try {
			MapPack param = new MapPack();
			param.put("id", user);
			param.put("pass", password);
			param.put("version", Version.getClientFullVersion());
			param.put("hostname", SysJMX.getHostName());
			
			MapPack out = TcpProxy.loginProxy(server.getId(), param);
			if (out == null) {
				result.success = false;
				result.errorMessage = "Network connection failed";
			} else {
				long session = out.getLong("session");
				String error = out.getText("error");
				if(error != null && session == 0L){
					result.success = false;
					result.errorMessage = "Authentication failed";
					return result;
				}
				server.setOpen(true);
				long time = out.getLong("time");
				String serverName = out.getText("server_id");
				String type = out.getText("type");
				String version = out.getText("version");
				String email = out.getText("email");
				String timezone = out.getText("timezone");
				int soTimeOut = out.getInt("so_time_out");
				
				server.setSession(session);
				server.setName(serverName);
				server.setDelta(time);
				server.setUserId(user);
				server.setPassword(password);
				server.setGroup(type);
				server.setVersion(version);
				server.setEmail(email);
				server.setTimezone(timezone);
				server.setSoTimeOut(soTimeOut);
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
				result.success = true;
			}
		} catch(Exception e){
			e.printStackTrace();
			result.success = false;
			result.errorMessage = "Network connection failed : " + e.getMessage();
		}
		return result;
	}
	
	public static MapPack getCounterXmlServer(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		try {
			p = tcp.getSingle(RequestCmd.GET_XML_COUNTER, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return (MapPack) p;
	}
	
}
