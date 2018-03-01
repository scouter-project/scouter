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
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.KeyValueData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class GlobalKvStoreConsumer {

	public boolean set(final String key, final String value, final Server server) {
		return set(key, value, ParamConstant.TTL_PERMANENT, server);
	}

	public boolean set(final String key, final String value, final long ttl, final Server server) {
		Value returnValue = null;
		MapPack mapPack = new MapPack();
		mapPack.put(ParamConstant.KEY, key);
		mapPack.put(ParamConstant.VALUE, value);
		mapPack.put(ParamConstant.TTL, ttl);

		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			returnValue = tcpProxy.getSingleValue(RequestCmd.SET_GLOBAL_KV, mapPack);
		}
		return returnValue != null ? (Boolean) returnValue.toJavaObject() : false;
	}

	public boolean setTTL(final String key, final long ttl, final Server server) {
		Value returnValue = null;
		MapPack mapPack = new MapPack();
		mapPack.put(ParamConstant.KEY, key);
		mapPack.put(ParamConstant.TTL, ttl);

		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			returnValue = tcpProxy.getSingleValue(RequestCmd.SET_GLOBAL_TTL, mapPack);
		}
		return returnValue != null ? (Boolean) returnValue.toJavaObject() : false;
	}

	public String get(final String key, final Server server) {
		Value value = null;
		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			value = tcpProxy.getSingleValue(RequestCmd.GET_GLOBAL_KV, new TextValue(key));
		}
		return value != null ? value.toString() : null;
	}

	public List<KeyValueData> setBulk(Map<String, String> paramMap, final Server server) {
		return setBulk(paramMap, ParamConstant.TTL_PERMANENT, server);
	}

	public List<KeyValueData> setBulk(Map<String, String> paramMap, Long ttl, final Server server) {
		MapPack input = new MapPack();
		input.put(ParamConstant.KEY_VALUE, MapValue.ofStringValueMap(paramMap));
		input.put(ParamConstant.TTL, ttl);

		MapPack resultPack ;
		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			resultPack = (MapPack) tcpProxy.getSingle(RequestCmd.SET_GLOBAL_KV_BULK, input);
		}

		List<KeyValueData> resultList = new ArrayList<>();
		for (String key : resultPack.keySet()) {
			resultList.add(new KeyValueData(key, resultPack.getBoolean(key)));
		}
		return resultList;
	}

	public List<KeyValueData> getBulk(List<String> paramList, final Server server) {
		MapPack paramPack = new MapPack();
		paramPack.put(ParamConstant.KEY, ListValue.ofStringValueList(paramList));

		MapPack resultPack ;
		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
			resultPack = (MapPack) tcpProxy.getSingle(RequestCmd.GET_GLOBAL_KV_BULK, paramPack);
		}

		List<KeyValueData> resultList = new ArrayList<>();
		for (String key : resultPack.keySet()) {
			resultList.add(new KeyValueData(key, resultPack.getText(key)));
		}
		return resultList;
	}
}
