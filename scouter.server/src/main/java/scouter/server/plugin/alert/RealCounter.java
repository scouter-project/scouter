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

import scouter.lang.AlertLevel;
import scouter.lang.CounterKey;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.server.core.AgentManager;
import scouter.server.core.cache.CounterCache;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

import java.util.Map;

public class RealCounter {
	public long lastCheckTime;

	private Value _value;
	private long _time;
	private LongKeyLinkedMap<Number> _history;
	private IntLongLinkedMap _lastAlertTime;

	private String _objType;
	private String _objName;
	private int _silentTime;
	private int _checkTerm;

	private int _objHash;
	private String _counter;
	private byte _timetype;

	public RealCounter(CounterKey key) {
		this._objHash = key.objHash;
		this._counter = key.counter;
		this._timetype = key.timetype;
	}

	public String counter() {
		return this._counter;
	}

	public int objHash() {
		return this._objHash;
	}

	public String objName() {
		if (_objName != null)
			return _objName;
		ObjectPack a = AgentManager.getAgent(_objHash);
		if (a != null && a.objName != null) {
			_objName = a.objName;
		}
		return _objName;
	}

	public String objType() {
		ObjectPack a = AgentManager.getAgent(_objHash);
		if (a == null)
			return _objType;
		if (a.objType != null) {
			_objType = a.objType;
		}
		return _objType;
	}

	public void value(Value v) {
		this._value = v;
		this._time = System.currentTimeMillis();
	}

	public int intValue() {
		if (_value instanceof Number)
			return ((Number) _value).intValue();
		else
			return 0;
	}

	public float floatValue() {
		if (_value instanceof Number)
			return ((Number) _value).floatValue();
		else
			return 0;
	}

	public int historySize() {
		return _history == null ? 0 : _history.size();
	}

	public int overCount(int value, int sec) {
		return overCount((float) value, sec);
	}

	public int overCount(float value, int sec) {
		if (historySize() == 0)
			return 0;
		long from = System.currentTimeMillis() - sec * 1000L;
		int cnt = 0;
		LongEnumer en = _history.keys();
		while (en.hasMoreElements()) {
			long tm = en.nextLong();
			if (tm < from)
				break;
			Number val = _history.get(tm);
			if (val.floatValue() >= value) {
				cnt++;
			}
		}
		return cnt;
	}

	public int getAvgtoInt(int fromAgoSec, int durationSec) {
		return (int)getAvg(fromAgoSec, durationSec);
	}

	public float getLatestAvg(int durationSec) {
		return getAvg(durationSec, durationSec);
	}

	public int getLatestAvgtoInt(int durationSec) {
		return (int)getLatestAvg(durationSec);
	}

	public float getAvg(int fromAgoSec, int durationSec) {
		if (historySize() == 0)
			return 0;
		long from = System.currentTimeMillis() - fromAgoSec * 1000L;
		long to = from + durationSec * 1000L;

		int cnt = 0;
		float sum = 0;
		LongEnumer en = _history.keys();
		while (en.hasMoreElements()) {
			long tm = en.nextLong();
			if (tm < from)
				break;
			if (tm >= to)
				continue;

			Number val = _history.get(tm);
			sum += val.floatValue();
			cnt++;
		}
		if (cnt == 0) {
			return 0;
		}
		return sum/cnt;
	}

	public long historyOldestTime() {
		if (historySize() == 0)
			return 0;
		long tm = _history.getLastKey();
		long now = System.currentTimeMillis();
		return (now - tm) / 1000;
	}

	public int historyCount(int sec) {
		if (historySize() == 0)
			return 0;
		long from = System.currentTimeMillis() - sec * 1000L;
		int cnt = 0;
		LongEnumer en = _history.keys();
		while (en.hasMoreElements()) {
			long tm = en.nextLong();
			if (tm < from)
				break;
			cnt++;
		}
		return cnt;
	}

	public static void main(String[] args) {
		LongKeyLinkedMap<String> m = new LongKeyLinkedMap<String>();
		m.putLast(10, "10");
		m.putLast(20, "20");
	}

	public void setAlertTime(byte level, long time) {
		if (this._lastAlertTime == null) {
			this._lastAlertTime = new IntLongLinkedMap().setMax(10);
		}
		this._lastAlertTime.put(level, time);
	}

	public void historySize(int size) {
		if (size <= 0) {
			this._history = null;
		} else {
			if (this._history == null) {
				this._history = new LongKeyLinkedMap<Number>();
			}
			this._history.setMax(size);
		}
	}

	public void addValueHistory(Number value) {
		if (this._history == null)
			return;
		long time = System.currentTimeMillis();
		this._history.putFirst(time, value);
	}

	public void silentTime(int sec) {
		this._silentTime = sec;
	}

	public int silentTime() {
		return this._silentTime;
	}

	public void checkTerm(int sec) {
		this._checkTerm = sec;
	}

	public int checkTerm() {
		return this._checkTerm;
	}

	public long lastAlertTime(int level) {
		if (this._lastAlertTime == null)
			return 0;
		return this._lastAlertTime.get(level);
	}

	public void info(String title, String message) {
		AlertUtil.alert(AlertLevel.INFO, this, title, message);
	}

	@Deprecated
	public void warning(String title, String message) {
		AlertUtil.alert(AlertLevel.WARN, this, title, message);
	}

	public void warn(String title, String message) {
		AlertUtil.alert(AlertLevel.WARN, this, title, message);
	}

	public void error(String title, String message) {
		AlertUtil.alert(AlertLevel.ERROR, this, title, message);
	}

	public void fatal(String title, String message) {
		AlertUtil.alert(AlertLevel.FATAL, this, title, message);
	}

	public String counterNames() {
		Map m = CounterCache.getObjectCounters(_objHash, TimeTypeEnum.REALTIME);
		if (m == null)
			return "[]";
		return m.keySet().toString();
	}

	public float floatValue(String counter) {
		Value v = CounterCache.get(new CounterKey(_objHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).floatValue();
		else
			return 0;
	}

	public int intValue(String counter) {
		Value v = CounterCache.get(new CounterKey(_objHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).intValue();
		else
			return 0;
	}
}
