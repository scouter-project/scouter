package scouter.server.alert;

import java.util.HashMap;

import scouter.lang.CounterKey;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.server.core.AgentManager;

public class AlertEngine {
	static HashMap<CounterKey, Counter> realTime = new HashMap<CounterKey, Counter>();

	public static void putRealTime(CounterKey key, Value value) {
		if (value instanceof Number == false) {
			return;
		}
		Counter c = realTime.get(key);
		if (c == null) {
			c = new Counter();
			c.objHash = key.objHash;
			realTime.put(key, c);
		}
		c.value =value;
		
		
		c.history.put(System.currentTimeMillis(), value);
	}

	public static void putDaily(int yyyymmdd, CounterKey key, int hhmm, Value value) {
	}
}
