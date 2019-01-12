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
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.Internal;
import scouter.lang.conf.ParamDesc;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.server.core.AgentManager;
import scouter.server.core.cache.CounterCache;
import scouter.util.HashUtil;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RealCounter {
	private static List<Desc> realCounterDesc;

	public long confLastModified;
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

	@ConfigDesc("get current counter name.")
	public String getCounter() {
		return this._counter;
	}

	@Deprecated
	public String counter() {
		return this._counter;
	}

	@ConfigDesc("get current object hash value.")
	public int getObjHash() {
		return this._objHash;
	}

	@Deprecated
	public int objHash() {
		return this._objHash;
	}

	@ConfigDesc("get current object name.")
	public String getObjName() {
		if (_objName != null)
			return _objName;
		ObjectPack a = AgentManager.getAgent(_objHash);
		if (a != null && a.objName != null) {
			_objName = a.objName;
		}
		return _objName;
	}

	@Deprecated
	public String objName() {
		return getObjName();
	}

	@ConfigDesc("get current object type name.")
	public String getObjType() {
		ObjectPack a = AgentManager.getAgent(_objHash);
		if (a == null)
			return _objType;
		if (a.objType != null) {
			_objType = a.objType;
		}
		return _objType;
	}

	@ConfigDesc("get comma separated object hashes string of the given object type.")
	public String getObjHashListString(String objType) {
		return AgentManager.getObjHashListAsString(objType);
	}

	@ConfigDesc("get comma separated object hashes string of the given object type.")
	public String getObjHashListWithParentsString(String objType) {
		String[] hashes = getObjHashListString(objType).split(",");
		List<Integer> hashesWithParents = new ArrayList<>();
		for (String hash : hashes) {
			int objHash = Integer.parseInt(hash);
			hashesWithParents.add(objHash);
			ObjectPack objectPack = AgentManager.getAgent(objHash);
			String objName = objectPack.objName;
			if (objName.lastIndexOf('/') > 0) {
				hashesWithParents.add(HashUtil.hash(objName.substring(0, objName.lastIndexOf('/'))));
			}
		}
		return hashesWithParents.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	@Deprecated
	public String objType() {
		return getObjType();
	}

	@Internal
	public void setValue(Value v) {
		this._value = v;
		this._time = System.currentTimeMillis();
	}

	@Deprecated
	public void value(Value v) {
		setValue(v);
	}

	@ConfigDesc("get value of current counter as int.")
	public int getIntValue() {
		if (_value instanceof Number)
			return ((Number) _value).intValue();
		else
			return 0;
	}

	@Deprecated
	public int intValue() {
		return getIntValue();
	}

	@ConfigDesc("get value of current counter as float.")
	public float getFloatValue() {
		if (_value instanceof Number)
			return ((Number) _value).floatValue();
		else
			return 0;
	}

	@Deprecated
	public float floatValue() {
		return getFloatValue();
	}

	@ConfigDesc("get the history size of the counter.")
	public int getHistorySize() {
		return _history == null ? 0 : _history.size();
	}

	@Deprecated
	public int historySize() {
		return getHistorySize();
	}

	@ConfigDesc("get how many times exceed the given value of the counter.")
	@ParamDesc("int value, int sec")
	public int getOverCount(int value, int sec) {
		return getOverCount((float) value, sec);
	}

	@Deprecated
	public int overCount(int value, int sec) {
		return getOverCount(value, sec);
	}

	@ConfigDesc("get how many times exceed the given value of the counter.")
	@ParamDesc("float value, int sec")
	public int getOverCount(float value, int sec) {
		if (getHistorySize() == 0)
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

	@Deprecated
	public int overCount(float value, int sec) {
		return getOverCount(value, sec);
	}

	@ConfigDesc("get average value of the counter in the given duration as int.")
	@ParamDesc("int fromAgoSec, int durationSec")
	public int getAvgToInt(int fromAgoSec, int durationSec) {
		return (int)getAvg(fromAgoSec, durationSec);
	}

	@Deprecated
	public int getAvgtoInt(int fromAgoSec, int durationSec) {
		return getAvgToInt(fromAgoSec, durationSec);
	}

	@ConfigDesc("get latest average value of the counter in the given duration.")
	@ParamDesc("int durationSec")
	public float getLatestAvg(int durationSec) {
		return getAvg(durationSec, durationSec);
	}

	@ConfigDesc("get latest average value of the counter in the given duration as int.")
	@ParamDesc("int durationSec")
	public int getLatestAvgToInt(int durationSec) {
		return (int)getLatestAvg(durationSec);
	}

	@Deprecated
	public int getLatestAvgtoInt(int durationSec) {
		return getLatestAvgToInt(durationSec);
	}

	@ConfigDesc("get average value of the counter in the given duration.")
	@ParamDesc("int fromAgoSec, int durationSec")
	public float getAvg(int fromAgoSec, int durationSec) {
		if (getHistorySize() == 0)
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

	@ConfigDesc("get oldest time of the counter's history.")
	public long getHistoryOldestTime() {
		if (getHistorySize() == 0)
			return 0;
		long tm = _history.getLastKey();
		long now = System.currentTimeMillis();
		return (now - tm) / 1000;
	}

	@Deprecated
	public long historyOldestTime() {
		return getHistoryOldestTime();
	}

	@ConfigDesc("get how many values (of the current counter) exist in the seconds.")
	public int getHistoryCountInSec(int sec) {
		if (getHistorySize() == 0)
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

	@Deprecated
	public int historyCount(int sec) {
		return getHistoryCountInSec(sec);
	}

	@ConfigDesc("get the counter's value as float.")
	@ParamDesc("String counter")
	public float getFloatValue(String counter) {
		Value v = CounterCache.get(new CounterKey(_objHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).floatValue();
		else
			return 0;
	}

	@ConfigDesc("get the another objects counter's value as float.")
	@ParamDesc("String objectFullName, String counter")
	public float getFloatValue(String objectFullName, String counter) {
		int anotherObjHash = HashUtil.hash(objectFullName);
		Value v = CounterCache.get(new CounterKey(anotherObjHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).floatValue();
		else
			return 0;
	}

	@Deprecated
	public float floatValue(String counter) {
		return getFloatValue(counter);
	}

	@ConfigDesc("get the counter's value as int.")
	@ParamDesc("String counter")
	public int getIntValue(String counter) {
		Value v = CounterCache.get(new CounterKey(_objHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).intValue();
		else
			return 0;
	}

	@ConfigDesc("get the another objects counter's value as int.")
	@ParamDesc("String objectFullName, String counter")
	public int getIntValue(String objectFullName, String counter) {
		int anotherObjHash = HashUtil.hash(objectFullName);
		Value v = CounterCache.get(new CounterKey(anotherObjHash, counter, _timetype));
		if (v instanceof Number)
			return ((Number) v).intValue();
		else
			return 0;
	}

	@Deprecated
	public int intValue(String counter) {
		return getIntValue(counter);
	}

	@ConfigDesc("alert on info level.")
	@ParamDesc("String title, String message")
	public void info(String title, String message) {
		AlertUtil.alert(AlertLevel.INFO, this, title, message);
	}

	@Deprecated
	public void warning(String title, String message) {
		AlertUtil.alert(AlertLevel.WARN, this, title, message);
	}

	@ConfigDesc("alert on warn level.")
	@ParamDesc("String title, String message")
	public void warn(String title, String message) {
		AlertUtil.alert(AlertLevel.WARN, this, title, message);
	}

	@ConfigDesc("alert on error level.")
	@ParamDesc("String title, String message")
	public void error(String title, String message) {
		AlertUtil.alert(AlertLevel.ERROR, this, title, message);
	}

	@ConfigDesc("alert on fatal level.")
	@ParamDesc("String title, String message")
	public void fatal(String title, String message) {
		AlertUtil.alert(AlertLevel.FATAL, this, title, message);
	}


	@Internal
	public void setAlertTime(byte level, long time) {
		if (this._lastAlertTime == null) {
			this._lastAlertTime = new IntLongLinkedMap().setMax(10);
		}
		this._lastAlertTime.put(level, time);
	}

	@Internal
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

	@Internal
	public void addValueHistory(Number value) {
		if (this._history == null)
			return;
		long time = System.currentTimeMillis();
		this._history.putFirst(time, value);
	}

	@Internal
	public void silentTime(int sec) {
		this._silentTime = sec;
	}

	@Internal
	public int silentTime() {
		return this._silentTime;
	}

	@Internal
	public void checkTerm(int sec) {
		this._checkTerm = sec;
	}

	@Internal
	public int checkTerm() {
		return this._checkTerm;
	}

	@Internal
	public long lastAlertTime(int level) {
		if (this._lastAlertTime == null)
			return 0;
		return this._lastAlertTime.get(level);
	}

	@Internal
	public String counterNames() {
		Map m = CounterCache.getObjectCounters(_objHash, TimeTypeEnum.REALTIME);
		if (m == null)
			return "[]";
		return m.keySet().toString();
	}

	public static class Desc {
		public String desc;
		public String methodName;
		public List<String> parameterTypeNames;
		public String returnTypeName;
	}

	public static synchronized List<Desc> getRealCounterDescription() {
		if (realCounterDesc != null) {
			return realCounterDesc;
		}
		List<Desc> descList = new ArrayList<Desc>();
		Method[] methods = RealCounter.class.getDeclaredMethods();
		for (Method method : methods) {
			int mod = method.getModifiers();
			if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
				Deprecated deprecated = method.getAnnotation(Deprecated.class);
				Internal internal = method.getAnnotation(Internal.class);
				if (deprecated != null || internal != null) {
					continue;
				}

				List<String> typeClassNameList = new ArrayList<String>();

				Class<?>[] clazzes = method.getParameterTypes();
				ParamDesc paramDesc = method.getAnnotation(ParamDesc.class);
				if (paramDesc != null) {
					typeClassNameList.add(paramDesc.value());
				} else {
					for (Class<?> clazz : clazzes) {
						typeClassNameList.add(clazz.getName());
					}
				}
				ConfigDesc configDesc = method.getAnnotation(ConfigDesc.class);

				Desc desc = new Desc();
				desc.methodName = method.getName();
				desc.returnTypeName = method.getReturnType().getName();
				if (configDesc != null) {
					desc.desc = configDesc.value();
				}
				desc.parameterTypeNames = typeClassNameList;
				descList.add(desc);
			}
		}
		realCounterDesc = descList;
		return realCounterDesc;
	}
}
