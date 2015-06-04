package scouter.server.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.db.DBCtr;
import scouter.server.db.XLogWR;
import scouter.util.SystemUtil;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.ThreadUtil;

public class AutoDeleteScheduler extends Thread {

	private static AutoDeleteScheduler instance = null;
	private static final long CHECK_INTERVAL = DateUtil.MILLIS_PER_MINUTE;
	Configure conf = Configure.getInstance();
	
	public final static synchronized AutoDeleteScheduler getInstance() {
		if (instance == null) {
			instance = new AutoDeleteScheduler();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}
	
	File dbDir;
	
	private AutoDeleteScheduler() {
		dbDir = new File(DBCtr.getRootPath());
	}
	
	String lastCheckDate;
	Set<String> deletedDays = new HashSet<String>();
	
	public void run() {
		while(CoreRun.running()) {
			if (conf.auto_delete_data) {
				String today = DateUtil.yyyymmdd();
				if (SystemUtil.IS_JAVA_1_5 == false) {
					int maxPercent = conf.auto_delete_max_percent;
					if (maxPercent > 0) {
						long totalSpace = dbDir.getTotalSpace();
						long usuableSpace = dbDir.getUsableSpace();
						double percent = (usuableSpace * 100.0d) / totalSpace;
						while ((100 - percent) > maxPercent) {
							String yyyymmdd = getLongAgoDate(deletedDays);
							if (yyyymmdd == null || today.equals(yyyymmdd)) {
								break;
							}
							deleteData(yyyymmdd);
							usuableSpace += dbDir.getUsableSpace();
							percent = (usuableSpace * 100.0d) / totalSpace;
							deletedDays.add(yyyymmdd);
						}
					}
				}
				int retainDays = conf.auto_delete_retain_days;
				if (retainDays > 0) {
					if (today.equals(lastCheckDate) == false) {
						lastCheckDate = today;
						int lastDeleteDate = CastUtil.cint(DateUtil.yyyymmdd(System.currentTimeMillis() - (DateUtil.MILLIS_PER_DAY * retainDays)));
						while (true) {
							String yyyymmdd = getLongAgoDate(deletedDays);
							if (yyyymmdd == null) {
								break;
							}
							if (CastUtil.cint(yyyymmdd) > lastDeleteDate) {
								break;
							}
							deleteData(yyyymmdd);
							deletedDays.add(yyyymmdd);
						}
					}
				}
			}
			ThreadUtil.sleep(CHECK_INTERVAL);
		}
	}
	
	private void deleteData(String yyyymmdd) {
		try {
			File f = null;
			if (conf.auto_delete_only_xlog) {
				f = new File(dbDir, yyyymmdd + XLogWR.dir());
			} else {
				f = new File(dbDir, yyyymmdd);
			}
			deleteFiles(f);
		} catch (Throwable th) {
			Logger.println("Failed auto deletion... " + yyyymmdd + "  " + th.toString());
		}
	}
	
	void deleteFiles(File file) throws IOException {
		if (file.exists() == false) {
			return;
		}
		if (file.isDirectory()) {
			for (File c : file.listFiles()) {
				deleteFiles(c);
			}
		}
		file.delete();
	}
	
	private String getLongAgoDate(Set<String> exceptDays) {
		File[] dirs = dbDir.listFiles();
		if (dirs == null || dirs.length < 1) {
			return null;
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < dirs.length; i++) {
			File f = dirs[i];
			String name = f.getName();
			if (f.isDirectory() && name.indexOf("0000") == -1 && exceptDays.contains(name) == false) {
				try {
					list.add(Integer.valueOf(f.getName()));
				} catch (Throwable th) { }
			}
		}
		if (list.size() == 0) {
			return null;
		}
		Collections.sort(list);
		return String.valueOf(list.get(0));
	}
}
