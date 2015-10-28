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
package scouter.client.daemon;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import scouter.util.FileUtil;
import scouter.util.ParamText;
import scouter.util.StringLinkedSet;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

public class Configure extends Thread {
	private static Configure instance = null;
	
	public StringLinkedSet scouter_server;
	public int long_content_length = 100;
	
	public final static synchronized Configure getInstance() {
		if (instance == null) {
			instance = new Configure();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private Configure() {
		Properties p = new Properties();
		Map args = new HashMap();
		args.putAll(System.getenv());
		args.putAll(System.getProperties());
		p.putAll(args);
		this.property = p;
		reload();
	}

	private long last_load_time = -1;
	public Properties property = new Properties();

	private boolean running = true;

	public void run() {
		while (running) {
			reload();
			ThreadUtil.sleep(3000);
		}
	}

	private File propertyFile;

	public File getPropertyFile() {
		if (propertyFile != null) {
			return propertyFile;
		}
		String s = System.getProperty("scouter.config", "./scouter.conf");
		propertyFile = new File(s.trim());
		return propertyFile;
	}

	long last_check = 0;

	public synchronized boolean reload() {
		long now = System.currentTimeMillis();
		if (now < last_check + 3000)
			return false;
		last_check = now;

		File file = getPropertyFile();

		if (file.lastModified() == last_load_time) {
			return false;
		}

		last_load_time = file.lastModified();
		Properties temp = new Properties();

		if (file.canRead()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				temp.load(in);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileUtil.close(in);
			}
		}
		property = replaceSysProp(temp);
		apply();

		return true;
	}

	private Properties replaceSysProp(Properties temp) {
		Properties p = new Properties();

		Map args = new HashMap();
		args.putAll(System.getenv());
		args.putAll(System.getProperties());

		p.putAll(args);

		Iterator itr = temp.keySet().iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			String value = (String) temp.get(key);
			p.put(key, new ParamText(StringUtil.trim(value)).getText(args));
		}
		return p;
	}

	private void apply() {
		this.scouter_server = toOrderSet(getValue("scouter_server", ""), ",;");
		this.long_content_length = getInt("long_content_length", 100);
	}

	public String getValue(String key) {
		return StringUtil.trim(property.getProperty(key));
	}

	public String getValue(String key, String def) {
		return StringUtil.trim(property.getProperty(key, def));
	}

	public int getInt(String key, int def) {
		try {
			String v = getValue(key);
			if (v != null)
				return Integer.parseInt(v);
		} catch (Exception e) {
		}
		return def;
	}

	public long getLong(String key, long def) {
		try {
			String v = getValue(key);
			if (v != null)
				return Long.parseLong(v);
		} catch (Exception e) {
		}
		return def;
	}

	public boolean getBoolean(String key, boolean def) {
		try {
			String v = getValue(key);
			if (v != null)
				return Boolean.parseBoolean(v);
		} catch (Exception e) {
		}
		return def;
	}


	public StringLinkedSet toOrderSet(String values, String deli) {
		StringLinkedSet set= new StringLinkedSet();
		StringTokenizer nizer=new StringTokenizer(values, deli);
		while(nizer.hasMoreTokens()){
			String s = StringUtil.trimToEmpty(nizer.nextToken());
			if(s.length()>0){
				set.put(s);
			}
		}
		return set;
	}

	public static void main(String[] args) {

	}
}