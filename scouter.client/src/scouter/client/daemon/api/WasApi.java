/*
 *  Copyright 2015 the original author or authors.
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
package scouter.client.daemon.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;

public class WasApi {
	
	
	public static Stack<Value> getCounterRealtime(MapPack param, int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final Stack<Value> value = new Stack<Value>();
		try {
			tcp.process(RequestCmd.COUNTER_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Value v = in.readValue();
					if (v != null && v.getValueType() != ValueEnum.NULL) {
						value.push(v);
					}
				}
			});
			
			return value;
		} catch(Throwable t){
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	/**
	 * MapPack param = new MapPack(); <br>
	 * param.put("counter", "TPS"); <br>
	 * param.put("objType", "tomcat");
	 * 
	 * @param param
	 * @return MapPack
	 */
	public static MapPack getCounterRealtimeAll(MapPack param, int serverId) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(RequestCmd.COUNTER_REAL_TIME_ALL, param);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack getCounterPasttime(MapPack param, int serverId){
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack out = null;
		try {
			out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_PAST_DATE, param);
			return out;
		} catch (Throwable t) {
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static ArrayList<MapPack> getCounterPasttimeAll(MapPack param, int serverId){
		final ArrayList<MapPack> values = new ArrayList<MapPack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			tcp.process(RequestCmd.COUNTER_PAST_LONGDATE_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					MapPack mpack = (MapPack) in.readPack();
					values.add(mpack);
				};
			});
			return values;
		} catch (Throwable t) {
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
	public static MapPack COUNTER_REAL_TIME_ALL(String counter, String objType, int serverId){
		MapPack param = new MapPack();
		param.put("counter", counter);
		param.put("objType", objType);
		return getCounterRealtimeAll(param, serverId);
	}
	public static MapPack COUNTER_REAL_TIME(String counter, String objType, int serverId){
		MapPack param = new MapPack();
		param.put("counter", counter);
		param.put("objType", objType);
		return getCounterRealtimeAll(param, serverId);
	}
}
