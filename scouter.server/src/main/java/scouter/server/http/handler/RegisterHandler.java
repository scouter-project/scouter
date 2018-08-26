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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.lang.pack.MapPack;
import scouter.server.CounterManager;
import scouter.server.LoginManager;
import scouter.server.LoginUser;
import scouter.server.management.RemoteControl;
import scouter.server.management.RemoteControlManager;
import scouter.server.util.AsyncRun;


/*
{
	"object" : {
		"type" : "redis",
		"display" : "Redis"
	},
	"counters" : [
		{"name" : "aof_rewrite_scheduled",
		 "unit" : "cnt",
		 "display" : "AofRewriteScheduled",
		 "total" : true,
		 "all" : true 
		},
		{"name" : "client_longest_output_list",
		 "unit" : "cnt",
		 "display" : "ClientLongOutList",
		},
		{"name" : "used_cpu_user",
		 "unit" : "cnt",
		 "display" : "UsedCpuUser",
		 "total" : false
		},
	]
}
*/
public class RegisterHandler {
	
	static CounterManager counterManager = CounterManager.getInstance();
	
	public static boolean process(Reader in) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject json = (JSONObject) parser.parse(in);
			JSONObject objectInfo = (JSONObject) json.get("object");
			JSONArray countersArray = (JSONArray) json.get("counters");
			String objType = (String) objectInfo.get("type");
			
			ObjectType objectType = counterManager.getCounterEngine().getObjectType(objType);
			if (objectType == null) {
				objectType = new ObjectType();
				objectType.setName(objType);
				if (objectInfo.containsKey("display")) {
					objectType.setDisplayName((String) objectInfo.get("display"));
				} else {
					objectType.setDisplayName(objType);
				}
				objectType.setIcon("");
				Family family = new Family();
				objectType.setFamily(family);
				if (counterManager.getCounterEngine().getFamily(objType) == null) {
					family.setName(objType);
				} else {
					family.setName(objType + "." + (Math.random() * 100));
				}
				int counterSize = countersArray.size();
				for (int i = 0; i < counterSize; i++) {
					JSONObject counterInfo = (JSONObject)countersArray.get(i);
					String name = (String) counterInfo.get("name");
					String unit = (String) counterInfo.get("unit");
					String display = name;
					if (counterInfo.containsKey("display")) {
						display = (String) counterInfo.get("display");
					}
					boolean total = true;
					if (counterInfo.containsKey("total")) {
						total = (Boolean) counterInfo.get("total");
					}
					boolean all = true;
					if (counterInfo.containsKey("all")) {
						all = (Boolean) counterInfo.get("all");
					}
					Counter counter = new Counter();
					counter.setName(name);
					counter.setUnit(unit);
					counter.setDisplayName(display);
					counter.setIcon("");
					counter.setTotal(total);
					counter.setAll(all);
					family.addCounter(counter);
					if (i == 0) {
						family.setMaster(name);
					}
				}
				boolean result = counterManager.addFamilyAndObjectType(family, objectType);
				if (result) {
					notifyAllClients();
					return true;
				}
			} else {
				ObjectType addObjectType = new ObjectType();
				addObjectType.setName(objType);
				addObjectType.setFamily(objectType.getFamily());
				addObjectType.setIcon(objectType.getIcon());
				addObjectType.setSubObject(objectType.isSubObject());
				if (objectInfo.containsKey("display")) {
					addObjectType.setDisplayName((String) objectInfo.get("display"));
				} else {
					addObjectType.setDisplayName(objectType.getDisplayName());
				}
				int counterSize = countersArray.size();
				for (int i = 0; i < counterSize; i++) {
					JSONObject counterInfo = (JSONObject)countersArray.get(i);
					String name = (String) counterInfo.get("name");
					if (objectType.getCounter(name) != null) continue;
					String unit = (String) counterInfo.get("unit");
					String display = name;
					if (counterInfo.containsKey("display")) {
						display = (String) counterInfo.get("display");
					}
					boolean total = true;
					if (counterInfo.containsKey("total")) {
						total = (Boolean) counterInfo.get("total");
					}
					boolean all = true;
					if (counterInfo.containsKey("all")) {
						all = (Boolean) counterInfo.get("all");
					}
					Counter counter = new Counter();
					counter.setName(name);
					counter.setUnit(unit);
					counter.setDisplayName(display);
					counter.setIcon("");
					counter.setTotal(total);
					counter.setAll(all);
					addObjectType.addCounter(counter);
				}
				boolean result = counterManager.editObjectType(addObjectType);
				if (result) {
					notifyAllClients();
					return true;
				}
			}
		} catch(Throwable th) {
			scouter.server.Logger.println("SC-8001", 30, "Http body parsing error", th);
			return false;
		}
		return false;
	}
	
	public static void notifyAllClients() {
		AsyncRun.getInstance().add(new Runnable() {
			public void run() {
				RemoteControl control = new RemoteControl("REFETCH_COUNTER_XML", System.currentTimeMillis(), new MapPack(), 0);
				LoginUser[] users = LoginManager.getLoginUserList();
				for (int i = 0, len = (users != null ? users.length : 0); i < len; i++) {
					long session = users[i].session();
					RemoteControlManager.add(session, control);
				}
			}
		});
	}
}
