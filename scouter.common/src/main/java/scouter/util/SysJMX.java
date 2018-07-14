/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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

package scouter.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.List;

public class SysJMX {
	private static ThreadMXBean threadmx;
	private static OperatingSystemMXBean osmx;
	private static RuntimeMXBean rtmx;
	private static Method getSystemLoadAverage;
	private static Method getProcessCpuTime;
	private static boolean sunThreadMX = false;
	static {
		try {
			threadmx = ManagementFactory.getThreadMXBean();
			osmx = ManagementFactory.getOperatingSystemMXBean();
			rtmx = ManagementFactory.getRuntimeMXBean();

			sunThreadMX = threadmx.getClass().getName().indexOf("sun") >= 0;
		} catch (Throwable t) {
			threadmx = null;
			osmx = null;
			rtmx = null;
			sunThreadMX=false;
		}
		try {
			if (osmx != null) {
				Class c = osmx.getClass();
				getSystemLoadAverage = c.getMethod("getSystemLoadAverage");
				getProcessCpuTime = c.getMethod("getProcessCpuTime");
			}
		} catch (Throwable t) {
		}
	}

	public static long getCurrentThreadCPU() {
		if (threadmx == null)
			return 0;
		try {
			return threadmx.getCurrentThreadCpuTime() / 1000000L;
		} catch (Throwable t) {
			return 0;
		}
	}
	public static long getCurrentThreadCPUnano() {
		if (threadmx == null)
			return 0;
		try {
			return threadmx.getCurrentThreadCpuTime() ;
		} catch (Throwable t) {
			return 0;
		}
	}
	public static long getCurrentThreadAllocBytes(boolean ok) {
		if (!ok) {
			return 0;
		}
		if (sunThreadMX ==false)
			return 0;
		try {
			return ((com.sun.management.ThreadMXBean)threadmx).getThreadAllocatedBytes(Thread.currentThread().getId());
		} catch (Throwable t) {
			sunThreadMX=false;
			return 0;
		}
	}

	public static long getThreadCPU(long id) {
		if (threadmx == null)
			return 0;
		try {
			return threadmx.getThreadCpuTime(id) / 1000000L;
		} catch (Throwable t) {
			return 0;
		}
	}

	public static long getCurrentProcGcTime() {
		if (threadmx == null)
			return 0;
		try {
			List gclist = ManagementFactory.getGarbageCollectorMXBeans();
			long gctime = 0;
			for (int i = 0; i < gclist.size(); i++) {
				GarbageCollectorMXBean mxs = (GarbageCollectorMXBean) gclist.get(i);
				gctime += mxs.getCollectionTime();
			}
			return gctime;
		} catch (Throwable t) {
			return 0;
		}
	}

	public static long[] getCurrentProcGcInfo() {
		long[] gc = new long[2];
		if (threadmx == null)
			return gc;
		try {
			List gclist = ManagementFactory.getGarbageCollectorMXBeans();
			for (int i = 0; i < gclist.size(); i++) {
				GarbageCollectorMXBean mxs = (GarbageCollectorMXBean) gclist.get(i);
				gc[0] += mxs.getCollectionCount();
				gc[1] += mxs.getCollectionTime();
			}
			return gc;
		} catch (Throwable t) {
			return gc;
		}
	}

	public static float getSystemLoad() {
		if (getSystemLoadAverage == null)
			return 0;
		try {
			Object o = getSystemLoadAverage.invoke(osmx, new Object[0]);
			return ((Float) o).floatValue();
		} catch (Throwable t) {
			return 0;
		}
	}

	public static boolean isProcessCPU() {
		return getProcessCpuTime != null;
	}

	public static long getProcessCPU() {
		if (getProcessCpuTime == null)
			return 0;
		try {
			Object o = getProcessCpuTime.invoke(osmx, new Object[0]);
			return ((Long) o).longValue();
		} catch (Throwable t) {
			return 0;
		}
	}

	public static int getProcessPID() {
		RuntimeMXBean o = ManagementFactory.getRuntimeMXBean();
		String nm = o.getName();
		int x = nm.indexOf("@");
		try {
			if (x > 0)
				return Integer.parseInt(nm.substring(0, x));
		} catch (Exception e) {
		}
		return -1;
	}

	private static String hostname = null;

	public static String getHostName() {
		if (hostname != null) {
			return hostname;
		}
		if (SystemUtil.IS_LINUX) {
			InputStream is = null;
			OutputStream os = null;
			InputStream es = null;
			try {
				Process process = Runtime.getRuntime().exec("hostname");
				is = process.getInputStream();
				os = process.getOutputStream();
				es = process.getErrorStream();
				byte[] isBytes = FileUtil.readAll(is);
				hostname = new String(isBytes);
				if (StringUtil.isNotEmpty(hostname)) {
					hostname = hostname.replaceAll("\n", "");
					hostname = hostname.replaceAll("\r", "");
				}

				return hostname;
			} catch (Throwable th) {
				//th.printStackTrace();
			} finally {
				FileUtil.close(is);
				FileUtil.close(os);
				FileUtil.close(es);
			}
		}
		RuntimeMXBean o = ManagementFactory.getRuntimeMXBean();
		String nm = o.getName();
		int x = nm.indexOf("@");
		hostname = nm.substring(x + 1);
		return hostname;
	}

	public static String getProcessName() {
		return ManagementFactory.getRuntimeMXBean().getName();
	}

	public static long getThreadCpuTime(Thread thread) {
		if (threadmx == null)
			return 0;
		return threadmx.getThreadCpuTime(thread.getId()) / 1000000L;
	}

	public static String getUserName() {

		String username = SystemUtil.USER_NAME;
		if (StringUtil.isNotEmpty(username)) {
			return username;
		}
		username = System.getenv("USERNAME");

		if (StringUtil.isNotEmpty(username)) {
			return username;
		}
		InputStream is = null;
		OutputStream os = null;
		InputStream es = null;
		try {
			Process process = Runtime.getRuntime().exec("whoami");
			is = process.getInputStream();
			os = process.getOutputStream();
			es = process.getErrorStream();
			byte[] isBytes = FileUtil.readAll(is);
			username = new String(isBytes);
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			FileUtil.close(is);
			FileUtil.close(os);
			FileUtil.close(es);
		}

		return username;
	}

	public static String getUsingJava() {
		return SystemUtil.JAVA_VERSION;
	}

}