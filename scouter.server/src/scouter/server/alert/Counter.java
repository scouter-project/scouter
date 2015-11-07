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

import java.util.Enumeration;

import scouter.lang.AlertLevel;
import scouter.lang.CounterKey;
import scouter.lang.pack.ObjectPack;
import scouter.server.core.AgentManager;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

public class Counter {

	private Number _value;
	private LongKeyLinkedMap<Number> _history;
	private IntLongLinkedMap _lastAlertTime;

	private int _objHash;
	private String _objType;
	private String _objName;
	private int _silentTime;
	private String _name;

	public Counter(String name, int objHash) {
		this._objHash = objHash;
		this._name = name;
	}

	public String name() {
		return this._name;
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

	public void value(Number value2) {
		this._value = value2;
	}

	public int intValue() {
		return _value.intValue();
	}

	public float floatValue() {
		return _value.floatValue();
	}

	public int historySize() {
		return _history == null ? 0 : _history.size();
	}

	public int overCount(int value, int sec) {
		return overCount((float) value, sec);
	}

	public int overCount(float value, int sec) {
		if (_history == null)
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

	public long historyHowOld() {
		if (_history == null)
			return 0;
		long now = System.currentTimeMillis();
		return now - _history.getLastKey();
	}

	public int historyCount(int sec) {
		if (_history == null)
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

	public long lastAlertTime(int level) {
		if (this._lastAlertTime == null)
			return 0;
		return this._lastAlertTime.get(level);
	}

	public void info(String title, String message) {
		AlertEngUtil.alert(AlertLevel.INFO, this, title, message);
	}

	public void warning(String title, String message) {
		AlertEngUtil.alert(AlertLevel.WARN, this, title, message);
	}

	public void error(String title, String message) {
		AlertEngUtil.alert(AlertLevel.ERROR, this, title, message);
	}

	public void fatal(String title, String message) {
		AlertEngUtil.alert(AlertLevel.FATAL, this, title, message);
	}
}
