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
package scouter.server.http.handler;

import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import scouter.io.DataOutputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.FloatValue;
import scouter.net.NetCafe;
import scouter.server.CounterManager;
import scouter.server.netio.data.NetDataProcessor;
import scouter.util.HashUtil;
import scouter.util.IPUtil;
import scouter.util.StringUtil;


/*
 [
 {
 	"object" : {
 	    "host" : "VM123",
 		"name" : "my_server1",
 		"type" : "redis",
 		"address" : "10.10.10.10"
 	},
	"counters" : [
		{"name" : "aof_rewrite_scheduled", "value" : 55},
		{"name" : "client_longest_output_list", "value" : 245},
		{"name" : "used_cpu_user", "value" : 4245}
	]
 },
 {
 	"object" : {
 	    "host" : "VM123",
 		"name" : "my_server2",
 		"type" : "redis",
 		"address" : "10.10.10.10"
 	},
	"counters" : [
		{"name" : "aof_rewrite_scheduled", "value" : 35},
		{"name" : "client_longest_output_list", "value" : 65},
		{"name" : "used_cpu_user", "value" : 8888}
	]
 }
  
 ]
 */
public class CounterHandler {
	
	static CounterManager counterManager = CounterManager.getInstance();
	
	public static void process(Reader in) {
		JSONParser parser = new JSONParser();
		try {
			Object o = parser.parse(in);
			if (o instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) o;
				for (int i = 0; i <jsonArray.size(); i++) {
					Object element = jsonArray.get(i);
					if (element instanceof JSONObject) {
						process((JSONObject) element);
					}
				}
			} else if (o instanceof JSONObject) {
				process((JSONObject) o);
			} else {
				scouter.server.Logger.println("SC-8001", 30, "Incorrect body");
			}
		} catch(Throwable th) {
			scouter.server.Logger.println("SC-8000", 30, "Http body parsing error", th);
		}
	}
	
	private static void process(JSONObject json) throws Exception {
		JSONObject objectInfo = (JSONObject) json.get("object");
		if (objectInfo != null) {
			ObjectPack objPack = extractObjectPack(objectInfo);
			InetAddress addr = extractIpv4Address(objectInfo);
			passToNetDataProcessor(objPack, addr);
			JSONArray perfArray = (JSONArray) json.get("counters");
			if (perfArray != null) {
				PerfCounterPack perfPack = extractPerfCounterPack(perfArray, objPack.objName);
				passToNetDataProcessor(perfPack, addr);
			}
		}
	}
	
	private static ObjectPack extractObjectPack(JSONObject objJson) {
		String host = (String) objJson.get("host");
		String name = (String) objJson.get("name");
		String objName = getObjName(host, name);
		int objHash = HashUtil.hash(objName);
		ObjectPack objPack = new ObjectPack();
		objPack.objHash = objHash;
		objPack.objName = objName;
		objPack.objType = (String) objJson.get("type");
		objPack.address = (String) objJson.get("address");
		return objPack;
	}
	
	private static InetAddress extractIpv4Address(JSONObject objJson) throws UnknownHostException {
		String address = (String) objJson.get("address");
		InetAddress addr = InetAddress.getByAddress(IPUtil.toBytes(address));
		return addr;
	}
	
	private static PerfCounterPack extractPerfCounterPack(JSONArray perfJson, String objName) {
		PerfCounterPack perfPack = new PerfCounterPack();
		perfPack.time = System.currentTimeMillis();
		perfPack.timetype = TimeTypeEnum.REALTIME;
		perfPack.objName = objName;
		for (int i = 0; i < perfJson.size(); i++) {
			JSONObject perf = (JSONObject) perfJson.get(i);
			String name = (String) perf.get("name");
			Number value = (Number) perf.get("value");
			perfPack.data.put(name, new FloatValue(value.floatValue()));
		}
		return perfPack;
	}
	
	private static void passToNetDataProcessor(Pack pack, InetAddress addr) throws IOException {
		DataOutputX out = new DataOutputX();
		out.write(NetCafe.CAFE);
		out.write(new DataOutputX().writePack(pack).toByteArray());
		NetDataProcessor.add(out.toByteArray(), addr);
	}
	
	private static String getObjName(String host, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("/");
		sb.append(host);
		if (StringUtil.isNotEmpty(name)) {
			sb.append("/");
			sb.append(name);
		}
		return sb.toString();
	}
}
