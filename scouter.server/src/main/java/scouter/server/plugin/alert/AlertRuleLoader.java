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
package scouter.server.plugin.alert;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.core.cache.AlertScriptLoadMessageCache;
import scouter.util.BitUtil;
import scouter.util.CastUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.LongSet;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
public class AlertRuleLoader extends Thread {
	public static final String ALERT_FILE_SUFFIX = ".alert";
	public static final String CONF_FILE_SUFFIX = ".conf";
	private static AlertRuleLoader instance;
	public synchronized static AlertRuleLoader getInstance() {
		if (instance == null) {
			instance = new AlertRuleLoader();
			instance.setDaemon(true);
			instance.setName("AlertRuleLoader");
			instance.start();
		}
		return instance;
	}
	public StringKeyLinkedMap<AlertRule> alertRuleTable = new StringKeyLinkedMap<AlertRule>();
	public StringKeyLinkedMap<AlertConf> alertConfTable = new StringKeyLinkedMap<AlertConf>();
	public void run() {
		while (true) {
			ThreadUtil.sleep(5000);
			try {
				File root = new File(Configure.getInstance().plugin_dir);
				if (root != null && root.canRead()) {
					checkModified(root);
					checkNewRule(root);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * get rule file contents as string by counter name
	 * @param counterName
	 * @return
	 */
	public String getRuleContents(String counterName) {
		return getContentsOfFile(counterName, ALERT_FILE_SUFFIX);
	}

	/**
	 * get rule configuration file contents as string by counter name
	 * @param counterName
	 * @return
	 */
	public String getRuleConfigContents(String counterName) {
		return getContentsOfFile(counterName, CONF_FILE_SUFFIX);
	}

	private String getContentsOfFile(String counterName, String fileSuffix) {
		String contents = "";
		try {
			File ruleFile = new File(Configure.getInstance().plugin_dir + "/" + counterName + fileSuffix);
			if (ruleFile.canRead()) {
				contents = new String(FileUtil.readAll(ruleFile));
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return contents;
	}

	/**
	 * save rule file
	 * @param counterName
	 * @return
	 */
	public boolean saveRuleContents(String counterName, String contents) {
		File ruleFile = new File(Configure.getInstance().plugin_dir + "/" + counterName + ALERT_FILE_SUFFIX);
		return FileUtil.saveText(ruleFile, contents);
	}

	/**
	 * save config file
	 * @param counterName
	 * @return
	 */
	public boolean saveConfigContents(String counterName, String contents) {
		File confFile = new File(Configure.getInstance().plugin_dir + "/" + counterName + CONF_FILE_SUFFIX);
		return FileUtil.saveText(confFile, contents);
	}


	private void checkNewRule(File root) {
		File[] ruleFiles = root.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(ALERT_FILE_SUFFIX);
			}
		});
		for (int i = 0; i < ruleFiles.length; i++) {
			String name = getRuleName(ruleFiles[i].getName());
			if (alertRuleTable.containsKey(name))
				continue;
			AlertRule rule = createRule(name, ruleFiles[i]);
			if (rule == null)
				continue;
			AlertConf conf = createConf(name, getConfFile(ruleFiles[i]));
			alertRuleTable.put(name, rule);
			alertConfTable.put(name, conf);
		}
	}
	private void clear(String name) {
		alertRuleTable.remove(name);
		alertConfTable.remove(name);
		Logger.println("S217", "Clear alert rule : " + name);
		AlertScriptLoadMessageCache.put("Alert rule cleared : " + name);
	}
	private void checkModified(File root) {
		StringEnumer en = alertRuleTable.keys();
		while (en.hasMoreElements()) {
			String name = en.nextString();
			AlertRule rule = alertRuleTable.get(name);
			/*
			 * if the Rule file is not existed, then also clear the conf-info
			 */
			File ruleFile = new File(root, name + ALERT_FILE_SUFFIX);
			if (ruleFile.canRead() == false || rule == null) {
				clear(name);
				continue;
			}
			if (ruleFile.lastModified() != rule.__lastModified) {
				rule = createRule(name, ruleFile);
				alertRuleTable.put(name, rule);
			}
			File ruleConf = new File(root, name + CONF_FILE_SUFFIX);
			AlertConf conf = alertConfTable.get(name);
			if (conf.lastModified != ruleConf.lastModified()) {
				conf = createConf(name, ruleConf);
				alertConfTable.put(name, conf);
			}
		}
	}
	private String getRuleName(String name) {
		name = name.substring(0, name.lastIndexOf('.'));
		return name;
	}
	private File getConfFile(File ruleFile) {
		if (ruleFile == null)
			return null;
		File conf = new File(ruleFile.getPath().substring(0, ruleFile.getPath().lastIndexOf('.')) + CONF_FILE_SUFFIX);
		if (conf.canRead())
			return conf;
		else
			return null;
	}
	// 각 룰에 대한 기본 설정을 로딩한다.
	// 각 설정은 스크립트에서 변경할 수 있다.
	private AlertConf createConf(String name, File confFile) {
		AlertConf conf = new AlertConf();
		if (confFile != null && confFile.canRead()) {
			conf.lastModified = confFile.lastModified();
			byte[] body = FileUtil.readAll(confFile);
			if (body != null) {
				Properties p = new Properties();
				try {
					p.load(new ByteArrayInputStream(body));
				} catch (Exception e) {
				}
				conf.history_size = getInt(p, "history_size", 0);
				conf.silent_time = getInt(p, "silent_time", 0);
				conf.check_term = getInt(p, "check_term", 0);
			}
		}
		return conf;
	}
	// 반복적인 컴파일 시도를 막기위해 한번 실패한 파일은 컴파일을 다시 시도하지 않도록 한다.
	private LongSet compileErrorFiles = new LongSet();
	private AlertRule createRule(String name, File ruleFile) {
		long fileSignature = fileSign(ruleFile);
		if (compileErrorFiles.contains(fileSignature))
			return null;
		try {
			String body = new String(FileUtil.readAll(ruleFile));
			ClassPool cp = ClassPool.getDefault();
//			String jar = FileUtil.getJarFileName(AlertRule.class);
//			if (jar != null) {
//				cp.appendClassPath(jar);
//			}
			if(this.getClass().getClassLoader() instanceof URLClassLoader){
				URLClassLoader u = (URLClassLoader)this.getClass().getClassLoader();
				URL[] urls = u.getURLs();
				for(int i = 0; urls!=null && i<urls.length ; i++){
					//Logger.println("[Alert rule load classpath urls]" + urls[i].toString());
					try {
						cp.appendClassPath(urls[i].getFile());
					} catch (NotFoundException e) {
						Logger.println("S219", "[Error]" + e.getMessage());
					}
				}	
			}
			name = "scouter.server.alert.impl." + name;
			Class c = null;
			CtClass cc = cp.get(AlertRule.class.getName());
			CtClass impl = null;
			CtMethod method = null;
			try {
				impl = cp.get(name);
				impl.defrost();
				method = impl.getMethod("process", "(" + nativeName(RealCounter.class) + ")V");
			} catch (javassist.NotFoundException e) {
				impl = cp.makeClass(name, cc);
				method = CtNewMethod.make("public void process(" + RealCounter.class.getName() + " c){}", impl);
				impl.addMethod(method);
			}
			method.setBody("{" + RealCounter.class.getName() + " $counter=$1;" + body + "\n}");
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AlertRule rule = (AlertRule) c.newInstance();
			rule.__lastModified = ruleFile.lastModified();
			Logger.println("S215", "Alert rule detected : " + ruleFile.getName());
			AlertScriptLoadMessageCache.put("Alert rule detected : " + ruleFile.getName());
			return rule;
		} catch (javassist.CannotCompileException ee) {
			compileErrorFiles.add(fileSignature);
			Logger.println("S212", ee.getMessage());
			AlertScriptLoadMessageCache.put("Alert rule compile error : " + ee.getMessage());
		} catch (Exception e) {
			Logger.println("S213", e);
			AlertScriptLoadMessageCache.put("error : " + e.getMessage());
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
