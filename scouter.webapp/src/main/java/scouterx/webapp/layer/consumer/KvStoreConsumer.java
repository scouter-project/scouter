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

package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class KvStoreConsumer {

	public String get(final String key, final Server server) {
		Value value = null;
		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			value = tcpProxy.getSingleValue(RequestCmd.GET_GLOBAL_KV, new TextValue(key));
		}
		return value.toString();
	}

	public boolean set(final String key, final String value, final Server server) {
		Value returnValue = null;
		MapPack mapPack = new MapPack();
		mapPack.put(ParamConstant.KEY, key);
		mapPack.put(ParamConstant.VALUE, value);

		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			returnValue = tcpProxy.getSingleValue(RequestCmd.SET_GLOBAL_KV, mapPack);
		}
		return (Boolean) returnValue.toJavaObject();
	}
}
