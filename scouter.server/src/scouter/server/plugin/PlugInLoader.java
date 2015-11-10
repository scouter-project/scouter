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
 *
 */
package scouter.server.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.BitUtil;
import scouter.util.CastUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.LongSet;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

public class PlugInLoader extends Thread {

	private static PlugInLoader instance;

	public synchronized static PlugInLoader getInstance() {
		if (instance == null) {
			instance = new PlugInLoader();
			instance.setDaemon(true);
			instance.setName("PluginLoader");
			instance.start();
		}
		return instance;
	}

	public void run() {
		while (true) {
			ThreadUtil.sleep(5000);

			try {
				File root = new File(Configure.getInstance().plugin_dir);
				checkModified(root);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	private void checkModified(File root) {
		File script = new File(root, "alert.plug");
		if (script.canRead() == false) {
			PlugInManager.alerts = null;
		} else {
			if (PlugInManager.alerts == null || PlugInManager.alerts.lastModified != script.lastModified()) {
				PlugInManager.alerts = (IAlert) create(script, "AlertImpl", IAlert.class, AlertPack.class);
			}
		}

		script = new File(root, "counter.plug");
		if (script.canRead() == false) {
			PlugInManager.counters = null;
		} else {
			if (PlugInManager.counters == null || PlugInManager.counters.lastModified != script.lastModified()) {
				PlugInManager.counters = (ICounter) create(script, "CounterImpl", ICounter.class, PerfCounterPack.class);
			}
		}

		script = new File(root, "object.plug");
		if (script.canRead() == false) {
			PlugInManager.objects = null;
		} else {
			if (PlugInManager.objects == null || PlugInManager.objects.lastModified != script.lastModified()) {
				PlugInManager.objects = (IObject) create(script, "ObjectImpl", IObject.class, ObjectPack.class);
			}
		}

		script = new File(root, "xlog.plug");
		if (script.canRead() == false) {
			PlugInManager.xlog = null;
		} else {
			if (PlugInManager.xlog == null || PlugInManager.xlog.lastModified != script.lastModified()) {
				PlugInManager.xlog = (IXLog) create(script, "XLogImpl", IXLog.class, XLogPack.class);
			}
		}
		script = new File(root, "xlogdb.plug");
		if (script.canRead() == false) {
			PlugInManager.xlogdb = null;
		} else {
			if (PlugInManager.xlogdb == null || PlugInManager.xlogdb.lastModified != script.lastModified()) {
				PlugInManager.xlogdb = (IXLog) create(script, "XLogDBImpl", IXLog.class, XLogPack.class);
			}
		}
		script = new File(root, "xlogprofile.plug");
		if (script.canRead() == false) {
			PlugInManager.xlogProfiles = null;
		} else {
			if (PlugInManager.xlogProfiles == null || PlugInManager.xlogProfiles.lastModified != script.lastModified()) {
				PlugInManager.xlogProfiles = (IXLogProfile) create(script, "XLogProfileImpl", IXLogProfile.class,
						XLogProfilePack.class);
			}
		}
		script = new File(root, "summary.plug");
		if (script.canRead() == false) {
			PlugInManager.summary = null;
		} else {
			if (PlugInManager.summary == null || PlugInManager.summary.lastModified != script.lastModified()) {
				PlugInManager.summary = (ISummary) create(script, "SummaryImpl", ISummary.class,
						SummaryPack.class);
			}
		}
	}

	// 반복적인 컴파일 시도를 막기위해 한번 실패한 파일은 컴파일을 다시 시도하지 않도록 한다.
	private LongSet compileErrorFiles = new LongSet();

	private IPlugIn create(File file, String className, Class superClass, Class paramClass) {
		long fileSignature = fileSign(file);
		if (compileErrorFiles.contains(fileSignature))
			return null;
		try {

			String methodName = "process";
			String superName = superClass.getName();
			String signature = "(" + nativeName(paramClass) + ")V";
			String parameter = paramClass.getName();
			String body = new String(FileUtil.readAll(file));
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(IAlert.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}

			className = "scouter.server.plugin.impl." + className;
			Class c = null;
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			CtMethod method = null;
			try {
				impl = cp.get(className);
				impl.defrost();
				method = impl.getMethod(methodName, signature);
			} catch (javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				method = CtNewMethod.make("public void " + methodName + "(" + parameter + " p){}", impl);
				impl.addMethod(method);
			}
			
			method.setBody("{" + parameter + " $pack=$1;" + body + "}");
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);

			IPlugIn plugin = (IPlugIn) c.newInstance();
			plugin.lastModified = file.lastModified();
			return plugin;
		} catch (javassist.CannotCompileException ee) {
			compileErrorFiles.add(fileSignature);
			Logger.println("IPlugIn", ee.getMessage());
		} catch (Exception e) {
			Logger.println("IPlugIn", e);
		}
		return null;
	}

	private long fileSign(File f) {
		if (f == null)
			return 0;
		String filename = f.getName();
		long filetime = f.lastModified();
		return BitUtil.setHigh(filetime, HashUtil.hash(filename));
	}

	private String nativeName(Class class1) {
		return "L" + class1.getName().replace('.', '/') + ";";
	}

	protected int getInt(Properties p, String key, int defValue) {
		String value = StringUtil.trimEmpty(p.getProperty(key));
		if (value.length() == 0)
			return defValue;
		return CastUtil.cint(value);
	}
}