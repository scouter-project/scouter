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

package scouter.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringLinkedSet;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

public class Configure extends Thread {

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


	//SERVER
	public String server_id = SysJMX.getHostName();

	//Log
	public boolean log_tcp_action_enabled = false;
	public boolean log_udp_multipacket = false;
	public boolean log_expired_multipacket = true;
	public boolean log_udp_packet = false;
	public boolean log_udp_counter = false;
	public boolean log_udp_xlog = false;
	public boolean log_udp_profile = false;
	public boolean log_udp_text = false;
	public boolean log_udp_alert = false;
	public boolean log_udp_object = false;
	public boolean log_udp_status = false;
	public boolean log_udp_stack = false;
	public boolean log_udp_summary = false;
	public boolean log_service_handler_list = false;
	public boolean log_rotation_enabled = true;
	public int log_keep_days = 31;
	public boolean log_sql_parsing_failed = false;

	//Network
	public String net_udp_listen_ip = "0.0.0.0";
	public int net_udp_listen_port = NetConstants.SERVER_UDP_PORT;
	public String net_tcp_listen_ip = "0.0.0.0";
	public int net_tcp_listen_port = NetConstants.SERVER_TCP_PORT;
	public int net_tcp_client_so_timeout_ms = 8000;
	public int net_tcp_agent_so_timeout_ms = 60000;
	public int net_tcp_agent_keepalive_interval_ms = 5000;
	public int net_tcp_get_agent_connection_wait_ms = 1000;
	public int net_udp_packet_buffer_size = 65535;
	public int net_udp_so_rcvbuf_size = 1024 * 1024 * 4;
	public int _net_udp_worker_thread_count = 3;
	public int net_tcp_service_pool_size = 100;

	//Dir
	public String db_dir = "./database";
	public String log_dir = "./logs";
	public String plugin_dir = "./plug-in";

	//Object
	public int object_deadtime_ms = 8000;

	//Compress
	public boolean compress_xlog_enabled = false;
	public boolean compress_profile_enabled = false;
	public int _compress_write_buffer_block_count = 3;
	public int _compress_read_cache_block_count = 3;
	public long _compress_read_cache_expired_ms = DateUtil.MILLIS_PER_MINUTE;
	public int _compress_dailycount_header_cache_size = 3;
	public int _compress_write_thread = 2;

	//Auto
	public boolean _auto_5m_sampling = true;

	//Manager
	public boolean mgr_purge_enabled = true;
	public boolean mgr_purge_only_xlog_enabled = false;
	public int mgr_purge_disk_usage_pct = 80;
	public int mgr_purge_keep_days = 0;
	public StringSet mgr_log_ignore_ids = new StringSet();

	//XLog
	public int xlog_queue_size = 10000;
	public int xlog_realtime_lower_bound_ms = 0;
	public int xlog_pasttime_lower_bound_ms = 0;
	public int xlog_profile_save_lower_bound_ms = 0;

	//Profile
	public int profile_queue_size = 1000;

	//GeoIP
	public boolean geoip_enabled = true;
	public String geoip_data_city_file = "./GeoLiteCity.dat";

	//SQL
	public boolean sql_table_parsing_enabled = true;

	//TagCount
	public boolean tagcnt_enabled = false;

	//Summary
	public boolean summary_alert_enabled = true;

	private Configure() {
		reload(false);
	}

	/**
	 * @deprecated
	 */
	private Configure(boolean b) {
	}

	private long last_load_time = -1;
	public Properties property = new Properties();

	private boolean running = true;

