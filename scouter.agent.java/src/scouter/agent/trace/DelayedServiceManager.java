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
package scouter.agent.trace;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.MapValue;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.ThreadUtil;

public class DelayedServiceManager extends Thread {
	
	static final String PREFIX_SERVICE = "service.";
	static final String PREFIX_TIME = "time.";
	private static volatile DelayedServiceManager instance;
	Configure conf = Configure.getInstance();
	long lastModifiedTime;
	static volatile boolean stopChecking = false;
	
	HashMap<Integer, DelayedCondition> conditionMap = new HashMap<Integer, DelayedCondition>();
	
	public static DelayedServiceManager getInstance() {
		if (instance == null) {
			synchronized (DelayedServiceManager.class) {
				if (instance == null) {
					instance = new DelayedServiceManager();
					instance.setDaemon(true);
					instance.setName(ThreadUtil.getName(instance));
					instance.start();
				}
			}
		}
		return instance;
	}
	
	public void run() {
		while (conf.isAlive()) {
			load();
			ThreadUtil.sleep(10000);
		}
	}

	private void load() {
		File dir = conf.getPropertyFile().getParentFile();
		String filename = conf.trace_delayed_service_mgr_filename;
		File file = new File(dir, filename);
		if (file.lastModified() == lastModifiedTime) {
			return;
		}
		try {
			stopChecking = true;
			Logger.println("SA-2001", "Load delayed service configure file : " + file.getAbsolutePath());
			lastModifiedTime = file.lastModified();
			Properties properties = new Properties();
			if (file.canRead()) {
				FileInputStream fis = null; 
				try {
					fis = new FileInputStream(file);
					properties.load(fis);
				} catch (Exception e) {
					Logger.println("SA-2002", "Load error delayed service property file", e);
				} finally {
					FileUtil.close(fis);
				}
			}
			HashMap<Integer, DelayedCondition> indexMap = new HashMap<Integer, DelayedCondition>();
			for (Object key : properties.keySet()) {
				try {
					String name = key.toString();
					if (name.startsWith(PREFIX_SERVICE)) {
						int index = Integer.valueOf(name.substring(PREFIX_SERVICE.length()));
						DelayedCondition condition = indexMap.get(index);
						if (condition == null) {
							condition = new DelayedCondition();
							indexMap.put(index, condition);
						}
						condition.service = HashUtil.hash(properties.getProperty(name).trim());
					} else if (name.startsWith(PREFIX_TIME)) {
						int index = Integer.valueOf(name.substring(PREFIX_TIME.length()));
						DelayedCondition condition = indexMap.get(index);
						if (condition == null) {
							condition = new DelayedCondition();
							indexMap.put(index, condition);
						}
						condition.time = Integer.valueOf(properties.getProperty(name));
					}
				} catch (Exception e) { }
			}
			conditionMap.clear();
			for (DelayedCondition condition : indexMap.values()) {
				if (condition.service != 0) {
					conditionMap.put(condition.service, condition);
				}
			}
		} finally {
			stopChecking = false;
		}
	}
	
	public void checkDelayedService(XLogPack pack, String serviceName) {
		if (stopChecking) {
			Logger.println("SA-2003", "Pass delayed checking... " + Hexa32.toString32(pack.txid));
			return;
		}
		DelayedCondition condition = conditionMap.get(pack.service);
		if (condition != null && pack.elapsed > condition.time) {
			StringBuilder msg = new StringBuilder();
			msg.append(serviceName + System.getProperty("line.separator"));
			msg.append(pack.elapsed + " ms (SET:" + condition.time + " ms)" + System.getProperty("line.separator"));
			msg.append("ID = " + Hexa32.toString32(pack.txid));
			MapValue tags = null;
			if (pack.error != 0) {
				tags = new MapValue();
				tags.put(AlertPack.HASH_FLAG + TextTypes.ERROR + "_error", pack.error);
			}
			StringBuilder titleSb = new StringBuilder();
			titleSb.append("DELAYED_SERVICE");
			if (serviceName != null && serviceName.length() > 15) {
				titleSb.append("(...");
				titleSb.append(serviceName.substring(serviceName.length() - 10));
				titleSb.append(")");
			} else {
				titleSb.append("(");
				titleSb.append(serviceName);
				titleSb.append(")");
			}
			AlertProxy.sendAlert(AlertLevel.WARN, titleSb.toString(), msg.toString(), tags);
		}
	}
	
	private static class DelayedCondition {
		public int service;
		public int time;
	}

}
