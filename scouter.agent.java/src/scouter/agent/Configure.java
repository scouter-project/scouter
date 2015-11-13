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
	public String objhost_type = "";
	public String objhost_name = "";

	public int objHash;
	public String objName;
	public int objHostHash;
	public String objHostName;

	public boolean enable_host_agent = false;
	public boolean enable_objname_pid = false;
	public boolean enable_plus_objtype = false;

	public boolean enable_asm_jdbc = true;
	public boolean enable_asm_socket = true;

	public boolean http_debug_querystring;
	public boolean http_debug_header;
	public String http_debug_header_url = "/";
	public boolean http_debug_parameter;
	public String http_debug_parameter_url = "/";

	/*
	 * user: 0 - remoteIp 1 - JSESSIONID + remoteIp 2 - SCOUTER(set-cookie)
	 */
	public int mode_userid = 2;

	public boolean enable_profile_summary = false;
	public boolean profile_thread_cputime = false;
	public boolean profile_socket_openstack = false;
	public int profile_socket_openstack_port = 0;
	public boolean profile_framework_sqlmap = true;

	public boolean listup_background_socket = true;

	public int xlog_time_limit = 0;

	public String http_error_status = "";
	private IntSet http_error_status_set = new IntSet();

	public String service_header_key;
	public String service_get_key;
	public String service_post_key;

	public File dump_dir = new File(".");
	public File subagent_dir = new File("./_scouter_");
	public File plugin_dir = new File("./_scouter_");

	public boolean enable_auto_dump = false;
	public int auto_dump_trigger = 10000;
	public long auto_dump_interval = 30000;
	public int auto_dump_level = 1;

	public int debug_long_tx_autostack = 0;

	public String http_static_contents = "js, htm, html, gif, png, jpg, css";
	private Set<String> static_contents = new HashSet<String>();

	public int alert_message_length = 3000;
	public long alert_send_interval = 3000;
	
	public int jdbc_fetch_max = 10000;
	public int sql_time_max = 30000;

	public int udp_packet_max = 60000;
	public boolean debug_asm;
	public boolean debug_udp_xlog;
	public boolean debug_udp_object;

	public long yellow_line_time = 3000;
	public long red_line_time = 8000;

	public String log_ignore = "";
	public StringSet log_ignore_set = new StringSet();

	public String hook_args = "";
	public String hook_return = "";
	public String hook_init = "";
	public String hook_connection_open = "";
	public boolean enable_trace_connection_open = true;
	public boolean enable_leaktrace_fullstack = false;
	public boolean debug_connection_open_fullstack = false;
	public boolean debug_connection_autocommit = false;

	public String hook_method = "";
	public String hook_method_ignore_prefix = "get,set";
	private String[] _hook_method_ignore_prefix = null;
	private int _hook_method_ignore_prefix_len = 0;

	public String hook_method_ignore_classes = "";
	private StringSet _hook_method_ignore_classes = new StringSet();

	public boolean hook_method_access_public = true;
	public boolean hook_method_access_private = false;
	public boolean hook_method_access_protected = false;
	public boolean hook_method_access_none = false;

	public boolean trace_method_enabled = true;

	public String hook_service = "";
	public String hook_apicall = "";
	public String hook_apicall_info = "";
	public String hook_jsp = "";

	public String hook_jdbc_pstmt = "";
	public String hook_jdbc_stmt = "";
	public String hook_jdbc_rs = "";

	public String hook_driver_connect_wrapper = "";

	// /LOAD CONTROL/////
	public boolean enable_reject_service = false;
	public int max_active_service = 10000;
	public boolean enable_reject_url = false;
	public String reject_text = "too many request!!";
	public String reject_url = "/error.html";

	public int profile_step_max = 1024;

	public boolean debug_background_sql = false;

	public boolean profile_fullstack_service_error = false;
	public boolean profile_fullstack_apicall_error = false;
	public boolean profile_fullstack_sql_error = false;
	public boolean profile_fullstack_sql_commit = false;

	public int profile_fullstack_lines = 0;
	public long udp_collection_interval = 100;
	public boolean profile_sql_escape = true;

	public String http_remote_ip_header_key = "";
	public boolean enable_trace_e2e = false;
	public String gxid = "scouter_gxid";
	public boolean enable_response_gxid = false;
	public String this_txid = "scouter_this_txid";
	public String caller_txid = "scouter_caller_txid";

	private int hook_signature;

	public String userid_jsessionid = "JSESSIONID";

	public boolean enable_auto_service_trace = false;
	public boolean enable_auto_service_backstack = true;

	public String hook_future_task = "";
	public String hook_future_task_prefix = "";

	// DEBUG OPTIONS
	public boolean enable_counter_task = true;
	public boolean enable_hook_service = true;
	public boolean enable_hook_dbsql = true;
	public boolean enable_hook_dbconn = true;
	public boolean enable_hook_cap = true;
	public boolean enable_hook_methods = true;
	public boolean enable_hook_socket = true;
	public boolean enable_hook_jsp = true;
	public boolean enable_hook_future = true;

	// //////////////////////////////////////////
	public boolean enable_usertx = true;
	public boolean enable_dbc_wrapper = true;

	public String direct_patch_class = "";

	public long max_think_time = DateUtil.MILLIS_PER_FIVE_MINUTE;
	public String object_registry = "/tmp/scouter";

	public boolean sfa_dump_enabled = false;
	public int sfa_dump_interval = 10000;

	public boolean enable_trace_web = false;
	public String key_web_name = "X-Forwarded-Host";
	public String key_web_time = "X-Forwarded-Time";

	public boolean enable_summary = true;
	public int summary_service_max = 5000;
	public int summary_sql_max = 5000;
	public int summary_api_max = 5000;
	public int summary_service_ip_max = 5000;
	public int summary_service_ua_max = 5000;
	public int summary_service_error_max = 500;

	public int heap_perm_warning_pct = 90;
	public long heap_perm_alert_interval = 30000;

	public boolean hook_spring_request_mapping = false;
	public boolean debug_sql_call = false;

	public int socket_open_fullstack_port = 0;

	/**
	 * sometimes call by sample application, at that time normally set some
	 * properties directly
	 */
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
		Logger.info("Version " + Version.getAgentFullVersion());
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
		String s = System.getProperty("scouter.config", "./scouter.conf");
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

		this.http_debug_querystring = getBoolean("http_debug_querystring", false);
		this.http_debug_header = getBoolean("http_debug_header", false);
		this.http_debug_parameter = getBoolean("http_debug_parameter", false);
		this.enable_profile_summary = getBoolean("enable_profile_summary", getBoolean("enable.profile.summary", false));
		this.xlog_time_limit = getInt("xlog_time_limit", getInt("xlog.time.limit", 0));
		this.service_header_key = getValue("service_header_key", getValue("service.header.key", null));
		this.service_get_key = getValue("service_get_key");
		this.service_post_key = getValue("service_post_key");

		this.http_error_status = getValue("http_error_status", getValue("http.error.status", ""));

		this.dump_dir = new File(getValue("dump_dir", getValue("dump.dir", ".")));
		try {
			this.dump_dir.mkdirs();
		} catch (Exception e) {
		}
		this.subagent_dir = new File(getValue("subagent_dir", getValue("subagent.dir", "./_scouter_")));
		try {
			this.subagent_dir.mkdirs();
		} catch (Exception e) {
		}
		this.plugin_dir = new File(getValue("plugin_dir", getValue("plugin.dir", "./_scouter_")));
		

		this.enable_auto_dump = getBoolean("enable_auto_dump", getBoolean("enable.auto.dump", false));
		this.auto_dump_trigger = getInt("auto_dump_trigger", getInt("auto.dump.trigger", 10000));
		if (this.auto_dump_trigger < 1) {
			this.auto_dump_trigger = 1;
		}
		this.auto_dump_level = getInt("auto_dump_level", getInt("auto.dump.level", 1));
		this.auto_dump_interval = getInt("auto_dump_interval", getInt("auto.dump.interval", 30000));
		if (this.auto_dump_interval < 5000) {
			this.auto_dump_interval = 5000;
		}
		this.debug_long_tx_autostack = getInt("debug_long_tx_autostack", 0);

		this.http_static_contents = getValue("http_static_contents",
				getValue("http.static.contents", "js, htm, html, gif, png, jpg, css"));

		this.profile_thread_cputime = getBoolean("profile_thread_cputime", getBoolean("profile.thread.cputime", false));
		this.profile_socket_openstack = getBoolean("profile_socket_openstack",
				getBoolean("profile.socket.openstack", false));
		this.listup_background_socket = getBoolean("listup_background_socket", true);
		this.profile_socket_openstack_port = getInt("profile_socket_openstack_port", 0);
		this.profile_sql_escape = getBoolean("profile_sql_escape", true);
		this.profile_framework_sqlmap = getBoolean("profile_framework_sqlmap", true);

		this.enable_asm_jdbc = getBoolean("enable_asm_jdbc", getBoolean("enable.asm.jdbc", true));
		this.enable_asm_socket = getBoolean("enable_asm_socket", getBoolean("enable.asm.socket", true));

		this.udp_packet_max = getInt("udp_packet_max", getInt("udp.packet.max", 60000));

		this.yellow_line_time = getLong("yellow_line_time", getLong("yellow.line.time", 3000));
		this.red_line_time = getLong("red_line_time", getLong("red.line.time", 8000));

		this.log_ignore = getValue("log_ignore", "");
		this.log_ignore_set = getStringSet("log_ignore", ",");

		this.debug_udp_xlog = getBoolean("debug_udp_xlog", getBoolean("debug.udp.xlog", false));
		this.debug_udp_object = getBoolean("debug_udp_object", getBoolean("debug.udp.object", false));

		this.local_udp_addr = getValue("local_udp_addr");
		this.local_udp_port = getInt("local_udp_port", 0);

		this.server_addr = getValue("server_addr", getValue("server.addr", "127.0.0.1"));
		this.server_udp_port = getInt("server_udp_port", getInt("server.port", NetConstants.SERVER_UDP_PORT));
		this.server_tcp_port = getInt("server_tcp_port", getInt("server.port", NetConstants.SERVER_TCP_PORT));
		this.server_tcp_session_count = getInt("server_tcp_session_count", 1, 1);
		this.server_tcp_connection_timeout = getInt("server_tcp_connection_timeout", 3000);
		this.server_tcp_so_timeout = getInt("server_tcp_so_timeout", 60000);

		this.hook_signature = 0;
		this.hook_args = getValue("hook_args", getValue("hook.args", ""));
		this.hook_return = getValue("hook_return", getValue("hook.return", ""));
		this.hook_init = getValue("hook_init", getValue("hook.init", ""));
		this.hook_connection_open = getValue("hook_connection_open", "");
		this.enable_trace_connection_open = getBoolean("enable_trace_connection_open", true);
		this.enable_leaktrace_fullstack = getBoolean("enable_leaktrace_fullstack", false);

		this.hook_method = getValue("hook_method", "");
		this.hook_method_access_public = getBoolean("hook_method_access_public", true);
		this.hook_method_access_protected = getBoolean("hook_method_access_protected", false);
		this.hook_method_access_private = getBoolean("hook_method_access_private", false);
		this.hook_method_access_none = getBoolean("hook_method_access_none", false);
		this.hook_method_ignore_prefix = StringUtil.removeWhitespace(getValue("hook_method_ignore_prefix", "get,set"));
		this._hook_method_ignore_prefix = StringUtil.split(this.hook_method_ignore_prefix, ",");
		this._hook_method_ignore_prefix_len = this._hook_method_ignore_prefix == null ? 0
				: this._hook_method_ignore_prefix.length;

		this.hook_method_ignore_classes = StringUtil.trimEmpty(StringUtil.removeWhitespace(getValue(
				"hook_method_ignore_classes", "")));
		this._hook_method_ignore_classes = new StringSet(StringUtil.tokenizer(
				this.hook_method_ignore_classes.replace('.', '/'), ","));

		this.trace_method_enabled = getBoolean("trace_method_enabled", true);

		this.hook_service = getValue("hook_service", "");
		this.hook_apicall = getValue("hook_apicall", "");
		this.hook_apicall_info = getValue("hook_apicall_info", "");
		this.hook_jsp = getValue("hook_jsp", "");

		this.hook_jdbc_pstmt = getValue("hook_jdbc_pstmt", "");
		this.hook_jdbc_stmt = getValue("hook_jdbc_stmt", "");
		this.hook_jdbc_rs = getValue("hook_jdbc_rs", "");
		this.hook_driver_connect_wrapper = getValue("hook_driver_connect_wrapper", "");

		this.hook_signature ^= this.hook_args.hashCode();
		this.hook_signature ^= this.hook_return.hashCode();
		this.hook_signature ^= this.hook_init.hashCode();
		this.hook_signature ^= this.hook_connection_open.hashCode();
		this.hook_signature ^= this.hook_method.hashCode();
		this.hook_signature ^= this.hook_service.hashCode();
		this.hook_signature ^= this.hook_apicall.hashCode();
		this.hook_signature ^= this.hook_jsp.hashCode();
		this.hook_signature ^= this.hook_driver_connect_wrapper.hashCode();

		this.enable_reject_service = getBoolean("enable_reject_service", false);
		this.max_active_service = getInt("max_active_service", 10000);
		this.enable_reject_url = getBoolean("enable_reject_url", false);
		this.reject_text = getValue("reject_text", "too many request!!");
		this.reject_url = getValue("reject_url", "/error.html");

		this.profile_step_max = getInt("profile_step_max", 1024);
		if (this.profile_step_max < 100)
			this.profile_step_max = 100;

		this.debug_background_sql = getBoolean("debug_background_sql", false);

		this.profile_fullstack_service_error = getBoolean("profile_fullstack_service_error", false);
		this.profile_fullstack_apicall_error = getBoolean("profile_fullstack_apicall_error", false);
		this.profile_fullstack_sql_error = getBoolean("profile_fullstack_sql_error", false);
		this.profile_fullstack_sql_commit = getBoolean("profile_fullstack_sql_commit", false);
		this.profile_fullstack_lines = getInt("profile_fullstack_lines", 0);

		this.udp_collection_interval = getInt("udp_collection_interval", 100);
		this.http_debug_parameter_url = getValue("http_debug_parameter_url", "/");
		this.http_debug_header_url = getValue("http_debug_header_url", "/");

		this.http_remote_ip_header_key = getValue("http_remote_ip_header_key", "");

		this.enable_trace_e2e = getBoolean("enable_trace_e2e", false);
		this.enable_response_gxid = getBoolean("enable_response_gxid", false);
		this.gxid = getValue("gxid", "scouter_gxid");
		this.this_txid = getValue("this_txid", "scouter_this_txid");
		this.caller_txid = getValue("caller_txid", "scouter_caller_txid");

		this.debug_connection_open_fullstack = getBoolean("debug_connection_open_fullstack", false);
		this.debug_connection_autocommit = getBoolean("debug_connection_autocommit", false);

		this.mode_userid = getInt("mode_userid", 2);

		this.userid_jsessionid = getValue("userid_jsessionid", "JSESSIONID");

		this.enable_host_agent = getBoolean("enable_host_agent", false);
		this.enable_auto_service_trace = getBoolean("enable_auto_service_trace", false);
		this.enable_auto_service_backstack = getBoolean("enable_auto_service_backstack", true);

		this.hook_future_task = getValue("hook_future_task", "");
		this.hook_future_task_prefix = getValue("hook_future_task_prefix", "");

		this.enable_counter_task = getBoolean("enable_counter_task", true);
		this.enable_hook_service = getBoolean("enable_hook_service", true);
		this.enable_hook_dbsql = getBoolean("enable_hook_dbsql", true);
		this.enable_hook_dbconn = getBoolean("enable_hook_dbconn", true);
		this.enable_hook_cap = getBoolean("enable_hook_cap", true);
		this.enable_hook_methods = getBoolean("enable_hook_methods", true);
		this.enable_hook_socket = getBoolean("enable_hook_socket", true);
		this.enable_hook_jsp = getBoolean("enable_hook_jsp", true);
		this.enable_hook_future = getBoolean("enable_hook_future", true);

		this.enable_dbc_wrapper = getBoolean("enable_dbc_wrapper", true);
		this.enable_usertx = getBoolean("enable_usertx", true);

		this.direct_patch_class = getValue("direct_patch_class", "");
		this.max_think_time = getLong("max_think_time", DateUtil.MILLIS_PER_FIVE_MINUTE);

		this.object_registry = getValue("object_registry", "/tmp/scouter");

		this.sfa_dump_enabled = getBoolean("sfa_dump_enabled", false);
		this.sfa_dump_interval = getInt("sfa_dump_interval", 10000);

		// 웹시스템으로 부터 WAS 사이의 성능과 어떤 웹서버가 요청을 보내 왔는지를 추적하는 기능을 ON/OFF하고
		// 관련 키정보를 지정한다.
		this.enable_trace_web = getBoolean("enable_trace_web", false);
		this.key_web_name = getValue("key_web_name", "X-Forwarded-Host");
		this.key_web_time = getValue("key_web_time", "X-Forwarded-Time");

		// SUMMARY최대 갯수를 관리한다.
		this.enable_summary = getBoolean("enable_summary", true);
		this.summary_sql_max = getInt("summary_sql_max", 5000);
		this.summary_api_max = getInt("summary_api_max", 5000);
		this.summary_service_max = getInt("summary_service_max", 5000);
		this.summary_service_ip_max = getInt("summary_service_ip_max", 5000);
		this.summary_service_ua_max = getInt("summary_service_ua_max", 5000);
		this.summary_service_error_max = getInt("summary_service_error_max", 500);

		this.heap_perm_alert_interval = getLong("heap_perm_alert_interval", 30000);
		this.heap_perm_warning_pct = getInt("heap_perm_warning_pct", 90);

		this.hook_spring_request_mapping = getBoolean("hook_spring_request_mapping", false);

		this.alert_message_length = getInt("alert_message_length", 3000);
		this.alert_send_interval = getInt("alert_send_interval", 3000);
		this.jdbc_fetch_max = getInt("jdbc_fetch_max", 10000);
		this.sql_time_max = getInt("sql_time_max", 30000);

		this.debug_asm = getBoolean("debug_asm", getBoolean("debug.asm", false));
		this.enable_plus_objtype = getBoolean("enable_plus_objtype", false);

		this.debug_sql_call = getBoolean("debug_sql_call", false);
		this.socket_open_fullstack_port = getInt("socket_open_fullstack_port", 0);

		resetObjInfo();
		setErrorStatus();
		setStaticContents();
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

	private void setStaticContents() {
		Set<String> tmp = new HashSet<String>();
		String[] s = StringUtil.split(this.http_static_contents, ',');
		for (int i = 0; i < s.length; i++) {
			String ss = s[i].trim();
			if (ss.length() > 0) {
				tmp.add(ss);
			}
		}
		static_contents = tmp;
	}

	public boolean isStaticContents(String content) {
		return static_contents.contains(content);
	}

	public boolean isIgnoreMethodPrefix(String name) {
		for (int i = 0; i < this._hook_method_ignore_prefix_len; i++) {
			if (name.startsWith(this._hook_method_ignore_prefix[i]))
				return true;
		}
		return false;
	}

	public boolean isIgnoreMethodClass(String classname) {
		return _hook_method_ignore_classes.hasKey(classname);
	}

	public synchronized void resetObjInfo() {
		String detected = ObjTypeDetector.drivedType != null ? ObjTypeDetector.drivedType
				: ObjTypeDetector.objType != null ? ObjTypeDetector.objType : CounterConstants.JAVA;

		this.scouter_type = getValue("scouter_type", detected);

		detected = CounterConstants.HOST;
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
		this.objhost_type = getValue("objhost_type", detected);
		this.objhost_name = getValue("objhost_name", SysJMX.getHostName());

		this.objHostName = "/" + this.objhost_name;
		this.objHostHash = HashUtil.hash(objHostName);

		this.enable_objname_pid = getBoolean("enable_objname_pid", false);
		String defaultName;
		if (this.enable_objname_pid == true) {
			defaultName = "" + SysJMX.getProcessPID();
		} else {
			defaultName = this.scouter_type + "1";
		}
		this.scouter_name = getValue("scouter_name", System.getProperty("jvmRoute", defaultName));

		this.objName = objHostName + "/" + this.scouter_name;
		this.objHash = HashUtil.hash(objName);

		System.setProperty("scouter.objname", this.objName);
		System.setProperty("scouter.objtype", this.scouter_type);
		System.setProperty("scouter.dir", new File(".").getAbsolutePath());
	}

	private void setErrorStatus() {
		String[] status = StringUtil.split(this.http_error_status, ',');
		http_error_status_set.clear();
		for (int i = 0; i < status.length; i++) {
			try {
				int code = Integer.parseInt(status[i].trim());
				if (code > 0) {
					http_error_status_set.add(code);
				}
			} catch (Exception e) {
			}
		}
	}

	public boolean isErrorStatus(int status) {
		return http_error_status_set.contains(status);
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
		Logger.info("Configure -Dscouter.config=" + propertyFile);
	}

	private static HashSet<String> ignoreSet = new HashSet<String>();

	static {
		ignoreSet.add("property");
		ignoreSet.add("objHash");
		ignoreSet.add("objHostHash");
		ignoreSet.add("objHostName");
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

	public int getHookSignature() {
		return this.hook_signature;
	}

	public static void main(String[] args) {
		System.out.println(Configure.getInstance().getKeyValueInfo().toString().replace(',', '\n'));
	}
}