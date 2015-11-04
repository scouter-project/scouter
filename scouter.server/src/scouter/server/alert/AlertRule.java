package scouter.server.alert;

import scouter.lang.AlertLevel;
import scouter.lang.pack.AlertPack;
import scouter.server.core.AlertCore;

public class AlertRule {

	public void process(Counter c) {
	}

	public void info(Counter c, String title, String message) {
		this.alert(AlertLevel.INFO, c, title, message);
	}

	public void warning(Counter c, String title, String message) {
		this.alert(AlertLevel.WARN, c, title, message);
	}

	public void error(Counter c, String title, String message) {
		this.alert(AlertLevel.ERROR, c, title, message);
	}

	public void fatal(Counter c, String title, String message) {
		this.alert(AlertLevel.FATAL, c, title, message);
	}

	private void alert(byte level, Counter c, String title, String message) {
		AlertPack p = new AlertPack();
		p.level = level;
		p.objHash = c.getObjHash();
		p.objType = c.getObjType();
		p.title = title;
		p.message = message;		
		c.lastAlertTime.put(level, System.currentTimeMillis());
		AlertCore.add(p);
	}
}
