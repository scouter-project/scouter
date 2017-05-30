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

		RealCounter counter = realTimeMap.get(key);
		if (counter == null) {
			counter = new RealCounter(key);
			AlertConf conf = loader.alertConfTable.get(key.counter);
			//defensive code for abnormal deletion
			if (conf == null) {
				conf = new AlertConf();
			}
			counter.historySize(conf.history_size);
			counter.silentTime(conf.silent_time);
			counter.checkTerm(conf.check_term);
			realTimeMap.put(key, counter);
		}
		counter.value(value);

		if (counter.checkTerm() > 0) {
			long now = System.currentTimeMillis();
			if (now - counter.checkTerm()*1000 > counter.lastCheckTime) {
				counter.lastCheckTime = now;
				rule.process(counter);
			}
		} else {
			rule.process(counter);
		}

		counter.addValueHistory((Number) value);
	}

	public static void load() {
		AlertRuleLoader.getInstance();
	}

}
