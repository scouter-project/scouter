package scouter.server.alert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import scouter.server.Configure;
import scouter.util.CastUtil;
import scouter.util.FileUtil;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringLongLinkedMap;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

public class RuleLoader extends Thread {

	private static RuleLoader instance;

	public synchronized static RuleLoader getInstance() {
		if (instance == null) {
			instance = new RuleLoader();
			instance.setDaemon(true);
			instance.setName("RuleLoader");
			instance.start();
		}
		return instance;
	}

	public StringKeyLinkedMap<AlertRule> alertRuleTable = new StringKeyLinkedMap<AlertRule>();
	public StringKeyLinkedMap<AlertConf> alertConfTable = new StringKeyLinkedMap<AlertConf>();

	public void run() {
		while (true) {
			try {
				checkModified();
				File root = new File(Configure.getInstance().alert_rule_dir);
				if (root != null && root.canRead()) {
					process(root);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			ThreadUtil.sleep(5000);
		}
	}

	private void clear(String name) {
		alertRuleTable.remove(name);
		alertConfTable.remove(name);
	}

	private void checkModified() {
		StringEnumer en = alertRuleTable.keys();
		while (en.hasMoreElements()) {
			String name = en.nextString();
			AlertRule rule = alertRuleTable.get(name);
			File ruleFile = getFileName(name);
			if (ruleFile.canRead() == false || rule == null) {
				clear(name);
				continue;
			}

			if (ruleFile.lastModified() != rule.lastModified) {
				rule = createRule(name, ruleFile);
				alertRuleTable.put(name, rule);
			}
			File ruleConf = getFileName(name);
			AlertConf conf = alertConfTable.get(name);
			if (conf.lastModified != ruleConf.lastModified()) {
				conf = createConf(name, ruleConf);
				alertConfTable.put(name, conf);
			}
		}
	}

	private File getFileName(String name) {
		return new File(Configure.getInstance().alert_rule_dir, name + ".rule");
	}

	private void process(File root) {
		File[] ruleFiles = root.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".rule");
			}
		});

		for (int i = 0; i < ruleFiles.length; i++) {
			String name = getRuleName(ruleFiles[i].getName());
			if (alertRuleTable.containsKey(name))
				continue;

			AlertRule rule = createRule(name, ruleFiles[i]);
			if (rule == null)
				continue;
			AlertConf conf = createConf(name, getConfFile(ruleFiles[i].getName()));
			alertRuleTable.put(name, rule);
			alertConfTable.put(name, conf);
		}
	}

	private String getRuleName(String name) {
		name = name.substring(0, name.lastIndexOf('.'));
		return name;
	}

	private File getConfFile(String f) {
		if (f == null)
			return null;
		File conf = new File(f.substring(0, f.lastIndexOf('.')) + ".conf");
		if (conf.canRead())
			return conf;
		else
			return null;
	}

	private AlertConf createConf(String name, File confFile) {
		AlertConf conf = new AlertConf();
		if (confFile != null) {
			conf.lastModified = confFile.lastModified();
			byte[] body = FileUtil.readAll(confFile);
			Properties p = new Properties();
			try {
				p.load(new ByteArrayInputStream(body));
			} catch (IOException e) {
			}
			conf.keep_history = getInt(p, "keep_history", 0);
		}
		return conf;
	}

	private AlertRule createRule(String name, File ruleFile) {
		try {
			String body = new String(FileUtil.readAll(ruleFile));
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(AlertRule.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			Class c = null;
			CtClass cc = cp.get("scouter.server.alert.AlertRule");
			CtClass impl = null;
			try {
				impl = cp.get(name);
				impl.defrost();
				CtMethod method = impl.getMethod("process", "(Lscouter/server/alert/Counter;)V");
				method.setBody("{scouter.server.alert.Counter c=$1;" + body + "}");
				c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			} catch (javassist.NotFoundException e) {
				impl = cp.makeClass(name, cc);
				CtMethod method = CtNewMethod.make("public void process(scouter.server.alert.Counter c){}", impl);
				impl.addMethod(method);
				method.setBody("{scouter.server.alert.Counter c=$1;" + body + "}");
				c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			}

			AlertRule rule = (AlertRule) c.newInstance();
			rule.lastModified = ruleFile.lastModified();
			return rule;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected int getInt(Properties p, String key, int defValue) {
		String value = StringUtil.trimEmpty(p.getProperty(key));
		if (value.length() == 0)
			return defValue;
		return CastUtil.cint(value);
	}
}