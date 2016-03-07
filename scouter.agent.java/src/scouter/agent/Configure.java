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

import scouter.Version;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.conf.ConfObserver;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.*;

import java.io.*;
import java.util.*;
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

	//Network
	public String net_local_udp_ip = null;
	public int net_local_udp_port;
	public String net_collector_ip = "127.0.0.1";
	public int net_collector_udp_port = NetConstants.SERVER_UDP_PORT;
	public int net_collector_tcp_port = NetConstants.SERVER_TCP_PORT;
	public int net_collector_tcp_session_count = 1;
	public int net_collector_tcp_so_timeout_ms = 60000;
	public int net_collector_tcp_connection_timeout_ms = 3000;
	public int net_udp_packet_max_bytes = 60000;
	public long net_udp_collection_interval_ms = 100;

	//Object
	public String obj_type = "";
	public String obj_name = "";
	public String obj_host_type = "";
	public String obj_host_name = "";
	public boolean obj_name_auto_pid_enabled = false;
	public boolean obj_type_inherit_to_child_enabled = false;

	//profile
	public boolean profile_http_querystring_enabled;
	public boolean profile_http_header_enabled;
	public String profile_http_header_url_prefix = "/";
	public boolean profile_http_parameter_enabled;
	public String profile_http_parameter_url_prefix = "/";
	public boolean profile_summary_mode_enabled = false;
	public boolean profile_thread_cputime_enabled = false;
	public boolean profile_socket_open_fullstack_enabled = false;
	public int profile_socket_open_fullstack_port = 0;
	public boolean profile_sqlmap_name_enabled = true;
	public boolean profile_connection_open_enabled = true;
	public boolean profile_connection_open_fullstack_enabled = false;
	public boolean profile_connection_autocommit_status_enabled = false;
	public boolean profile_method_enabled = true;
	public int profile_step_max_count = 1024;
	public boolean profile_fullstack_service_error_enabled = false;
	public boolean profile_fullstack_apicall_error_enabled = false;
	public boolean profile_fullstack_sql_error_enabled = false;
	public boolean profile_fullstack_sql_commit_enabled = false;
	public int profile_fullstack_max_lines = 0;
	public boolean profile_sql_escape_enabled = true;
	public boolean _profile_fullstack_sql_connection_enabled = false;

	//Trace
	public int trace_user_mode = 2; // 0:Remote IP, 1:JSessionID, 2:SetCookie
	public boolean trace_background_socket_enabled = true;
	public String trace_service_name_header_key;
	public String trace_service_name_get_key;
	public String trace_service_name_post_key;
	public long trace_activeserivce_yellow_time = 3000;
	public long trace_activeservice_red_time = 8000;
	public String trace_http_client_ip_header_key = "";
	public boolean trace_interservice_enabled = false;
	public String _trace_interservice_gxid_header_key = "X-Scouter-Gxid";
	public boolean trace_response_gxid_enabled = false;
	public String _trace_interservice_callee_header_key = "X-Scouter-Callee";
	public String _trace_interservice_caller_header_key = "X-Scouter-Caller";
	public String trace_user_session_key = "JSESSIONID";
	public boolean _trace_auto_service_enabled = false;
	public boolean _trace_auto_service_backstack_enabled = true;
	public boolean trace_db2_enabled = true;
	public boolean trace_webserver_enabled = false;
	public String trace_webserver_name_header_key = "X-Forwarded-Host";
	public String trace_webserver_time_header_key = "X-Forwarded-Time";
	public int _trace_fullstack_socket_open_port = 0;

	//Dir
	public File plugin_dir = new File("./plugin");
	public File dump_dir = new File("./dump");
	//public File mgr_agent_lib_dir = new File("./_scouter_");

	//Manager
	public String mgr_static_content_extensions = "js, htm, html, gif, png, jpg, css";
	public String mgr_log_ignore_ids = "";

	//Autodump
	public boolean autodump_enabled = false;
	public int autodump_trigger_active_service_cnt = 10000;
	public long autodump_interval_ms = 30000;
	public int autodump_level = 1; // 1:ThreadDump, 2:ActiveService, 3:ThreadList
	public int autodump_stuck_thread_ms = 0;

	//XLog
	public int xlog_lower_bound_time_ms = 0;
	public int xlog_error_jdbc_fetch_max = 10000;
	public int xlog_error_sql_time_max_ms = 30000;
	public boolean xlog_error_check_user_transaction_enabled = true;

	//Alert
	public int alert_message_length = 3000;
	public long alert_send_interval_ms = 3000;
	public int alert_perm_warning_pct = 90;

	//Log
	public boolean _log_asm_enabled;
	public boolean _log_udp_xlog_enabled;
	public boolean _log_udp_object_enabled;
	public boolean _log_udp_counter_enabled;
	public boolean _log_datasource_lookup_enabled = true;
	public boolean _log_background_sql = false;
	public String log_dir ="";
	public boolean log_rotation_enabled =true;
	public int log_keep_days =7;
	public boolean _log_trace_enabled = false;
    public boolean _log_trace_use_logger = false;

	//Hook
	public String hook_args_patterns = "";
	public String hook_return_patterns = "";
	public String hook_constructor_patterns = "";
	public String hook_connection_open_patterns = "";
	public String hook_context_classes = "javax/naming/InitialContext";
	public String hook_method_patterns = "";
	public String hook_method_ignore_prefixes = "get,set";
	public String hook_method_ignore_classes = "";
	private StringSet _hook_method_ignore_classes = new StringSet();
	public boolean hook_method_access_public_enabled = true;
	public boolean hook_method_access_private_enabled = false;
	public boolean hook_method_access_protected_enabled = false;
	public boolean hook_method_access_none_enabled = false;
	public String hook_service_patterns = "";
	public String hook_apicall_patterns = "";
	public String hook_apicall_info_patterns = "";
	public String hook_jsp_patterns = "";
	public String hook_jdbc_pstmt_classes = "";
	public String hook_jdbc_stmt_classes = "";
	public String hook_jdbc_rs_classes = "";
	public String hook_jdbc_wrapping_driver_patterns = "";
	public String hook_add_fields = "";
	public boolean _hook_serivce_enabled = true;
	public boolean _hook_dbsql_enabled = true;
	public boolean _hook_dbconn_enabled = true;
	public boolean _hook_cap_enabled = true;
	public boolean _hook_methods_enabled = true;
	public boolean _hook_socket_enabled = true;
	public boolean _hook_jsp_enabled = true;
	public boolean _hook_async_enabled = true;
	public boolean _hook_usertx_enabled = true;
	public String _hook_direct_patch_classes = "";
	public boolean _hook_spring_rest_enabled = false;
	public String _hook_boot_prefix=null;

	//Control
	public boolean control_reject_service_enabled = false;
	public int control_reject_service_max_count = 10000;
	public boolean control_reject_redirect_url_enabled = false;
	public String control_reject_text = "too many request!!";
	public String control_reject_redirect_url = "/error.html";

	// Counter
	public boolean counter_enabled = true;
	public long counter_recentuser_valid_ms = DateUtil.MILLIS_PER_FIVE_MINUTE;
	public String counter_object_registry_path = "/tmp/scouter";

	// SFA(Stack Frequency Analyzer)
	public boolean sfa_dump_enabled = false;
	public int sfa_dump_interval_ms = 10000;

	//Summary
	public boolean summary_enabled = true;
	public boolean _summary_connection_leak_fullstack_enabled = false;
	public int _summary_service_max_count = 5000;
	public int _summary_sql_max_count = 5000;
	public int _summary_api_max_count = 5000;
	public int _summary_ip_max_count = 5000;
	public int _summary_useragent_max_count = 5000;
	public int _summary_error_max_count = 500;
	public int _summary_enduser_nav_max_count = 5000;
	public int _summary_enduser_ajax_max_count = 5000;
	public int _summary_enduser_error_max_count = 5000;
	
	//EndUser
	public String enduser_trace_endpoint_url = "/_scouter_browser.jsp";

	//Experimental(ignoreset)
	public boolean __experimental = false;
	public boolean __control_connection_leak_autoclose_enabled = false;

	//internal variables
	private int objHash;
	private String objName;
	private int objHostHash;
	private String objHostName;
	private Set<String> static_contents = new HashSet<String>();
	private StringSet log_ignore_set = new StringSet();
	private String[] _hook_method_ignore_prefix = null;
	private int _hook_method_ignore_prefix_len = 0;
	private int hook_signature;
	private int enduser_perf_endpoint_hash = HashUtil.hash(enduser_trace_endpoint_url);

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
		this.profile_http_querystring_enabled = getBoolean("profile_http_querystring_enabled", false);
		this.profile_http_header_enabled = getBoolean("profile_http_header_enabled", false);
		this.profile_http_parameter_enabled = getBoolean("profile_http_parameter_enabled", false);
		this.profile_summary_mode_enabled = getBoolean("profile_summary_mode_enabled", false);
		this.xlog_lower_bound_time_ms = getInt("xlog_lower_bound_time_ms", 0);
		this.trace_service_name_header_key = getValue("trace_service_name_header_key", null);
		this.trace_service_name_get_key = getValue("trace_service_name_get_key");
		this.trace_service_name_post_key = getValue("trace_service_name_post_key");
		this.dump_dir = new File(getValue("dump_dir", "./dump"));
		try {
			this.dump_dir.mkdirs();
		} catch (Exception e) {
		}
