package scouter.lang.conf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConfObserver {
	private static Map<String, Runnable> observer = new HashMap<String, Runnable>();

	public static void add(String cls, Runnable run) {
		observer.put(cls, run);
	}

	public static void run() {
		try {
			Iterator<Runnable> itr = observer.values().iterator();
			while (itr.hasNext()) {
				itr.next().run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
