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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.plugin.impl.Neighbor;
import scouter.util.BitUtil;
import scouter.util.CastUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.LongSet;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class PlugInLoader extends Thread {
	private static PlugInLoader instance;
	private static final Set<String> registeredJarOnCp = Collections.synchronizedSet(new HashSet<>());
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
				reloadIfModified(root);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	private void reloadIfModified(File root) {
		File scriptFile = new File(root, "alert.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.alerts = null;
		} else {
			if (PlugInManager.alerts == null || PlugInManager.alerts.__lastModified != scriptFile.lastModified()) {
				PlugInManager.alerts = (IAlert) create(scriptFile, "AlertImpl", IAlert.class, AlertPack.class);
			}
		}
		scriptFile = new File(root, "counter.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.counters = null;
		} else {
			if (PlugInManager.counters == null || PlugInManager.counters.__lastModified != scriptFile.lastModified()) {
				PlugInManager.counters = (ICounter) create(scriptFile, "CounterImpl", ICounter.class, PerfCounterPack.class);
			}
		}
		scriptFile = new File(root, "object.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.objects = null;
		} else {
			if (PlugInManager.objects == null || PlugInManager.objects.__lastModified != scriptFile.lastModified()) {
				PlugInManager.objects = (IObject) create(scriptFile, "ObjectImpl", IObject.class, ObjectPack.class);
			}
		}
		scriptFile = new File(root, "xlog.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.xlog = null;
		} else {
			if (PlugInManager.xlog == null || PlugInManager.xlog.__lastModified != scriptFile.lastModified()) {
				PlugInManager.xlog = (IXLog) create(scriptFile, "XLogImpl", IXLog.class, XLogPack.class);
			}
		}
		scriptFile = new File(root, "xlogdb.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.xlogdb = null;
		} else {
			if (PlugInManager.xlogdb == null || PlugInManager.xlogdb.__lastModified != scriptFile.lastModified()) {
				PlugInManager.xlogdb = (IXLog) create(scriptFile, "XLogDBImpl", IXLog.class, XLogPack.class);
			}
		}
		scriptFile = new File(root, "xlogprofile.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.xlogProfiles = null;
		} else {
			if (PlugInManager.xlogProfiles == null || PlugInManager.xlogProfiles.__lastModified != scriptFile.lastModified()) {
				PlugInManager.xlogProfiles = (IXLogProfile) create(scriptFile, "XLogProfileImpl", IXLogProfile.class,
						XLogProfilePack.class);
			}
		}
		scriptFile = new File(root, "summary.plug");
		if (scriptFile.canRead() == false) {
			PlugInManager.summary = null;
		} else {
			if (PlugInManager.summary == null || PlugInManager.summary.__lastModified != scriptFile.lastModified()) {
				PlugInManager.summary = (ISummary) create(scriptFile, "SummaryImpl", ISummary.class,
						SummaryPack.class);
			}
		}
	}

	// Do Not retry compiling the file failed to compile before to prevent compiling broken files permanently.
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
//			String jar = FileUtil.getJarFileName(IAlert.class);
//			if (jar != null) {
//				cp.appendClassPath(jar);
//			}
			//클래스 패스를 자동으로 잡아주도록 수정함, 사용자가 추가한 jar도 자동인식하도록 
			if(this.getClass().getClassLoader() instanceof URLClassLoader){
				URLClassLoader u = (URLClassLoader)this.getClass().getClassLoader();
				URL[] urls = u.getURLs();
				for(int i = 0; urls!=null && i<urls.length ; i++){
					try {
						if (!registeredJarOnCp.contains(urls[i].getFile())) {
							registeredJarOnCp.add(urls[i].getFile());
							cp.appendClassPath(urls[i].getFile());
							Logger.trace("[TR001] javassist CP classpath added: " + urls[i].getFile());
						}
					} catch (NotFoundException e) {
						Logger.println("S229", "[Error]" + e.getMessage());
					}
				}	
			}
			className = "scouter.server.plugin.impl." + className;
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
			
			method.setBody("{" + parameter + " $pack=$1;" + body + "\n}");
			Class<?> c = toClass(impl);
			IPlugIn plugin = (IPlugIn) c.newInstance();
			plugin.__lastModified = file.lastModified();
			Logger.println("PLUG-IN : " + superClass.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (javassist.CannotCompileException ee) {
			compileErrorFiles.add(fileSignature);
			Logger.println("S215", ee.getMessage());
		} catch (Exception e) {
			Logger.println("S216", e);
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

	private Class<?> toClass(CtClass impl) throws CannotCompileException {
		Class<?> c;
		try {
			c = impl.toClass(Neighbor.class); //for java9+ error on java8 (because no module on java8)
		} catch (Throwable t) {
			Logger.println("S1600", "error on toClass with javassist. try to fallback for java8 below. err:" + t.getMessage());
			c= impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null); //for to java8
		}
		return c;
	}
}
