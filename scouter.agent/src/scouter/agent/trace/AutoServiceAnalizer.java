package scouter.agent.trace;

import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;

public class AutoServiceAnalizer {
	private static StringKeyLinkedMap<String> table = new StringKeyLinkedMap<String>().setMax(200);

	public static void regist(String classMethod, String stack) {
		table.putLast(classMethod, stack);
	}

	public static StringEnumer keys() {
		return table.keys();
	}

	public static String getStack(String key) {
		return table.get(key);
	}
}
