/*
 *  Copyright 2015 LG CNS.
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

package scouter.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import scouter.Version;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.conf.ConfObserver;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.IntSet;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

public class Configure extends Thread {

	public static boolean JDBC_REDEFINED = false;
	private static Configure instance = null;
	public final static String CONF_DIR = "./conf/";
	public final static synchronized Configure getInstance() {
		if (instance == null) {
			instance = new Configure();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	public String local_udp_addr = null;
	public int local_udp_port;

	public String server_addr = "127.0.0.1";
	public int server_udp_port = NetConstants.SERVER_UDP_PORT;
	public int server_tcp_port = NetConstants.SERVER_TCP_PORT;
	public int server_tcp_session_count = 1;
	public int server_tcp_so_timeout = 60000;
	public int server_tcp_connection_timeout = 3000;

	public String scouter_type = "";
	public String scouter_name = "";

	public int objHash;
	public String objName;

	public int alert_message_length = 3000;
	public long alert_send_interval = 3000;
	public int alert_fetch_count = 100000;
	public int alert_sql_time = 30000;

	public int udp_packet_max = 60000;
	public StringSet log_ignore = new StringSet();
	public int max_concurrent_server_request = 10;

	public boolean enable_counter_task = true;
	public boolean debug_udp_object = false;

	public boolean log_rotation = true;
	public String logs_dir = "./logs";
	public int log_keep_dates = 365;

	public String object_registry="./tmp/scouter";
	
	private Configure() {
		Properties p = new Properties();
		Map args = new HashMap();
		args.putAll(System.getenv());
		args.putAll(System.getProperties());
		p.putAll(args);
		this.property = p;
		reload(false);
	}

	private Configure(boolean b) {
	}

	private long last_load_time = -1;
	public Properties property = new Properties();

	private boolean running = true;

	public void run() {
		Logger.println("Version " + Version.getAgentFullVersion());
		long dateUnit = DateUtil.getDateUnit();
		while (running) {
			reload(false);
			// Text Data Reset..
			long nowUnit = DateUtil.getDateUnit();
			if (dateUnit != nowUnit) {
				dateUnit = nowUnit;
				DataProxy.reset();
			}
			ThreadUtil.sleep(3000);
		}
	}

	private File propertyFile;

	public File getPropertyFile() {
		if (propertyFile != null) {
			return propertyFile;
		}
		String s = System.getProperty("scouter.config", CONF_DIR + "scouter.conf");
		propertyFile = new File(s.trim());
		return propertyFile;
	}

	long last_check = 0;

	public synchronized boolean reload(boolean force) {
		long now = System.currentTimeMillis();
		if (force == false && now < last_check + 3000)
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
		property = ConfigValueUtil.replaceSysProp(temp);
		apply();
		ConfObserver.run();
		return true;
	}

	private void apply() {

		this.udp_packet_max = getInt("udp_packet_max", getInt("udp.packet.max", 60000));
		this.log_ignore = getStringSet("log_ignore", ",");

		this.local_udp_addr = getValue("local_udp_addr");
		this.local_udp_port = getInt("local_udp_port", 0);

		this.server_addr = getValue("server_addr", getValue("server.addr", "127.0.0.1"));
		this.server_udp_port = getInt("server_udp_port", getInt("server.port", NetConstants.SERVER_UDP_PORT));
		this.server_tcp_port = getInt("server_tcp_port", getInt("server.port", NetConstants.SERVER_TCP_PORT));
		this.server_tcp_session_count = getInt("server_tcp_session_count", 1, 1);
		this.server_tcp_connection_timeout = getInt("server_tcp_connection_timeout", 3000);
		this.server_tcp_so_timeout = getInt("server_tcp_so_timeout", 60000);

		this.max_concurrent_server_request = getInt("max_concurrent_server_request", 10);
		this.enable_counter_task = getBoolean("enable_counter_task", true);
		this.debug_udp_object = getBoolean("debug_udp_object", false);

		this.logs_dir = getValue("logs_dir", "./logs");
		this.log_rotation = getBoolean("log_rotation", true);
		this.log_keep_dates = getInt("log_keep_dates", 365);

		this.object_registry = getValue("object_registry", "./tmp/scouter");

		resetObjInfo();
	}

	private StringSet getStringSet(String key, String deli) {
		StringSet set = new StringSet();
		String v = getValue(key);
		if (v != null) {
			String[] vv = StringUtil.split(v, deli);
			for (String x : vv) {
				x = StringUtil.trimToEmpty(x);
				if (x.length() > 0)
					set.put(x);
			}
		}
		return set;
	}

	public synchronized void resetObjInfo() {
		String detected = CounterConstants.HOST;
		if (SystemUtil.IS_LINUX) {
			detected = CounterConstants.LINUX;
		} else if (SystemUtil.IS_WINDOWS) {
			detected = CounterConstants.WINDOWS;
		} else if (SystemUtil.IS_MAC_OSX) {
			detected = CounterConstants.OSX;
		} else if (SystemUtil.IS_AIX) {
			detected = CounterConstants.AIX;
		} else if (SystemUtil.IS_HP_UX) {
			detected = CounterConstants.HPUX;
		}
		this.scouter_type = getValue("scouter_type", detected);
		this.scouter_name = getValue("scouter_name", SysJMX.getHostName());

		this.objName = "/" + this.scouter_name;
		this.objHash = HashUtil.hash(objName);

		this.alert_message_length = getInt("alert_message_length", 3000);
		this.alert_send_interval = getInt("alert_send_interval", 3000);
		this.alert_fetch_count = getInt("alert_fetch_count", 100000);
		this.alert_sql_time = getInt("alert_sql_time", 30000);

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

	public int getInt(String key, int def, int min) {
		try {
			String v = getValue(key);
			if (v != null) {
				return Math.max(Integer.parseInt(v), min);
			}
		} catch (Exception e) {
		}
		return Math.max(def, min);
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

	public String loadText() {
		File file = getPropertyFile();
		InputStream fin = null;
		try {
			fin = new FileInputStream(file);
			byte[] buff = FileUtil.readAll(fin);
			return new String(buff);
		} catch (Exception e) {
		} finally {
			FileUtil.close(fin);
		}
		return null;
	}

	public boolean saveText(String text) {
		File file = getPropertyFile();
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(text.getBytes());
			return true;
		} catch (Exception e) {
		} finally {
			FileUtil.close(out);
		}
		return false;
	}

	public void printConfig() {
		System.out.println("Configure -Dscouter.config=" + propertyFile);
	}

	private static HashSet<String> ignoreSet = new HashSet<String>();

	static {
		ignoreSet.add("property");
		ignoreSet.add("objHash");
		ignoreSet.add("objName");
		ignoreSet.add("objType");
	}

	public MapValue getKeyValueInfo() {
		StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(new Configure(true));
		StringKeyLinkedMap<Object> curMap = ConfigValueUtil.getConfigDefault(this);
		MapValue m = new MapValue();
		ListValue nameList = m.newList("key");
		ListValue valueList = m.newList("value");
		ListValue defList = m.newList("default");

		StringEnumer enu = defMap.keys();
		while (enu.hasMoreElements()) {
			String key = enu.nextString();
			if (ignoreSet.contains(key))
				continue;
			nameList.add(key);
			valueList.add(ConfigValueUtil.toValue(curMap.get(key)));
			defList.add(ConfigValueUtil.toValue(defMap.get(key)));
		}

		return m;
	}

	public static void main(String[] args) {
		System.out.println(Configure.getInstance().getKeyValueInfo().toString().replace(',', '\n'));
	}
}