package scouter.server.alert;

import java.util.HashMap;

import scouter.lang.CounterKey;
import scouter.lang.value.Value;

public class AlertEngine {
	static HashMap<CounterKey, Counter> realTime = new HashMap<CounterKey, Counter>();

	public static void putRealTime(CounterKey key, Value value) {
		RuleLoader loader = RuleLoader.getInstance();
		AlertRule rule = loader.alertRuleTable.get(key.counter);
		AlertConf conf = loader.alertConfTable.get(key.counter);
		if (rule == null || conf == null)
			return;
		if (value instanceof Number == false) {
			return;
		}
		Counter c = realTime.get(key);
		if (c == null) {
			c = new Counter(key.objHash, conf.keep_history);
			realTime.put(key, c);
		}
		c.value = (Number) value;
		rule.process(c);
		c.addValueHistory((Number) value);
	}

	public static void putDaily(int yyyymmdd, CounterKey key, int hhmm, Value value) {
	}

	public static void load() {
		RuleLoader.getInstance();
	}

}
