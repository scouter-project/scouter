/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.agent.batch.netio.request.handle;

import java.util.Enumeration;

import scouter.agent.batch.Main;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

public class AgentThread {
	@RequestHandler(RequestCmd.OBJECT_BATCH_ACTIVE_LIST)
	public Pack activeThreadList(Pack param) {
		MapPack rPack = new MapPack();
		ListValue keys = rPack.newList("key");
		ListValue bathJobId = rPack.newList("batchJobId");
		ListValue args = rPack.newList("args");
		ListValue pID = rPack.newList("pID");
		ListValue startTime = rPack.newList("startTime");
		ListValue elapsedTime = rPack.newList("elapsedTime");
		ListValue cPUTime = rPack.newList("cPUTime");
		ListValue sqlTotalTime = rPack.newList("sqlTotalTime");
		ListValue sqlTotalRows = rPack.newList("sqlTotalRows");
		ListValue sqlTotalRuns = rPack.newList("sqlTotalRuns");
		ListValue lastStack = rPack.newList("lastStack");

		String key;
		MapPack pack;
		Enumeration<String> en =  Main.batchMap.keys();
		while (en.hasMoreElements()) {
			key = en.nextElement();
			if (key == null || (pack = Main.batchMap.get(key))  == null) {
				continue;
			}
			keys.add(key);
			bathJobId.add(pack.get("batchJobId"));
			args.add(pack.get("args"));
			pID.add(pack.get("pID"));
			startTime.add(pack.get("startTime"));
			elapsedTime.add(pack.get("elapsedTime"));
			cPUTime.add(pack.get("cPUTime"));
			sqlTotalTime.add(pack.get("sqlTotalTime"));
			sqlTotalRows.add(pack.get("sqlTotalRows"));
			sqlTotalRuns.add(pack.get("sqlTotalRuns"));
			if("None".equals(pack.getText("lastStack"))){
				lastStack.add(new Boolean(false));
			}else{
				lastStack.add(new Boolean(true));				
			}
		}
		rPack.put("complete", new BooleanValue(true));
		return rPack;
	}
}
