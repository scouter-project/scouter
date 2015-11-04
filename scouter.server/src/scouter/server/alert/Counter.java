package scouter.server.alert;

import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.server.core.AgentManager;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

public class Counter {

	public Number value;
	public int objHash;
	public LongKeyLinkedMap<Number> history = new LongKeyLinkedMap<Number>().setMax(30);
	public IntLongLinkedMap lastAlertTime = new IntLongLinkedMap().setMax(10);

	private String _objType;

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

	
	public int over(int v, int sec) {
		long from = System.currentTimeMillis() - sec*1000L;
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
		LongKeyLinkedMap<String >m = new LongKeyLinkedMap<String>();
		m.putLast(10, "10");
		m.putLast(20, "20");
		
	}
}
