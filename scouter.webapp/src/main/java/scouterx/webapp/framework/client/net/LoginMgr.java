/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouterx.webapp.framework.client.net;

import scouter.Version;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CipherUtil;
import scouter.util.SysJMX;
import scouterx.webapp.framework.client.server.Server;

public class LoginMgr{
	public static LoginRequest login(Server server) {
		String encPassword = CipherUtil.sha256(server.getPassword());
		server.setSecureMode(true);
		return loginInternal(server, server.getUserId(), encPassword);
	}
	
	public static LoginRequest loginInternal(Server server, String user, String encPassword){
		LoginRequest result = new LoginRequest();
		try {
			MapPack param = new MapPack();
			param.put("id", user);
			param.put("pass", encPassword);
			param.put("version", Version.getClientFullVersion());
			param.put("hostname", SysJMX.getHostName());
			param.put("internal", "true");
			
			MapPack out = TcpProxy.loginByCleanConnection(server.getId(), param);
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
				long time = out.getLong("time");
				String serverName = out.getText("server_id");
				String type = out.getText("type");
				String version = out.getText("version");
				String recommendedClientVersion = out.getText("client_version");
				String email = out.getText("email");
				String timezone = out.getText("timezone");
				int soTimeOut = out.getInt("so_time_out");
				
				server.setSession(session);
				server.setName(serverName);
				server.setDelta(time);
				server.setGroup(type);
				server.setVersion(version);
				server.setRecommendedClientVersion(recommendedClientVersion);
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
				server.getConnectionPool().initPool(server.getId());
				server.setOpen(true);

				refreshCounterEngine(server);
				result.success = true;
			}
		} catch(Exception e){
			e.printStackTrace();
			result.success = false;
			result.errorMessage = "Network connection failed : " + e.getMessage();
		}
		return result;
	}

	public static MapPack getCounterXmlServer(Server server) {
		TcpProxy tcp = TcpProxy.getTcpProxy(server);
		Pack p = null;
		try {
			p = tcp.getSingle(RequestCmd.GET_XML_COUNTER, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			TcpProxy.close(tcp);
		}
		return (MapPack) p;
	}

	public static void refreshCounterEngine(Server server) {
		CounterEngine counterEngine = new CounterEngine();
		MapPack m = getCounterXmlServer(server);
		if (m != null) {
			Value v1 = m.get("default");
			counterEngine.parse(((BlobValue)v1).value);
			v1 = m.get("custom");
			if (v1 != null) {
				counterEngine.parse(((BlobValue)v1).value);
			}
			server.setCounterEngine(counterEngine);
		}
	}
	
}
