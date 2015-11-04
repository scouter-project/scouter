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

	public int getObjHash() {
		return objHash;
	}

	private String objType;

	public String getObjType() {
		ObjectPack a = AgentManager.getAgent(objHash);
		if (a != null && a.objType != null) {
			objType = a.objType;
			return objType;
		}
		return objType;
	}

}
