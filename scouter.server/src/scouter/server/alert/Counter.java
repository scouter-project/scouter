package scouter.server.alert;

import scouter.lang.pack.ObjectPack;
import scouter.server.core.AgentManager;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

public class Counter {

	public Number value;
	public int objHash;
	private LongKeyLinkedMap<Number> history;
	private IntLongLinkedMap lastAlertTime;

	private String _objType;

	public Counter(int objHash, int keep_history) {
		this.objHash = objHash;
		this.setHistorySize(keep_history);
	}

	public String objType() {
		ObjectPack a = AgentManager.getAgent(objHash);
		if (a != null && a.objType != null) {
			_objType = a.objType;
			return _objType;
		}
		return _objType;
	}

	public int intValue() {
		return value.intValue();
	}

	public float floatValue() {
		return value.floatValue();
	}

	public int overTimes(int v, int sec) {
		if (history == null)
			return 0;

		long from = System.currentTimeMillis() - sec * 1000L;
		int cnt = 0;
		LongEnumer en = history.keys();
		while (en.hasMoreElements()) {
			long tm = en.nextLong();
			if (tm < from)
				break;
			Number val = history.get(tm);
			if (val.intValue() >= v) {
				cnt++;
			}
		}
		return cnt;
	}

	public static void main(String[] args) {
		LongKeyLinkedMap<String> m = new LongKeyLinkedMap<String>();
		m.putLast(10, "10");
		m.putLast(20, "20");

	}

	public void addAlertHistory(byte level, long time) {
		if (this.lastAlertTime == null) {
			this.lastAlertTime = new IntLongLinkedMap().setMax(10);
		}
		this.lastAlertTime.put(level, time);
	}

	public void setHistorySize(int keep_history) {
		if (keep_history <= 0) {
			this.history = null;
		} else {
			if (this.history == null) {
				this.history = new LongKeyLinkedMap<Number>();
			}
			this.history.setMax(keep_history);
		}
	}

	public void addValueHistory(Number value) {
		if (this.history == null)
			return;
		long time = System.currentTimeMillis();
		this.history.putFirst(time, value);
	}
}
