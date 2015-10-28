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
package scouter.client.util;

import java.util.HashSet;
import java.util.Set;

import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.lang.counters.CounterConstants;
import scouter.util.StringUtil;

public class CounterUtil {

	public static boolean isPercentValue(String objType, String counter) {
		Server server = ServerManager.getInstance().getDefaultServer();
		if (server == null){
			return false;
		}
		String unit = server.getCounterEngine().getCounterUnit(objType, counter);
		if (StringUtil.isNotEmpty(unit)) {
			return unit.trim().equals("%");
		}
		return false;
	}
	
	private static Set<String> counterAvgSet = new HashSet<String>();
	static {
		counterAvgSet.add(CounterConstants.WAS_ERROR_RATE); 
		counterAvgSet.add(CounterConstants.WAS_ELAPSED_TIME);
		counterAvgSet.add(CounterConstants.WAS_ELAPSED_90PCT);
	}
	
	public static String getTotalMode(String objType, String counter) {
		if (counterAvgSet.contains(counter) || isPercentValue(objType, counter)) {
			return "avg";
		}
		return "sum";
	}
}
