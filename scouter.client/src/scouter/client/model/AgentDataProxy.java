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
package scouter.client.model;

import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;

import java.util.List;

public class AgentDataProxy {

	public static MapPack getThreadList(int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objHash", objHash);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_LIST, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static List<Pack> getActiveThreadList(String objType, int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objType", objType);
		param.put("objHash", objHash);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return tcp.process(RequestCmd.OBJECT_ACTIVE_SERVICE_LIST, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static List<Pack> getBatchActiveList(String objType, int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objType", objType);
		param.put("objHash", objHash);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return tcp.process(RequestCmd.OBJECT_BATCH_ACTIVE_LIST, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack getThreadDetail(int objHash, long threadId, long txid, int serverId) {
		MapPack param = new MapPack();
		param.put("objHash", objHash);
		param.put("id", threadId);
		param.put("txid", txid);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_DETAIL, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static MapPack getEnv(int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objHash", objHash);
		
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		
		Server server = ServerManager.getInstance().getServer(serverId);
		param.put("userSession", server.getSession());
		param.put("userIp", tcp.getLocalInetAddress().getHostAddress());
		
		try {
			return (MapPack) tcp.getSingle(RequestCmd.OBJECT_ENV, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack getLoadedClassList(int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objHash", objHash);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.OBJECT_CLASS_LIST, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static void resetCache(int objHash, int serverId) {
		MapPack param = new MapPack();
		param.put("objHash", objHash);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			tcp.getSingle(RequestCmd.OBJECT_RESET_CACHE, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}

}
