package scouter.server.alert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import scouter.lang.CounterKey;
import scouter.lang.value.Value;
import scouter.server.Configure;
import scouter.util.CastUtil;
import scouter.util.FileUtil;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

public class AlertEngine {
	static HashMap<CounterKey, Counter> realTime = new HashMap<CounterKey, Counter>();

	public static void putRealTime(CounterKey key, Value value) {
		AlertRule rule = ruleInst.get(key.counter);
		if (rule == null)
			return;

		if (value instanceof Number == false) {
			return;
		}
		AlertConf conf = ruleConf.get(key.counter);
		Counter c = realTime.get(key);
		if (c == null) {
			c = new Counter(key.objHash, conf.keep_history);
			realTime.put(key, c);
		}
		c.value = (Number) value;
		rule.process(c);
		c.addValueHistory((Number) value);
	}

	public static void putDaily(int yyyymmdd, CounterKey key, int hhmm, Value value) {
	}

	private static StringKeyLinkedMap<String> ruleBody = new StringKeyLinkedMap<String>();
	private static StringKeyLinkedMap<AlertRule> ruleInst = new StringKeyLinkedMap<AlertRule>();
	private static StringKeyLinkedMap<AlertConf> ruleConf = new StringKeyLinkedMap<AlertConf>();

	public static synchronized AlertRule create(String name, String body, AlertConf conf) throws Exception {
		if (body.equals(ruleBody.get(name)))
			return ruleInst.get(name);

		ClassPool cp = ClassPool.getDefault();
		String jar = FileUtil.getJarFileName(AlertRule.class);
		if (jar != null) {
			cp.appendClassPath(jar);
		}
		CtClass cc = cp.get("scouter.server.alert.AlertRule");
		CtClass impl = cp.makeClass(name, cc);

		CtMethod method = CtNewMethod.make("public void process(scouter.server.alert.Counter c){}", impl);
		impl.addMethod(method);
		method.setBody("{scouter.server.alert.Counter c=$1;" + body + "}");
		Class c = impl.toClass();

		AlertRule rule = (AlertRule) c.newInstance();
		ruleInst.put(name, rule);
		ruleBody.put(name, body);
		ruleConf.put(name, conf);
		return rule;
	}

	public static void load() {
		try {
			String root = Configure.getInstance().alert_rule_dir;
			File[] file = new File(root).listFiles();
			for (int i = 0; file != null && i < file.length; i++) {
				String name = file[i].getName();
				if (name.endsWith(".rule") == false)
					continue;
				name = name.substring(0, name.length() - ".rule".length());
				String body = new String(FileUtil.readAll(file[i]));
				AlertConf conf = loadConf(name);
				create(name, body, conf);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static AlertConf loadConf(String name) throws IOException {
		AlertConf conf = new AlertConf();
		File confFile = new File(name + ".conf");
		if (confFile.canRead()) {
			Properties p = new Properties();
			p.load(new ByteArrayInputStream(FileUtil.readAll(confFile)));
			conf.keep_history = getInt(p, "keep_history", 0);
		}
		return conf;
	}

	private static int getInt(Properties p, String key, int defValue) {
		String value = StringUtil.trimEmpty(p.getProperty(key));
		if (value.length() == 0)
			return defValue;
		return CastUtil.cint(value);
	}

	public static void main(String[] args) {
		System.setProperty("alert_rule_dir", "/app/stage/scouter/server/conf/alert");
		load();
	}
}
