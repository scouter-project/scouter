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
import scouter.client.util.ConsoleProxy;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;

public class ServerDataProxy {

	public static MapPack getThreadList(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.SERVER_THREAD_LIST, null);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static MapPack getThreadDetail(long id, int serverId) {
		MapPack param = new MapPack();
		param.put("id", id);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.SERVER_THREAD_DETAIL, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static MapPack getEnv(int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.SERVER_ENV, null);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

}