	public void run() {
		while (running) {
			reload(false);
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

		return true;
	}

	public static boolean WORKABLE = true;

	private void apply() {
		this.xlog_queue_size = getInt("xlog_queue_size", 10000);
		this.profile_queue_size = getInt("profile_queue_size", 1000);
		this.log_tcp_action_enabled = getBoolean("log_tcp_action_enabled", false);

		this.net_udp_listen_ip = getValue("net_udp_listen_ip", "0.0.0.0");
		this.net_udp_listen_port = getInt("net_udp_listen_port", NetConstants.SERVER_UDP_PORT);
		this.net_tcp_listen_ip = getValue("net_tcp_listen_ip", "0.0.0.0");
		this.net_tcp_listen_port = getInt("net_tcp_listen_port", NetConstants.SERVER_TCP_PORT);
		this.net_tcp_client_so_timeout_ms = getInt("net_tcp_client_so_timeout_ms", 8000);
		this.net_tcp_agent_so_timeout_ms = getInt("net_tcp_agent_so_timeout_ms", 60000);
		this.net_tcp_agent_keepalive_interval_ms = getInt("net_tcp_agent_keepalive_interval_ms", 5000);
		this.net_tcp_get_agent_connection_wait_ms = getInt("net_tcp_get_agent_connection_wait_ms", 1000);

		this.server_id = getValue("server_id", SysJMX.getHostName());
		this.db_dir = getValue("db_dir", "./database");
		this.log_dir = getValue("log_dir", "./logs");
		this.plugin_dir = getValue("plugin_dir", "./plug-in");

		this.object_deadtime_ms = getInt("object_deadtime_ms", 8000);

		this.compress_xlog_enabled = getBoolean("compress_xlog_enabled", false);
		this.compress_profile_enabled = getBoolean("compress_profile_enabled", false);
		this._compress_write_buffer_block_count = getInt("_compress_write_buffer_block_count", 3);
		this._compress_dailycount_header_cache_size = getInt("_compress_dailycount_header_cache_size", 3);
		this._compress_read_cache_block_count = getInt("_compress_read_cache_block_count", 3);
		this._compress_read_cache_expired_ms = getLong("_compress_read_cache_expired_ms", DateUtil.MILLIS_PER_MINUTE);
		this._compress_write_thread = getInt("_compress_write_thread", 2);
		
		this.net_udp_packet_buffer_size = getInt("net_udp_packet_buffer_size", 65535);

		int default_so_rcvbuf = 1024 * 1024 * 4;
		if (SystemUtil.IS_AIX || SystemUtil.IS_HP_UX) {
			default_so_rcvbuf = 0;
		}
		this.net_udp_so_rcvbuf_size = getInt("net_udp_so_rcvbuf_size", default_so_rcvbuf);
		this.log_expired_multipacket = getBoolean("log_expired_multipacket", true);
		this.log_udp_multipacket = getBoolean("log_udp_multipacket", false);
		this.log_udp_packet = getBoolean("log_udp_packet", false);
		this.log_udp_counter = getBoolean("log_udp_counter", false);
		this.log_udp_xlog = getBoolean("log_udp_xlog", false);
		this.log_udp_profile = getBoolean("log_udp_profile", false);
		this.log_udp_text = getBoolean("log_udp_text", false);
		this.log_udp_alert = getBoolean("log_udp_alert", false);
		this.log_udp_object = getBoolean("log_udp_object", false);
		this.log_udp_status = getBoolean("log_udp_status", false);
		this.log_udp_stack = getBoolean("log_udp_stack", false);
		this.log_udp_summary = getBoolean("log_udp_summary", false);
		this.log_service_handler_list = getBoolean("log_service_handler_list", false);
		this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
		this.log_keep_days = getInt("log_keep_days", 31);
		this.log_sql_parsing_failed = getBoolean("log_sql_parsing_failed", false);

		this._auto_5m_sampling = getBoolean("_auto_5m_sampling", true);

		this.xlog_realtime_lower_bound_ms = getInt("xlog_realtime_lower_bound_ms", 0);
		this.xlog_pasttime_lower_bound_ms = getInt("xlog_pasttime_lower_bound_ms", 0);
		this.mgr_purge_enabled = getBoolean("mgr_purge_enabled", true);
		this.mgr_purge_only_xlog_enabled = getBoolean("mgr_purge_only_xlog_enabled", false);
		this.mgr_purge_disk_usage_pct = getInt("mgr_purge_disk_usage_pct", 80);
		this.mgr_purge_keep_days = getInt("mgr_purge_keep_days", 0);
		this._net_udp_worker_thread_count = getInt("_net_udp_worker_thread_count", 3);
		this.geoip_data_city_file = getValue("geoip_data_city_file", "./GeoLiteCity.dat");
		this.geoip_enabled = getBoolean("geoip_enabled", true);

		this.xlog_profile_save_lower_bound_ms = getInt("xlog_profile_save_lower_bound_ms", 0);
		this.sql_table_parsing_enabled = getBoolean("sql_table_parsing_enabled", true);

		this.mgr_log_ignore_ids = getStringSet("mgr_log_ignore_ids", ",");

		this.tagcnt_enabled = getBoolean("tagcnt_enabled", false);
		
		this.net_tcp_service_pool_size = getInt("net_tcp_service_pool_size", 100);
		this.summary_alert_enabled =  getBoolean("summary_alert_enabled", true);
		
		ConfObserver.exec();
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
			File parentDir = file.getParentFile();
			parentDir.mkdirs();
			out = new FileOutputStream(file);
			out.write(text.getBytes());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(out);
		}
		return false;
	}

	public static void main(String[] args) {
		StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(new Configure(true));
		StringEnumer enu = defMap.keys();
		while (enu.hasMoreElements()) {
			String key = enu.nextString();
			if (ignoreSet.contains(key))
				continue;
			System.out.println(key + " : " + ConfigValueUtil.toValue(defMap.get(key)));
		}
	}

	private static HashSet<String> ignoreSet = new HashSet<String>();

	static {
		ignoreSet.add("property");
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

	public static StringLinkedSet toOrderSet(String values, String deli) {
		StringLinkedSet set = new StringLinkedSet();
		StringTokenizer nizer = new StringTokenizer(values, deli);
		while (nizer.hasMoreTokens()) {
			String s = StringUtil.trimToEmpty(nizer.nextToken());
			if (s.length() > 0) {
				set.put(s);
			}
		}
		return set;
	}

}