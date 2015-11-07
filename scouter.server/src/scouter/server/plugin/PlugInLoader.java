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
		File script = new File(root, "alert.plugin");
		if (script.canRead() == false) {
			PlugInManager.alerts = null;
		} else {
			if (PlugInManager.alerts == null || PlugInManager.alerts.lastModified != script.lastModified()) {
				PlugInManager.alerts = (IAlert) create(script, "Alert", IAlert.class.getName(), "process", "("
						+ nativeName(AlertPack.class) + ")V", AlertPack.class.getName(), "void");
			}
		}

		script = new File(root, "counter.plugin");
		if (script.canRead() == false) {
			PlugInManager.counters = null;
		} else {
			if (PlugInManager.counters == null || PlugInManager.counters.lastModified != script.lastModified()) {
				PlugInManager.counters = (ICounter) create(script, "Counter",//
						ICounter.class.getName(), "process", //
						"(" + nativeName(PerfCounterPack.class) + ")V"//
						, PerfCounterPack.class.getName(), "void");
			}
		}

		script = new File(root, "object.plugin");
		if (script.canRead() == false) {
			PlugInManager.objects = null;
		} else {
			if (PlugInManager.objects == null || PlugInManager.objects.lastModified != script.lastModified()) {
				PlugInManager.objects = (IObject) create(script, "Object1",//
						IObject.class.getName(), "process", //
						"(" + nativeName(ObjectPack.class) + ")V"//
						, ObjectPack.class.getName(), "void");
			}
		}

		script = new File(root, "xlog.plugin");
		if (script.canRead() == false) {
			PlugInManager.xlog = null;
		} else {
			if (PlugInManager.xlog == null || PlugInManager.xlog.lastModified != script.lastModified()) {
				PlugInManager.xlog = (IXLog) create(script, "XLog",//
						IXLog.class.getName(), "process", //
						"(" + nativeName(XLogPack.class) + ")V"//
						, XLogPack.class.getName(), "void");
			}
		}
		script = new File(root, "xlogdb.plugin");
		if (script.canRead() == false) {
			PlugInManager.xlogdb = null;
		} else {
			if (PlugInManager.xlogdb == null || PlugInManager.xlogdb.lastModified != script.lastModified()) {
				PlugInManager.xlogdb = (IXLog) create(script, "XLogDB",//
						IXLog.class.getName(), "process", //
						"(" + nativeName(XLogPack.class) + ")V"//
						, XLogPack.class.getName(), "void");
			}
		}
		script = new File(root, "xlogprofile.plugin");
		if (script.canRead() == false) {
			PlugInManager.xlogProfiles = null;
		} else {
			if (PlugInManager.xlogProfiles == null || PlugInManager.xlogProfiles.lastModified != script.lastModified()) {
				PlugInManager.xlogProfiles = (IXLogProfile) create(script, "XLog",//
						IXLogProfile.class.getName(), "process", //
						"(" + nativeName(XLogProfilePack.class) + ")V"//
						, XLogProfilePack.class.getName(), "void");
			}
		}
	}

	// 반복적인 컴파일 시도를 막기위해 한번 실패한 파일은 컴파일을 다시 시도하지 않도록 한다.
	private LongSet compileErrorFiles = new LongSet();

	private IPlugIn create(File file, String className, String superName, String mname, String sig, String paramName,
			String returnValue) {
		long fileSignature = signature(file);
		if (compileErrorFiles.contains(fileSignature))
			return null;
		try {

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
				method = impl.getMethod(mname, sig);
			} catch (javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				method = CtNewMethod.make("public " + returnValue + " " + mname + "(" + paramName + " p){"
						+ returnString(returnValue) + "}", impl);
				impl.addMethod(method);
			}
			method.setBody("{" + paramName + " $p=$1;" + body + "}");
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);

			IPlugIn rule = (IPlugIn) c.newInstance();
			rule.lastModified = file.lastModified();
			return rule;
		} catch (javassist.CannotCompileException ee) {
			compileErrorFiles.add(fileSignature);
			Logger.println("IPlugIn", ee.getMessage());
		} catch (Exception e) {
			Logger.println("IPlugIn", e);
		}
		return null;
	}

	private String returnString(String v) {
		if (v.equals("void"))
			return "";
		if (v.equals("int") || v.equals("long") || v.equals("float") || v.equals("double"))
			return " return 0;";
		else
			return "return null;";
	}

	private long signature(File f) {
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