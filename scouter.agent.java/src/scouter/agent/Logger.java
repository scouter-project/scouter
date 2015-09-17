/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.agent;

import java.io.FileWriter;
import java.io.PrintWriter;

import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.IClose;
import scouter.util.StringLongLinkedMap;

public class Logger {
	private static StringLongLinkedMap lastLog = new StringLongLinkedMap().setMax(500);

	public static void println(String id, String message) {
		if (checkOk(id) == false)
			return;

		System.out.println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
	}

	private static boolean checkOk(String id) {
		if (Configure.getInstance().log_ignore_set.hasKey(id))
			return false;
		long last = lastLog.get(id);
		long now = System.currentTimeMillis();
		if (now < last + 10 * 1000)
			return false;
		lastLog.put(id, now);
		return true;
	}

	public static void println(String id, String message, Throwable t) {
		if (checkOk(id) == false)
			return;
		System.out.println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + message);
		t.printStackTrace();
	}

	public static void println(String id, Throwable t) {
		if (checkOk(id) == false)
			return;
		System.out.println(DateUtil.datetime(System.currentTimeMillis()) + " [" + id + "] " + t);
		t.printStackTrace();
	}

	public static void info(String message) {
		System.out.println(DateUtil.datetime(System.currentTimeMillis()) + " [SCOUTER] " + message);
	}

	public static class FileLog implements IClose {
		private PrintWriter out;

		public FileLog(String filename) {
			try {
				this.out = new PrintWriter(new FileWriter(filename));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void println(String message) {
			if (this.out == null)
				return;
			this.out.println(DateUtil.datetime(System.currentTimeMillis()) + " " + message);
			this.out.flush();
		}

		public void close() {
			FileUtil.close(this.out);
		}
	}
}