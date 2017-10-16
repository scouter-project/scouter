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

package scouter.agent.netio.request.handle;

import java.io.File;
import java.io.FileFilter;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import javax.management.MBeanServer;

import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.SystemUtil;


public class AgentHeapDump {

	private String folderName = "heapdump";
	private String fileExt = ".hprof";
	private long lastCallTime = 0;

	@RequestHandler(RequestCmd.OBJECT_CALL_HEAP_DUMP)
	public Pack callHeapDump(Pack param) {

		long curTime = System.currentTimeMillis();
		if (curTime - lastCallTime < 10000) {
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(false));
			p.put("msg", "please wait 10 sec. from last request...");
			return p;
		}
		lastCallTime = curTime;
		long time = ((MapPack) param).getLong("time");
		String yyyymmdd = DateUtil.yyyymmdd(time);
		String hhmmss = FormatUtil.print(new Date(time), "HHmmss");
		String fName = yyyymmdd + "-" + hhmmss + ((MapPack) param).getText("fName").replaceAll("/", "_") + fileExt;

		File hprofDir = new File(folderName);
		if (hprofDir.exists() == false) {
			hprofDir.mkdirs();
		}

		File hprofFile = new File(hprofDir, fName);
		if (hprofFile.exists()) {
			hprofFile.delete();
		}

		// version check.
		if (SystemUtil.IS_JAVA_1_5) {
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(false));
			p.put("msg", "dumpHeap only works on a Sun Java 1.6+ VM");
			return p;
		}

		String error = dumpHeap(folderName + "/" + fName);
		if (error == null) {
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(true));
			p.put("msg", "Successfully request heap dump...");
			return p;
		} else {
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(false));
			p.put("msg", error);
			return p;
		}

		// HotSpotDiagnosticMXBean hdm =
		// sun.management.ManagementFactory.getDiagnosticMXBean();
		// try {
		// hdm.dumpHeap(folderName+"/"+fName, true);
		//
		// MapPack p = new MapPack();
		// p.put("success", new BooleanValue(true));
		// p.put("msg", "Successfully request heap dump...");
		//
		// return p;
		//
		// } catch (IOException e) {
		// MapPack p = new MapPack();
		// p.put("success", new BooleanValue(false));
		// p.put("msg", e.getMessage());
		//
		// return p;
		// }

	}

	public String dumpHeap(String fileName) {
		Class clazz;
		try {
			clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
		} catch (ClassNotFoundException e) {
			return "ERROR: dumpHeap only works on a Sun Java 1.6+ VM containing "
					+ "the class com.sun.management.HotSpotDiagnosticMXBean";
		}

		Object hotspotMBean = null;
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server,
					"com.sun.management:type=HotSpotDiagnostic", clazz);
		} catch (Throwable e) {
			return "ERROR: dumpHeap was unable to obtain the HotSpotDiagnosticMXBean: " + e.getMessage();
		}

		try {
			Method method = hotspotMBean.getClass().getMethod("dumpHeap", String.class, Boolean.TYPE);
			method.invoke(hotspotMBean, fileName, true);
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause() != null ? e.getCause() : e;
			return "ERROR: dumpHeap threw a InvocationTargetException: " + t;
		} catch (Throwable e) {
			return "ERROR: dumpHeap threw a Throwable: " + e;
		}
		return null;
	}

	@RequestHandler(RequestCmd.OBJECT_DELETE_HEAP_DUMP)
	public Pack deleteHeapDump(Pack param) {
		File delFile = new File(folderName + "/" + ((MapPack) param).getText("delfileName"));
		if (delFile.exists() == false) {
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(false));
			p.put("msg", "file \'" + delFile.getName() + "\' is not exist...");
			return p;
		} else {
			delFile.delete();
			MapPack p = new MapPack();
			p.put("success", new BooleanValue(true));
			p.put("msg", "Successfully deleted...");
			return p;
		}
	}

	@RequestHandler(RequestCmd.OBJECT_LIST_HEAP_DUMP)
	public Pack listHeapDump(Pack param) {

		MapPack p = new MapPack();
		ListValue nameLv = p.newList("name");
		ListValue sizeLv = p.newList("size");

		File hprofDir = new File(folderName);
		if (hprofDir.exists() == false) {
			return null;
		}

		File[] fileList = hprofDir.listFiles(new ContentFilter());
		if (fileList != null && fileList.length > 0) {
			for (int i = 0; i < fileList.length; i++) {
				File f = fileList[i];
				if (/* f.isFile() && */f.getName().endsWith(fileExt)) {
					nameLv.add(f.getName());
					sizeLv.add(f.length());
				}
			}
			return p;
		}
		return p;
	}

	public class ContentFilter implements FileFilter {
		public boolean accept(File file) {
			return file.isFile() || !file.getName().startsWith(".");
		}
	}

	@RequestHandler(RequestCmd.OBJECT_DOWNLOAD_HEAP_DUMP)
	public Pack downloadHeapDump(Pack param, DataInputX in, DataOutputX out) {
		int buff = 2 * 1024 * 1024;
		File downloadFile = new File(folderName + "/" + ((MapPack) param).getText("fileName"));
		try {
			RandomAccessFile raf = new RandomAccessFile(downloadFile, "r");

			byte[] buffer = new byte[buff];
			int read = 0;

			long offset = downloadFile.length();
			int unitsize;
			while (read < offset) {
				unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
				raf.read(buffer, 0, unitsize);
				out.writeByte(TcpFlag.HasNEXT);
				out.writeBlob(buffer, 0, unitsize);
				read += unitsize;
			}
			raf.close();
			return null;

		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

}