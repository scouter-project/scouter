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
package scouter.server.plugin.alert;

import scouter.lang.CounterKey;
import scouter.lang.value.Value;
import scouter.util.LinkedMap;

public class AlertEngine {

	static LinkedMap<CounterKey, RealCounter> realTimeMap = new LinkedMap<CounterKey, RealCounter>().setMax(3000);

	public static void putRealTime(CounterKey key, Value value) {
		AlertRuleLoader loader = AlertRuleLoader.getInstance();
		AlertRule rule = loader.alertRuleTable.get(key.counter);
		if (rule == null)
			return;

		RealCounter realCounter = realTimeMap.get(key);
		AlertConf alertConf = loader.alertConfTable.get(key.counter);

		if (alertConf == null) {
			alertConf = new AlertConf();
		}

		if (realCounter == null) {
			realCounter = new RealCounter(key);
			realTimeMap.put(key, realCounter);
			realCounter.historySize(alertConf.history_size);
			realCounter.silentTime(alertConf.silent_time);
			realCounter.checkTerm(alertConf.check_term);
		}

		if(alertConf.lastModified > realCounter.confLastModified) {
			realCounter.confLastModified = alertConf.lastModified;
			realCounter.historySize(alertConf.history_size);
			realCounter.silentTime(alertConf.silent_time);
			realCounter.checkTerm(alertConf.check_term);
		}
		realCounter.setValue(value);

		if (realCounter.checkTerm() > 0) {
			long now = System.currentTimeMillis();
			if (now - realCounter.checkTerm()*1000 > realCounter.lastCheckTime) {
				realCounter.lastCheckTime = now;
				rule.process(realCounter);
			}
		} else {
			rule.process(realCounter);
		}

		realCounter.addValueHistory((Number) value);
	}

	public static void load() {
		AlertRuleLoader.getInstance();
	}

}
