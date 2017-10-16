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
 */

package scouter.test;

import java.util.ArrayList;

import scouter.agent.AgentBoot;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.task.AgentHeartBeat;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.util.HashUtil;
import scouter.util.ShellArg;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

public class ObjectRush {
	public static void main(String[] args) {
		ShellArg sh = new ShellArg(args);
		String server = sh.get("-h", "127.0.0.1");
		String port = sh.get("-p", "6100");
		int objNum = Integer.valueOf(sh.get("-n", "20"));

		System.setProperty("server.addr", server);
		System.setProperty("server.port", port);
		
		AgentBoot.boot();
		
		ArrayList<String> objNames = new ArrayList<String>();
		
		for (int i = 0; i < objNum; i++) {
			String objName = "/" + SysJMX.getHostName() + "/dummy_java_instance_" + (i+1);
			objNames.add(objName);
			int objHash = HashUtil.hash(objName);
			AgentHeartBeat.addObject(CounterConstants.JAVA, objHash, objName);
		}
		CounterBasket basket = new CounterBasket();
		int count = 0;
		while (true) {
			//Logger.info("*********** " + (++count) + " ***********");
			for (String objName : objNames) {
				PerfCounterPack pcp = basket.getPack(objName, TimeTypeEnum.REALTIME);
				pcp.put(CounterConstants.WAS_TPS, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_ACTIVE_SPEED, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_ELAPSED_TIME, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_ELAPSED_90PCT, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_SERVICE_COUNT, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_ERROR_RATE, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_RECENT_USER, new DecimalValue(5));
				pcp.put(CounterConstants.WAS_ACTIVE_SERVICE, new DecimalValue(5));
				pcp.put(CounterConstants.JAVA_GC_COUNT, new DecimalValue(5));
				pcp.put(CounterConstants.JAVA_GC_TIME, new DecimalValue(5));
				pcp.put(CounterConstants.JAVA_HEAP_USED, new DecimalValue(5));
				pcp.put(CounterConstants.JAVA_PERM_USED, new DecimalValue(5));
				pcp.put(CounterConstants.JAVA_PROCESS_CPU, new DecimalValue(5));
				int act1 = (int) (Math.random() * 10);
				int act2 = (int) (Math.random() * 10);
				int act3 = (int) (Math.random() * 10);
				int active = act1 + act2 + act3;
				//Logger.info(objName + " : " + active);
				ListValue activeSpeed = new ListValue();
				activeSpeed.add(act1);
				activeSpeed.add(act2);
				activeSpeed.add(act3);
				pcp.put(CounterConstants.WAS_ACTIVE_SPEED, activeSpeed);
				pcp.put(CounterConstants.WAS_ACTIVE_SERVICE, new DecimalValue(active));
				PerfCounterPack fiveminPcp = basket.getPack(objName, TimeTypeEnum.FIVE_MIN);
				fiveminPcp.put(CounterConstants.WAS_TPS, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_ACTIVE_SPEED, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_ELAPSED_TIME, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_ELAPSED_90PCT, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_SERVICE_COUNT, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_ERROR_RATE, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_RECENT_USER, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.WAS_ACTIVE_SERVICE, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.JAVA_GC_COUNT, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.JAVA_GC_TIME, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.JAVA_HEAP_USED, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.JAVA_PERM_USED, new DecimalValue(5));
				fiveminPcp.put(CounterConstants.JAVA_PROCESS_CPU, new DecimalValue(5));
				DataProxy.sendCounter(new PerfCounterPack[]{pcp, fiveminPcp});
			}
			ThreadUtil.sleep(2000);
		}
	}
}