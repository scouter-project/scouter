package scouter.server.alert;

import scouter.lang.pack.AlertPack;
import scouter.server.core.AlertCore;

public class AlertEngUtil {

	public static void alert(byte level, Counter c, String title, String message) {
		long now = System.currentTimeMillis();
		if (c.silentTime() > 0) {
			if (now < c.lastAlertTime(level) + c.silentTime() * 1000)
				return;
		}
		AlertPack p = new AlertPack();
		p.time = now;
		p.level = level;
		p.objHash = c.objHash();
		p.objType = c.objType();
		p.title = title;
		p.message = message;
		c.setAlertTime(level, now);
		AlertCore.add(p);
	}

}