//		this.mgr_agent_lib_dir = new File(getValue("mgr_agent_lib_dir", "./_scouter_"));
//		try {
//			this.mgr_agent_lib_dir.mkdirs();
//		} catch (Exception e) {
//		}
		this.plugin_dir = new File(getValue("plugin_dir", "./plugin"));
		
		this.autodump_enabled = getBoolean("autodump_enabled", false);
		this.autodump_trigger_active_service_cnt = getInt("autodump_trigger_active_service_cnt", 10000);
		if (this.autodump_trigger_active_service_cnt < 1) {
			this.autodump_trigger_active_service_cnt = 1;
		}
		this.autodump_level = getInt("autodump_level", 1);
		this.autodump_interval_ms = getInt("autodump_interval_ms", 30000);
		if (this.autodump_interval_ms < 5000) {
			this.autodump_interval_ms = 5000;
		}
		this.autodump_stuck_thread_ms = getInt("autodump_stuck_thread_ms", 0);
		this.mgr_static_content_extensions = getValue("mgr_static_content_extensions", "js, htm, html, gif, png, jpg, css");
		this.profile_thread_cputime_enabled = getBoolean("profile_thread_cputime_enabled", false);
		this.profile_socket_open_fullstack_enabled = getBoolean("profile_socket_open_fullstack_enabled", false);
		this.trace_background_socket_enabled = getBoolean("trace_background_socket_enabled", true);
		this.profile_socket_open_fullstack_port = getInt("profile_socket_open_fullstack_port", 0);
		this.profile_sql_escape_enabled = getBoolean("profile_sql_escape_enabled", true);
		this.profile_sqlmap_name_enabled = getBoolean("profile_sqlmap_name_enabled", true);
		this.net_udp_packet_max_bytes = getInt("net_udp_packet_max_bytes", 60000);
		this.trace_activeserivce_yellow_time = getLong("trace_activeserivce_yellow_time", 3000);
		this.trace_activeservice_red_time = getLong("trace_activeservice_red_time", 8000);
		this.mgr_log_ignore_ids = getValue("mgr_log_ignore_ids", "");
		this.log_ignore_set = getStringSet("mgr_log_ignore_ids", ",");
		this._log_udp_xlog_enabled = getBoolean("_log_udp_xlog_enabled", false);
		this._log_udp_counter_enabled = getBoolean("_log_udp_counter_enabled", false);
		this._log_udp_object_enabled = getBoolean("_log_udp_object_enabled", false);
		this.net_local_udp_ip = getValue("net_local_udp_ip");
		this.net_local_udp_port = getInt("net_local_udp_port", 0);
		this.net_collector_ip = getValue("net_collector_ip", "127.0.0.1");
		this.net_collector_udp_port = getInt("net_collector_udp_port", NetConstants.SERVER_UDP_PORT);
		this.net_collector_tcp_port = getInt("net_collector_tcp_port", NetConstants.SERVER_TCP_PORT);
		this.net_collector_tcp_session_count = getInt("net_collector_tcp_session_count", 1, 1);
		this.net_collector_tcp_connection_timeout_ms = getInt("net_collector_tcp_connection_timeout_ms", 3000);
		this.net_collector_tcp_so_timeout_ms = getInt("net_collector_tcp_so_timeout_ms", 60000);
		this.hook_signature = 0;
		this.hook_args_patterns = getValue("hook_args_patterns", "");
		this.hook_return_patterns = getValue("hook_return_patterns", "");
		this.hook_constructor_patterns = getValue("hook_constructor_patterns", "");
		this.hook_connection_open_patterns = getValue("hook_connection_open_patterns", "");

		this._log_datasource_lookup_enabled = getBoolean("_log_datasource_lookup_enabled", true);
		this.profile_connection_open_enabled = getBoolean("profile_connection_open_enabled", true);
		this._summary_connection_leak_fullstack_enabled = getBoolean("_summary_connection_leak_fullstack_enabled", false);
		this.hook_method_patterns = getValue("hook_method_patterns", "");
		this.hook_method_access_public_enabled = getBoolean("hook_method_access_public_enabled", true);
		this.hook_method_access_protected_enabled = getBoolean("hook_method_access_protected_enabled", false);
		this.hook_method_access_private_enabled = getBoolean("hook_method_access_private_enabled", false);
		this.hook_method_access_none_enabled = getBoolean("hook_method_access_none_enabled", false);
		this.hook_method_ignore_prefixes = StringUtil.removeWhitespace(getValue("hook_method_ignore_prefixes", "get,set"));
		this._hook_method_ignore_prefix = StringUtil.split(this.hook_method_ignore_prefixes, ",");
		this._hook_method_ignore_prefix_len = this._hook_method_ignore_prefix == null ? 0
				: this._hook_method_ignore_prefix.length;
		this.hook_method_ignore_classes = StringUtil.trimEmpty(StringUtil.removeWhitespace(getValue(
				"hook_method_ignore_classes", "")));
		this._hook_method_ignore_classes = new StringSet(StringUtil.tokenizer(
				this.hook_method_ignore_classes.replace('.', '/'), ","));
		this.profile_method_enabled = getBoolean("profile_method_enabled", true);
		this.hook_service_patterns = getValue("hook_service_patterns", "");
		this.hook_apicall_patterns = getValue("hook_apicall_patterns", "");
		this.hook_apicall_info_patterns = getValue("hook_apicall_info_patterns", "");
		this.hook_jsp_patterns = getValue("hook_jsp_patterns", "");
		
		this.hook_jdbc_pstmt_classes = getValue("hook_jdbc_pstmt_classes", "");
		this.hook_jdbc_stmt_classes = getValue("hook_jdbc_stmt_classes", "");
		this.hook_jdbc_rs_classes = getValue("hook_jdbc_rs_classes", "");
		this.hook_jdbc_wrapping_driver_patterns = getValue("hook_jdbc_wrapping_driver_patterns", "");
		this.hook_add_fields = getValue("hook_add_fields", "");
		this.hook_context_classes = getValue("hook_context_classes", "javax/naming/InitialContext");
		
		this.hook_signature ^= this.hook_args_patterns.hashCode();
		this.hook_signature ^= this.hook_return_patterns.hashCode();
		this.hook_signature ^= this.hook_constructor_patterns.hashCode();
		this.hook_signature ^= this.hook_connection_open_patterns.hashCode();
		this.hook_signature ^= this.hook_method_patterns.hashCode();
		this.hook_signature ^= this.hook_service_patterns.hashCode();
		this.hook_signature ^= this.hook_apicall_patterns.hashCode();
		this.hook_signature ^= this.hook_jsp_patterns.hashCode();
		this.hook_signature ^= this.hook_jdbc_wrapping_driver_patterns.hashCode();
		
		this.control_reject_service_enabled = getBoolean("control_reject_service_enabled", false);
		this.control_reject_service_max_count = getInt("control_reject_service_max_count", 10000);
		this.control_reject_redirect_url_enabled = getBoolean("control_reject_redirect_url_enabled", false);
		this.control_reject_text = getValue("control_reject_text", "too many request!!");
		this.control_reject_redirect_url = getValue("control_reject_redirect_url", "/error.html");

		this.profile_step_max_count = getInt("profile_step_max_count", 1024);
		if (this.profile_step_max_count < 100)
			this.profile_step_max_count = 100;
		this._log_background_sql = getBoolean("_log_background_sql", false);
		this.profile_fullstack_service_error_enabled = getBoolean("profile_fullstack_service_error_enabled", false);
		this.profile_fullstack_apicall_error_enabled = getBoolean("profile_fullstack_apicall_error_enabled", false);
		this.profile_fullstack_sql_error_enabled = getBoolean("profile_fullstack_sql_error_enabled", false);
		this.profile_fullstack_sql_commit_enabled = getBoolean("profile_fullstack_sql_commit_enabled", false);
		this.profile_fullstack_max_lines = getInt("profile_fullstack_max_lines", 0);
		this.net_udp_collection_interval_ms = getInt("net_udp_collection_interval_ms", 100);
		this.profile_http_parameter_url_prefix = getValue("profile_http_parameter_url_prefix", "/");
		this.profile_http_header_url_prefix = getValue("profile_http_header_url_prefix", "/");
		this.trace_http_client_ip_header_key = getValue("trace_http_client_ip_header_key", "");
		this.trace_interservice_enabled = getBoolean("trace_interservice_enabled", false);
		this.trace_response_gxid_enabled = getBoolean("trace_response_gxid_enabled", false);
		this._trace_interservice_gxid_header_key = getValue("_trace_interservice_gxid_header_key", "X-Scouter-Gxid");
		this._trace_interservice_callee_header_key = getValue("_trace_interservice_callee_header_key", "X-Scouter-Callee");
		this._trace_interservice_caller_header_key = getValue("_trace_interservice_caller_header_key", "X-Scouter-Caller");
		this.profile_connection_open_fullstack_enabled = getBoolean("profile_connection_open_fullstack_enabled", false);
		this.profile_connection_autocommit_status_enabled = getBoolean("profile_connection_autocommit_status_enabled", false);
		this.trace_user_mode = getInt("trace_user_mode", 2);
		this.trace_user_session_key = getValue("trace_user_session_key", "JSESSIONID");
		this._trace_auto_service_enabled = getBoolean("_trace_auto_service_enabled", false);
		this._trace_auto_service_backstack_enabled = getBoolean("_trace_auto_service_backstack_enabled", true);
		this.counter_enabled = getBoolean("counter_enabled", true);
		this._hook_serivce_enabled = getBoolean("_hook_serivce_enabled", true);
		this._hook_dbsql_enabled = getBoolean("_hook_dbsql_enabled", true);
		this._hook_dbconn_enabled = getBoolean("_hook_dbconn_enabled", true);
		this._hook_cap_enabled = getBoolean("_hook_cap_enabled", true);
		this._hook_methods_enabled = getBoolean("_hook_methods_enabled", true);
		this._hook_socket_enabled = getBoolean("_hook_socket_enabled", true);
		this._hook_jsp_enabled = getBoolean("_hook_jsp_enabled", true);
		this._hook_async_enabled = getBoolean("_hook_async_enabled", true);
		this.trace_db2_enabled = getBoolean("trace_db2_enabled", true);
		this._hook_usertx_enabled = getBoolean("_hook_usertx_enabled", true);
		this._hook_direct_patch_classes = getValue("_hook_direct_patch_classes", "");
		this._hook_boot_prefix = getValue("_hook_boot_prefix");
		this.counter_recentuser_valid_ms = getLong("counter_recentuser_valid_ms", DateUtil.MILLIS_PER_FIVE_MINUTE);
		this.counter_object_registry_path = getValue("counter_object_registry_path", "/tmp/scouter");
		this.sfa_dump_enabled = getBoolean("sfa_dump_enabled", false);
		this.sfa_dump_interval_ms = getInt("sfa_dump_interval_ms", 10000);
		// 웹시스템으로 부터 WAS 사이의 성능과 어떤 웹서버가 요청을 보내 왔는지를 추적하는 기능을 ON/OFF하고
		// 관련 키정보를 지정한다.
		this.trace_webserver_enabled = getBoolean("trace_webserver_enabled", false);
		this.trace_webserver_name_header_key = getValue("trace_webserver_name_header_key", "X-Forwarded-Host");
		this.trace_webserver_time_header_key = getValue("trace_webserver_time_header_key", "X-Forwarded-Time");
		// SUMMARY최대 갯수를 관리한다.
		this.summary_enabled = getBoolean("summary_enabled", true);
		this._summary_sql_max_count = getInt("_summary_sql_max_count", 5000);
		this._summary_api_max_count = getInt("_summary_api_max_count", 5000);
		this._summary_service_max_count = getInt("_summary_service_max_count", 5000);
		this._summary_ip_max_count = getInt("_summary_ip_max_count", 5000);
		this._summary_useragent_max_count = getInt("_summary_useragent_max_count", 5000);
		this._summary_error_max_count = getInt("_summary_error_max_count", 500);
		
		this._summary_enduser_nav_max_count = getInt("_summary_enduser_nav_max_count", 5000);
		this._summary_enduser_ajax_max_count = getInt("_summary_enduser_ajax_max_count", 5000);
		this._summary_enduser_error_max_count = getInt("_summary_enduser_error_max_count", 5000);

		//Experimental(ignoreset)
		this.__experimental = getBoolean("__experimental", false);
		this.__control_connection_leak_autoclose_enabled = getBoolean("_control_connection_leak_autoclose_enabled", false);

		this.alert_perm_warning_pct = getInt("alert_perm_warning_pct", 90);
		this._hook_spring_rest_enabled = getBoolean("_hook_spring_rest_enabled", false);
		this.alert_message_length = getInt("alert_message_length", 3000);
		this.alert_send_interval_ms = getInt("alert_send_interval_ms", 3000);
		this.xlog_error_jdbc_fetch_max = getInt("xlog_error_jdbc_fetch_max", 10000);
		this.xlog_error_sql_time_max_ms = getInt("xlog_error_sql_time_max_ms", 30000);
		this._log_asm_enabled = getBoolean("_log_asm_enabled", false);
		this.obj_type_inherit_to_child_enabled = getBoolean("obj_type_inherit_to_child_enabled", false);
		this._profile_fullstack_sql_connection_enabled = getBoolean("_profile_fullstack_sql_connection_enabled", false);
		this._trace_fullstack_socket_open_port = getInt("_trace_fullstack_socket_open_port", 0);
		this.log_dir = getValue("log_dir", "");
		this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
		this.log_keep_days = getInt("log_keep_days", 7);
        this._log_trace_enabled = getBoolean("_log_trace_enabled", false);
        this._log_trace_use_logger = getBoolean("_log_trace_use_logger", false);
		
		this.enduser_trace_endpoint_url = getValue("enduser_trace_endpoint_url", "_scouter_browser.jsp");
		this.enduser_perf_endpoint_hash = HashUtil.hash(this.enduser_trace_endpoint_url);
		
		this.xlog_error_check_user_transaction_enabled = getBoolean("xlog_error_check_user_transaction_enabled", true);
			
		resetObjInfo();
		setStaticContents();
	}

	public int getObjHash() {
		return this.objHash;
	}

	public String getObjName() {
		return this.objName;
	}

	public int getObjHostHash(){
		return this.objHostHash;
	}

	public String getObjHostName() {
		return this.objHostName;
	}

	public int getEndUserPerfEndpointHash() {
		return this.enduser_perf_endpoint_hash;
	}

	public boolean isIgnoreLog(String id) {
		return log_ignore_set.hasKey(id);
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
		String[] s = StringUtil.split(this.mgr_static_content_extensions, ',');
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
		this.obj_type = getValue("obj_type", detected);
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
		this.obj_host_type = getValue("obj_host_type", detected);
		this.obj_host_name = getValue("obj_host_name", SysJMX.getHostName());
		this.objHostName = "/" + this.obj_host_name;
		this.objHostHash = HashUtil.hash(objHostName);
		this.obj_name_auto_pid_enabled = getBoolean("obj_name_auto_pid_enabled", false);
		String defaultName;
		if (this.obj_name_auto_pid_enabled == true) {
			defaultName = "" + SysJMX.getProcessPID();
		} else {
			defaultName = this.obj_type + "1";
		}
		this.obj_name = getValue("obj_name", System.getProperty("jvmRoute", defaultName));
		this.objName = objHostName + "/" + this.obj_name;
		this.objHash = HashUtil.hash(objName);
		System.setProperty("scouter.objname", this.objName);
		System.setProperty("scouter.objtype", this.obj_type);
		System.setProperty("scouter.dir", new File(".").getAbsolutePath());
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
			if (file.getParentFile().exists() == false) {
				file.getParentFile().mkdirs();
			}
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
		ignoreSet.add("__experimental");
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
		StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(new Configure(true));
		StringEnumer enu = defMap.keys();
		while (enu.hasMoreElements()) {
			String key = enu.nextString();
			if (ignoreSet.contains(key))
				continue;
			System.out.println(key + " : " + ConfigValueUtil.toValue(defMap.get(key)));
		}
	}
}
