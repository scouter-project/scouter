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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.server.CounterManager;
import scouter.server.LoginManager;
import scouter.server.LoginUser;
import scouter.server.core.AgentManager;
import scouter.server.core.PerfCountCore;
import scouter.server.management.RemoteControl;
import scouter.server.management.RemoteControlManager;
import scouter.util.HashUtil;


/*
 {
 	"object" : {
 	    "host" : "VM123",
 		"name" : "my_server",
 		"type" : "redis",
 		"address" : "10.10.10.10"
 	},
	"perf" : [
		{"name" : "AofRewrScheduled", "unit" : "cnt", "value" : 55},
		{"name" : "ClientLongOutList", "unit" : "cnt", "value" : 245, "total" : false},
		{"name" : "UsedCpuTime", "unit" : "ms", "value" : 4245, "total" : false}
	]
 }
  
 */
public class CounterHandler {
	
	static CounterManager counterManager = CounterManager.getInstance();
	
	public static void process(Reader in) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject json = (JSONObject) parser.parse(in);
			ObjectPack objPack = heartBeat((JSONObject) json.get("object"));
			List<Counter> counterList = processPerfCounter((JSONArray) json.get("perf"), objPack);
			processCounterEngine(objPack, counterList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static ObjectPack heartBeat(JSONObject objJson) {
		String host = (String) objJson.get("host");
		String name = (String) objJson.get("name");
		String objName = getObjName(host, name);
		int objHash = HashUtil.hash(objName);
		ObjectPack objPack = new ObjectPack();
		objPack.objHash = objHash;
		objPack.objName = objName;
		objPack.objType = (String) objJson.get("type");
		objPack.address = (String) objJson.get("address");
		AgentManager.active(objPack);
		return objPack;
	}
	
	private static List<Counter> processPerfCounter(JSONArray perfJson, ObjectPack objPack) {
		List<Counter> counterList = new ArrayList<Counter>();
		int size = perfJson.size();
		if (size > 0) {
			PerfCounterPack perfPack = new PerfCounterPack();
			perfPack.time = System.currentTimeMillis();
			perfPack.timetype = TimeTypeEnum.REALTIME;
			perfPack.objName = objPack.objName;
			for (int i = 0; i < size; i++) {
				JSONObject perf = (JSONObject) perfJson.get(i);
				Counter counter = new Counter();
				String name = (String) perf.get("name");
				counter.setName(name);
				Number value = (Number) perf.get("value");
				perfPack.data.put(name, new FloatValue(value.floatValue()));
				String unit = (String) perf.get("unit");
				counter.setUnit(unit);
				if (perf.containsKey("total")) {
					counter.setTotal((Boolean) perf.get("total"));
				}
				if (perf.containsKey("all")) {
					counter.setAll((Boolean) perf.get("all"));
				}
				counterList.add(counter);
			}
			perfPack.data.put(CounterConstants.COMMON_OBJHASH, new DecimalValue(objPack.objHash));
			perfPack.data.put(CounterConstants.COMMON_TIME, new DecimalValue(perfPack.time));
			PerfCountCore.add(perfPack);
		}
		return counterList;
	}
	
	private static void processCounterEngine(ObjectPack objPack, List<Counter> counterList) {
		ObjectType objectType = counterManager.getCounterEngine().getObjectType(objPack.objType);
		boolean notifyClient = false;
		if (objectType == null) {
			Family family = new Family();
			if (counterManager.getCounterEngine().getFamily(objPack.objType) == null) {
				family.setName(objPack.objType);
			} else {
				family.setName(counterManager.getCounterEngine().getFamily(objPack.objType).getName() + "_custom" + (int)(Math.random() * 10));
			}
			for (int i = 0; i < counterList.size() ; i++) {
				Counter counter = counterList.get(i);
				counter.setFamily(family.getName());
				counter.setDisplayName(counter.getName());
				family.addCounter(counter);
				if (i == 0) {
					family.setMaster(counter.getName());
				}
			}
			counterManager.addFamily(family);
			objectType = new ObjectType();
			objectType.setName(objPack.objType);
			objectType.setDisplayName(objPack.objType);
			objectType.setFamily(family);
			objectType.setIcon("");
			counterManager.addObjectType(objectType);
			notifyClient = true;
		} else {
			
		}
		if (notifyClient) {
			RemoteControl control = new RemoteControl("REFETCH_COUNTER_XML", System.currentTimeMillis(), new MapPack(), 0);
			LoginUser[] users = LoginManager.getLoginUserList();
			for (int i = 0, len = (users != null ? users.length : 0); i < len; i++) {
				long session = users[i].session();
				RemoteControlManager.add(session, control);
			}
		}
	}
	
	private static String getObjName(String host, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("/");
		sb.append(host);
		sb.append("/");
		sb.append(name);
		return sb.toString();
	}
}
