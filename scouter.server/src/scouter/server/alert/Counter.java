package scouter.server.alert;

import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.server.core.AgentManager;
import scouter.util.IntLongLinkedMap;
import scouter.util.LongKeyLinkedMap;

public class Counter {

	public Value value;
	public int objHash;
	public LongKeyLinkedMap<Value> history = new LongKeyLinkedMap<Value>().setMax(30);
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

	public int intValue(){
		return ((Number)value).intValue();
	}
	public float floatValue(){
		return ((Number)value).floatValue();
	}
}
