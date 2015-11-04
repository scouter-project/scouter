package scouter.server.alert;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import scouter.lang.CounterKey;
import scouter.lang.value.Value;
import scouter.server.Configure;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;

public class AlertEngine {
	static HashMap<CounterKey, Counter> realTime = new HashMap<CounterKey, Counter>();

	public static void putRealTime(CounterKey key, Value value) {
		AlertRule rule = ruleInst.get(key.counter);
		if (rule == null)
			return;

		if (value instanceof Number == false) {
			return;
		}

		Counter c = realTime.get(key);
		if (c == null) {
			c = new Counter();
			c.objHash = key.objHash;
			realTime.put(key, c);
		}
		c.value = (Number)value;
		rule.process(c);
		c.history.putFirst(System.currentTimeMillis(), (Number)value);
	}

	public static void putDaily(int yyyymmdd, CounterKey key, int hhmm, Value value) {
	}

	private static StringKeyLinkedMap<String> ruleBody = new StringKeyLinkedMap<String>();
	private static StringKeyLinkedMap<AlertRule> ruleInst = new StringKeyLinkedMap<AlertRule>();

	public static synchronized AlertRule create(String name, String body) throws Exception {
		if (body.equals(ruleBody.get(name)))
			return ruleInst.get(name);

		ClassPool cp = ClassPool.getDefault();
		CtClass cc =cp.get("scouter.server.alert.AlertRule");
		CtClass impl = cp.makeClass( name, cc);
		
		CtMethod method = CtNewMethod.make("public void process(scouter.server.alert.Counter c){}", impl);
		impl.addMethod(method);
		method.setBody("{scouter.server.alert.Counter c=$1;"+body+"}");
		Class c = impl.toClass();

		AlertRule rule = (AlertRule) c.newInstance();
		ruleInst.put(name, rule);
		ruleBody.put(name, body);
		return rule;
	}

	public static void load() {
		try {
			String root = Configure.getInstance().alert_rule_dir;
			File[] file = new File(root).listFiles();
			for (int i = 0; file != null && i < file.length; i++) {
				String name = file[i].getName();
				if (name.endsWith(".alert") == false)
					continue;
				name = name.substring(0, name.length() - ".alert".length());
				String body = new String(FileUtil.readAll(file[i]));
				create(name, body);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	public static void main(String[] args) {
		ClassPool cp = ClassPool.getDefault();
		URL u = cp.find("scouter.server.alert.AlertRule");
		System.out.println(u);
	}
}
