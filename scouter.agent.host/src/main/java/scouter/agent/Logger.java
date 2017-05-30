/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *  
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import scouter.lang.conf.ConfObserver;
import scouter.util.CompareUtil;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.StringLongLinkedMap;
import scouter.util.ThreadUtil;

public class Logger {

	private static StringLongLinkedMap lastLog = new StringLongLinkedMap().setMax(1000);

	public static void info(String msg) {
		println("INFO", msg);
	}

	public static void println(Object message) {
		println(DateUtil.datetime(System.currentTimeMillis()) + " " + message);
	}

	public static void println(String id, Object message) {
		if (checkOk(id, 0) == false) {
			return;
		}
		println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
	}

	public static void println(String id, int sec, Object message) {
		if (checkOk(id, sec) == false) {
			return;
		}
		println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
	}

	public static void println(String id, int sec, String message, Throwable t) {
		if (checkOk(id, sec) == false) {
			return;
		}
		println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
		println(ThreadUtil.getStackTrace(t));
	}

	public static String getCallStack(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			t.printStackTrace(pw);
			return sw.toString();
		} finally {
			pw.close();
		}
	}

	private static boolean checkOk(String id, int sec) {
		if (Configure.getInstance().mgr_log_ignore_ids.hasKey(id))
			return false;
		if (sec > 0) {
			long last = lastLog.get(id);
			long now = System.currentTimeMillis();
			if (now < last + sec * 1000)
				return false;
			lastLog.put(id, now);
		}
		return true;
	}

	public static PrintWriter pw = null;
	private static long lastDataUnit = 0;
	private static String lastDir = ".";
	private static boolean lastFileRotation = true;

	private static void println(String msg) {
		try {
			if (pw != null) {
				pw.println(msg);
				pw.flush();
				return;
			}
			openFile();
			if (pw == null) {
				System.out.println(msg);
			} else {
				pw.println(msg);
				pw.flush();
			}
		} catch (Exception e) {
			pw = (PrintWriter) FileUtil.close(pw);
			System.out.println(msg);
		}
	}

	private static Runnable proc = new Runnable() {

		public void run() {
			long last = System.currentTimeMillis();
			while (true) {
				long now = System.currentTimeMillis();
				if (now > last + DateUtil.MILLIS_PER_HOUR) {
					last = now;
					clearOldLog();
				}

				if (lastDataUnit != DateUtil.getDateUnit()) {
					pw = (PrintWriter) FileUtil.close(pw);
					lastDataUnit = DateUtil.getDateUnit();
				}
				ThreadUtil.sleep(5000);
			}
		}
	};
	static Configure conf = Configure.getInstance();
	static {
		ConfObserver.add(Logger.class.getName(), new Runnable() {
			public void run() {
				if (CompareUtil.equals(lastDir, conf.log_dir) == false || lastFileRotation != conf.log_rotation_enalbed) {
					pw = (PrintWriter) FileUtil.close(pw);
					lastDir = conf.log_dir;
					lastFileRotation = conf.log_rotation_enalbed;
				}
			}
		});
		Thread t = new Thread(proc, "scouter.agent.Logger");
		t.setDaemon(true);
		t.start();
	}

	private static synchronized void openFile() throws IOException {
		if (pw == null) {
			lastDataUnit = DateUtil.getDateUnit();
			lastDir = conf.log_dir;
			lastFileRotation = conf.log_rotation_enalbed;

			new File(lastDir).mkdirs();
			if (conf.log_rotation_enalbed) {
				FileWriter fw = new FileWriter(new File(conf.log_dir, "agent-" + DateUtil.yyyymmdd() + ".log"), true);
				pw = new PrintWriter(fw);
			} else {
				pw = new PrintWriter(new File(conf.log_dir, "agent.log"));
			}
			lastDataUnit = DateUtil.getDateUnit();
		}
	}

	protected static void clearOldLog() {
		if (conf.log_rotation_enalbed == false)
			return;
		if (conf.log_keep_days <= 0)
			return;
		long nowUnit = DateUtil.getDateUnit();
		File dir = new File(conf.log_dir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				continue;
			String name = files[i].getName();
			if (name.startsWith("agent-") == false)
				continue;
			int x = name.lastIndexOf('.');
			if (x < 0)
				continue;
			String date = name.substring("agent-".length(), x);
			if (date.length() != 8)
				continue;

			try {
				long d = DateUtil.yyyymmdd(date);
				long fileUnit = DateUtil.getDateUnit(d);
				if (nowUnit - fileUnit > DateUtil.MILLIS_PER_DAY * conf.log_keep_days) {
					files[i].delete();
				}
			} catch (Exception e) {
			}
		}

	}

	public static void main(String[] args) {
		String name = "agent-19701123.log";
		int x = name.lastIndexOf('.');
		String date = name.substring("agent-".length(), x);
		System.out.println(date);

	}



}