package scouter.server.core;

import scouter.util.SortUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

public class ServerStat {
	private static StringKeyLinkedMap<Number> stat = new StringKeyLinkedMap<Number>().setMax(1000);

	public static void put(String id, int value) {
		stat.put(id, value);
	}

	public static void put(String id, float value) {
		stat.put(id, value);
	}

	public static void put(String id, long value) {
		stat.put(id, value);
	}

	public static String toString(String key, String prefix) {
		if(prefix==null)
			prefix="";
		boolean emptyKey = StringUtil.isEmpty(key);
		StringBuffer sb = new StringBuffer();
		String[] keys = SortUtil.sort(stat.keyArray());
		for (int i = 0; i < keys.length; i++) {
			if (emptyKey || keys[i].indexOf(key) >= 0) {
				sb.append(prefix).append(keys[i]).append("=").append(stat.get(keys[i])).append("\n");
			}
		}
		return sb.toString();
	}
}
