package scouter.server.test;

import scouter.server.Logger;
import scouter.server.db.XLogWR;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Copyright (C) 2017 SNOW Corporation. All rights Reserved.
 * SNOW Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Created by gunlee on 2017. 5. 30.
 */
public class AutoDeleteTest {
	File dbDir = new File("/Users/gunlee/Documents/workspace/scouter/run-dir/scouter-server/database");

	enum Mode {
		PROFILE, XLOG, ALL,;
	}

	private void deleteData(String yyyymmdd, Mode mode) {
		try {
			File f = null;
			if (mode == Mode.ALL) {
				f = new File(dbDir, yyyymmdd);
				deleteFiles(f);
			} else if(mode == Mode.XLOG) {
				f = new File(dbDir, yyyymmdd + XLogWR.dir());
				deleteFiles(f);
			} else if (mode == Mode.PROFILE) {
				f = new File(dbDir, yyyymmdd + XLogWR.dir() + "/xlog.profile");
				deleteFiles(f);
				f = new File(dbDir, yyyymmdd + XLogWR.dir() + "/xlog_profile.hfile");
				deleteFiles(f);
				f = new File(dbDir, yyyymmdd + XLogWR.dir() + "/xlog_profile.kfile");
				deleteFiles(f);
			} else {
				throw new IllegalArgumentException("Not expected Mode : " + mode);
			}

			Logger.println("S206", "Auto deletion... " + yyyymmdd);
		} catch (Throwable th) {
			Logger.println("S207", "Failed auto deletion... " + yyyymmdd + "  " + th.toString());
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

	public static void main(String[] args) {
		AutoDeleteTest test = new AutoDeleteTest();
		Set except = new HashSet<String>();
		except.add("20161119");
		except.add("20170501");
		String longAgo = test.getLongAgoDate(except);
		System.out.println(longAgo);
	}

	Set<String> deletedProfileDays = new HashSet<String>();
	Set<String> deletedXLogDays = new HashSet<String>();
	Set<String> deletedDays = new HashSet<String>();

	int maxPercent;
	int retainDays;
	int retainXlogDays;
	int retainCounterDays;
	boolean delOnlyXLog;
	String lastCheckDate;
	
	public void purge() {
		String today = DateUtil.yyyymmdd();
		if (SystemUtil.IS_JAVA_1_5 == false) {
			int maxPercent = 85;
			if (maxPercent > 0) {
				long totalSpace = dbDir.getTotalSpace();
				long usuableSpace = dbDir.getUsableSpace();
				double freePercent = (usuableSpace * 100.0d) / totalSpace;
				while ((100 - freePercent) > maxPercent) {
					String yyyymmdd = getLongAgoDate(deletedProfileDays);
					if (yyyymmdd == null || today.equals(yyyymmdd)) {
						break;
					}
					deleteData(yyyymmdd, Mode.PROFILE);
					usuableSpace = dbDir.getUsableSpace();
					freePercent = (usuableSpace * 100.0d) / totalSpace;
					deletedProfileDays.add(yyyymmdd);
				}
				while ((100 - freePercent) > maxPercent) {
					String yyyymmdd = getLongAgoDate(deletedXLogDays);
					if (yyyymmdd == null || today.equals(yyyymmdd)) {
						break;
					}
					deleteData(yyyymmdd, Mode.XLOG);
					usuableSpace = dbDir.getUsableSpace();
					freePercent = (usuableSpace * 100.0d) / totalSpace;
					deletedXLogDays.add(yyyymmdd);
				}
				while ((100 - freePercent) > maxPercent) {
					String yyyymmdd = getLongAgoDate(deletedDays);
					if (yyyymmdd == null || today.equals(yyyymmdd)) {
						break;
					}
					deleteData(yyyymmdd, Mode.ALL);
					usuableSpace = dbDir.getUsableSpace();
					freePercent = (usuableSpace * 100.0d) / totalSpace;
					deletedDays.add(yyyymmdd);
				}
			}
		}

		int retainProfileDays = 10;
		if (retainProfileDays > 0) {
			if (today.equals(lastCheckDate) == false) {
				lastCheckDate = today;
				int lastDeleteDate = CastUtil.cint(DateUtil.yyyymmdd(System.currentTimeMillis() - (DateUtil.MILLIS_PER_DAY * retainProfileDays)));
				while (true) {
					String yyyymmdd = getLongAgoDate(deletedProfileDays);
					if (yyyymmdd == null) {
						break;
					}
					if (CastUtil.cint(yyyymmdd) > lastDeleteDate) {
						break;
					}
					deleteData(yyyymmdd, Mode.PROFILE);
					deletedProfileDays.add(yyyymmdd);
				}
			}
		}

		int retainXLogDays = 20;
		if (retainXLogDays > 0) {
			if (today.equals(lastCheckDate) == false) {
				lastCheckDate = today;
				int lastDeleteDate = CastUtil.cint(DateUtil.yyyymmdd(System.currentTimeMillis() - (DateUtil.MILLIS_PER_DAY * retainXLogDays)));
				while (true) {
					String yyyymmdd = getLongAgoDate(deletedXLogDays);
					if (yyyymmdd == null) {
						break;
					}
					if (CastUtil.cint(yyyymmdd) > lastDeleteDate) {
						break;
					}
					deleteData(yyyymmdd, Mode.XLOG);
					deletedXLogDays.add(yyyymmdd);
				}
			}
		}

		int retainCounterDays = 30;
		if (retainCounterDays > 0) {
			if (today.equals(lastCheckDate) == false) {
				lastCheckDate = today;
				int lastDeleteDate = CastUtil.cint(DateUtil.yyyymmdd(System.currentTimeMillis() - (DateUtil.MILLIS_PER_DAY * retainCounterDays)));
				while (true) {
					String yyyymmdd = getLongAgoDate(deletedDays);
					if (yyyymmdd == null) {
						break;
					}
					if (CastUtil.cint(yyyymmdd) > lastDeleteDate) {
						break;
					}
					deleteData(yyyymmdd, Mode.ALL);
					deletedDays.add(yyyymmdd);
				}
			}
		}
	}
}
