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
package scouter.server.alert;

import scouter.lang.CounterKey;
import scouter.lang.value.Value;
import scouter.util.IntKeyLinkedMap;
import scouter.util.LinkedMap;
import scouter.util.StringKeyLinkedMap;

public class FxAlertEngine {

	static LinkedMap<CounterKey, RealCounter> realTime = new LinkedMap<CounterKey, RealCounter>().setMax(3000);

	public static void putRealTime(CounterKey key, Value value) {
		FxAlertRuleLoader loader = FxAlertRuleLoader.getInstance();
		AlertRule rule = loader.alertRuleTable.get(key.counter);
		if (rule == null)
			return;
		
		RealCounter c = realTime.get(key);
		if (c == null) {
			c = new RealCounter( key);
			AlertConf conf = loader.alertConfTable.get(key.counter);
			//일시적으로 삭제되었을 가능성에 대비 
			if (conf == null) {
				conf = new AlertConf();
			}
			c.historySize(conf.history_size);
			c.silentTime(conf.silent_time);
			realTime.put(key, c);
		}
		c.value(value);
		rule.process(c);
		c.addValueHistory((Number) value);
	}



	public static void load() {
		FxAlertRuleLoader.getInstance();
	}

}
