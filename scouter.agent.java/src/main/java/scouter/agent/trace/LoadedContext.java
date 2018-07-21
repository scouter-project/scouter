package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.util.StringLinkedSet;

public class LoadedContext {
	public static StringLinkedSet ctxSet = new StringLinkedSet().setMax(100);
	public static void put(Object ds) {
		String old = ctxSet.put(ds.getClass().getName());
		if (old == null && Configure.getInstance()._log_datasource_lookup_enabled) {
			Logger.println("DataSource lookup : " + ds.getClass().getName());
		}
	}
}
