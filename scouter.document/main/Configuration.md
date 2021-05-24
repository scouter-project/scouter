# Configuration
[![English](https://img.shields.io/badge/language-English-orange.svg)](Configuration.md) [![Korean](https://img.shields.io/badge/language-Korean-blue.svg)](Configuration_kr.md)

## Server options
 * **Useful options of collector server**
   (It's not every option. you can get whole options on the source code that the file name is Configure.java.)
  * you can use the variable name as the property name you want to set in configuration file.
   ```(aka in the file scouter.conf by default)```

   * example
   ```properties
   db_dir = /user/home/scouter/server/database
   log_dir = /user/home/scouter/server/logs
   net_udp_listen_port = 6100
   net_tcp_listen_port = 6100
   ```
 
```java
	@ConfigDesc("Logging TCP connection related event")
	public boolean log_tcp_action_enabled = false;
	@ConfigDesc("Logging incoming MultiPacket")
	public boolean log_udp_multipacket = false;
	@ConfigDesc("Logging expired MultiPacket")
	public boolean log_expired_multipacket = true;
	@ConfigDesc("Logging all incoming packs")
	public boolean log_udp_packet = false;
	@ConfigDesc("Logging incoming CounterPack")
	public boolean log_udp_counter = false;
	@ConfigDesc("Logging incoming PerfInteractionCounterPack")
	public boolean log_udp_interaction_counter = false;
	@ConfigDesc("Logging incoming XLogPack")
	public boolean log_udp_xlog = false;
	@ConfigDesc("Logging incoming ProfilePack")
	public boolean log_udp_profile = false;
	@ConfigDesc("Logging incoming TextPack")
	public boolean log_udp_text = false;
	@ConfigDesc("Logging incoming AlertPack")
	public boolean log_udp_alert = false;
	@ConfigDesc("Logging incoming ObjectPack")
	public boolean log_udp_object = false;
	@ConfigDesc("Logging incoming StatusPack")
	public boolean log_udp_status = false;
	@ConfigDesc("Logging incoming StackPack")
	public boolean log_udp_stack = false;
	@ConfigDesc("Logging incoming SummaryPack")
	public boolean log_udp_summary = false;
	@ConfigDesc("Logging incoming BatchPack")
	public boolean log_udp_batch = false;	
	@ConfigDesc("Logging all request handlers in starting")	
	public boolean log_service_handler_list = false;
	@ConfigDesc("Logging incoming SpanPack")
	public boolean log_udp_span = false;

	@ConfigDesc("Logging when index traversal is too heavy.")
	public int log_index_traversal_warning_count = 100;

	@ConfigDesc("Retaining log according to date")
	public boolean log_rotation_enabled = true;
	@ConfigDesc("Keeping period of log")
	public int log_keep_days = 31;
	@ConfigDesc("Logging sql failed to parse")
	public boolean log_sql_parsing_fail_enabled = false;
	@ConfigDesc("")
	public boolean _trace = false;

	//Network
	@ConfigDesc("UDP Host")
	public String net_udp_listen_ip = "0.0.0.0";
	@ConfigDesc("UDP Port")
	public int net_udp_listen_port = NetConstants.SERVER_UDP_PORT;
	@ConfigDesc("TCP Host")
	public String net_tcp_listen_ip = "0.0.0.0";
	@ConfigDesc("TCP Port")
	public int net_tcp_listen_port = NetConstants.SERVER_TCP_PORT;
	@ConfigDesc("Client Socket Timeout(ms)")
	public int net_tcp_client_so_timeout_ms = 8000;
	@ConfigDesc("Agent Socket Timeout(ms)")
	public int net_tcp_agent_so_timeout_ms = 60000;
	@ConfigDesc("Transfer period(ms) of KEEP_ALIVE")
	public int net_tcp_agent_keepalive_interval_ms = 5000;
	@ConfigDesc("Waiting time(ms) for agent session")
	public int net_tcp_get_agent_connection_wait_ms = 1000;
	@ConfigDesc("UDP Packet Buffer Size")
	public int net_udp_packet_buffer_size = 65535;
	@ConfigDesc("UDP Receiver Buffer Size")
	public int net_udp_so_rcvbuf_size = 1024 * 1024 * 4;
	@ConfigDesc("")
	public int _net_udp_worker_thread_count = 3;
	@ConfigDesc("TCP Thread Pool Size")
	public int net_tcp_service_pool_size = 100;
	@ConfigDesc("Activating Http Server")
	public boolean net_http_server_enabled = false;
	@ConfigDesc("Http Port")
	public int net_http_port = NetConstants.SERVER_HTTP_PORT;
	@ConfigDesc("user extension web root")
	public String net_http_extweb_dir = "./extweb";
	@ConfigDesc("Activating Scouter API")
	public boolean net_http_api_enabled = false;
	@ConfigDesc("Enable a swagger for HTTP API.")
	public boolean net_http_api_swagger_enabled = false;
	@ConfigDesc("Swagger option of host's ip or domain to call APIs.")
	public String net_http_api_swagger_host_ip = "";
	@ConfigDesc("API CORS support for Access-Control-Allow-Origin")
	public String net_http_api_cors_allow_origin = "*";
	@ConfigDesc("Access-Control-Allow-Credentials")
	public String net_http_api_cors_allow_credentials = "true";

	@ConfigDesc("size of webapp connection pool to collector")
	public int net_webapp_tcp_client_pool_size = 30;
	@ConfigDesc("timeout of web app connection pool to collector")
	public int net_webapp_tcp_client_pool_timeout = 60000;
	@ConfigDesc("So timeout of web app to collector")
	public int net_webapp_tcp_client_so_timeout = 30000;

	@ConfigDesc("Enable api access control by client ip")
	public boolean net_http_api_auth_ip_enabled = false;
	@ConfigDesc("If get api caller's ip from http header.")
	public String net_http_api_auth_ip_header_key;

	@ConfigDesc("Enable api access control by JSESSIONID of Cookie")
	public boolean net_http_api_auth_session_enabled = false;
	@ConfigDesc("api http session timeout")
	public int net_http_api_session_timeout = 3600*24;
	@ConfigDesc("Enable api access control by Bearer token(of Authorization http header) - get access token from /user/loginGetToken.")
	public boolean net_http_api_auth_bearer_token_enabled = false;
	@ConfigDesc("Enable gzip response on api call")
	public boolean net_http_api_gzip_enabled = true;

	@ConfigDesc("api access allow ip addresses")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String net_http_api_allow_ips = "localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1";
	public Set<String> allowIpExact;
	public List<StrMatch> allowIpMatch;

	//Dir
	@ConfigDesc("Store directory of database")
	public String db_dir = "./database";
	@ConfigDesc("Path to log directory")
	public String log_dir = "./logs";
	@ConfigDesc("Path to plugin directory")
	public String plugin_dir = "./plugin";
	@ConfigDesc("Path to client related directory")
	public String client_dir = "./client";
	@ConfigDesc("temp dir")
	public String temp_dir = "./tempdata";

	//Object
	@ConfigDesc("Waiting time(ms) until stopped heartbeat of object is determined to be inactive")
	public int object_deadtime_ms = 8000;
	@ConfigDesc("inactive object warning level. default 0.(0:info, 1:warn, 2:error, 3:fatal)")
	public int object_inactive_alert_level = 0;

	@ConfigDesc("Zipkin Waiting time(ms) until stopped heartbeat of object is determined to be inactive")
	public int object_zipkin_deadtime_ms = 180 * 1000;

	//Compress
	@ConfigDesc("Activating XLog data in zip file")
	public boolean compress_xlog_enabled = false;
	@ConfigDesc("Activating profile data in zip file")
	public boolean compress_profile_enabled = false;
	@ConfigDesc("")
	public int _compress_write_buffer_block_count = 3;
	@ConfigDesc("")
	public int _compress_read_cache_block_count = 3;
	@ConfigDesc("")
	public long _compress_read_cache_expired_ms = DateUtil.MILLIS_PER_MINUTE;
	@ConfigDesc("")
	public int _compress_dailycount_header_cache_size = 3;
	@ConfigDesc("")
	public int _compress_write_thread = 2;

	//Auto
	@ConfigDesc("")
	public boolean _auto_5m_sampling = true;

	//Manager
	@ConfigDesc("Activating automatic deletion function in the database")
	public boolean mgr_purge_enabled = true;
	@ConfigDesc("Condition of disk usage for automatic deletion. if lack, delete profile data first exclude today data.")
	public int mgr_purge_disk_usage_pct = 80;

	@ConfigDesc("Retaining date for automatic deletion. delete profile data first.")
	public int mgr_purge_profile_keep_days = 10;
	@Deprecated
	@ConfigDesc("Deprecated : use mgr_purge_profile_keep_days")
	public int mgr_purge_keep_days = mgr_purge_profile_keep_days;

	@ConfigDesc("Retaining date for automatic deletion.")
	public int mgr_purge_xlog_keep_days = 30;
	@Deprecated
	@ConfigDesc("Deprecated : use mgr_purge_xlog_keep_days")
	public int mgr_purge_xlog_without_profile_keep_days = mgr_purge_xlog_keep_days;

	@ConfigDesc("Retaining date for automatic deletion")
	public int mgr_purge_counter_keep_days = 70;

	@ConfigDesc("Retaining date for automatic deletion. realtime-counter only.")
	public int mgr_purge_realtime_counter_keep_days = mgr_purge_counter_keep_days;
	@ConfigDesc("Retaining date for automatic deletion. tag-counter only.")
	public int mgr_purge_tag_counter_keep_days = mgr_purge_counter_keep_days;
	@ConfigDesc("Retaining date for automatic deletion. visitor-counter only")
	public int mgr_purge_visitor_counter_keep_days = mgr_purge_counter_keep_days;

	@ConfigDesc("Retaining date for automatic deletion. daily text dictionary only")
	public int mgr_purge_daily_text_days = Math.max(mgr_purge_tag_counter_keep_days * 2, mgr_purge_xlog_keep_days * 2);

	@ConfigDesc("Retaining date for automatic deletion. summary(stat) data only")
	public int mgr_purge_sum_data_days = 60;

	@ConfigDesc("Ignored log ID set")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public StringSet mgr_log_ignore_ids = new StringSet();

	//db
	@ConfigDesc("true for daily dictionary mode about service name. default value is false that means it's permanent.")
	public boolean mgr_text_db_daily_service_enabled = false;
	@ConfigDesc("true for daily dictionary mode about api name. default value is false that means it's permanent.")
	public boolean mgr_text_db_daily_api_enabled = false;
	@ConfigDesc("true for daily dictionary mode about user agent. default value is false that means it's permanent.")
	public boolean mgr_text_db_daily_ua_enabled = false;

	@ConfigDesc("change default memory size of hash index.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_default_mb = 1;
	@ConfigDesc("change memory size of hash index for service text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_service_mb = 1;
	@ConfigDesc("change memory size of hash index for apicall text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_api_mb = 1;
	@ConfigDesc("change memory size of hash index for user agent text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_ua_mb = 1;
	@ConfigDesc("change memory size of hash index for login text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_login_mb = 1;
	@ConfigDesc("change memory size of hash index for desc text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_desc_mb = 1;
	@ConfigDesc("change memory size of hash index for hashed message text.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_index_hmsg_mb = 1;
	@ConfigDesc("change memory size of hash index for daily text db.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_text_db_daily_index_mb = 1;

	@ConfigDesc("change default memory size of key value store index.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_kv_store_index_default_mb = 8;

	@ConfigDesc("change default memory size of xlog txid/gxid index.(MB)" +
			"[warn] modified this will break the database files.\nbackup old database files before change values.(restart required)")
	public int _mgr_xlog_id_index_mb = 1;

	//external-link
	@ConfigDesc("name of 3rd party ui")
	public String ext_link_name = "scouter-paper";
	@ConfigDesc("outgoing link pattern for a 3rd party UI.(client restart required)\n" +
			"Context menu in any chart shows the menu 'Open with 3rd-party UI.'\n" +
			"* variable patterns : \n" +
			"   $[objHashes] : comma separated objHash values\n" +
			"   $[objType] : object type\n" +
			"   $[from] : start time in chart by millis\n" +
			"   $[to] : end time in chart by millis")
	public String ext_link_url_pattern = "http://my-scouter-paper-ip:6188/index.html#/paper?&address=localhost&port=6188&realtime=false&xlogElapsedTime=8000&instances=$[objHashes]&from=$[from]&to=$[to]&layout=my-layout-template-01";

	//Span
	@ConfigDesc("Span Queue Size")
	public int span_queue_size = 1000;

	//XLog
	@ConfigDesc("XLog Writer Queue Size")
	public int xlog_queue_size = 10000;
	@ConfigDesc("Ignored time(ms) in retrieving XLog in real time")
	public int xlog_realtime_lower_bound_ms = 0;
	@ConfigDesc("Ignored time(ms) in retrieving previous XLog")
	public int xlog_pasttime_lower_bound_ms = 0;
//	@ConfigDesc("Ignored profile time(ms) without saving")
//	public int xlog_profile_save_lower_bound_ms = 0;

	//Profile
	@ConfigDesc("Profile Writer Queue Size")
	public int profile_queue_size = 1000;

	@ConfigDesc("gxid keeping count in memory for XLog consequent sampling")
	public int xlog_sampling_matcher_gxid_keep_memory_count = 500000;
	@ConfigDesc("xlog keeping count in memory for XLog consequent sampling")
	public int xlog_sampling_matcher_xlog_keep_memory_count = 100000;
	@ConfigDesc("max keeping millis of xlog for XLog consequent sampling")
	public int xlog_sampling_matcher_xlog_keep_memory_millis = 5000;

	@ConfigDesc("profile keeping count (in one bucket, 500ms) in memory for XLog consequent sampling")
	public int xlog_sampling_matcher_profile_keep_memory_count = 5000;
	@ConfigDesc("max keeping seconds of profile for XLog consequent sampling")
	public int xlog_sampling_matcher_profile_keep_memory_secs = 5;

	//GeoIP
	@ConfigDesc("Activating IP-based city/country extraction")
	public boolean geoip_enabled = true;
	@ConfigDesc("Path to GeoIP data file")
	public String geoip_data_city_file = CONF_DIR + "GeoLiteCity.dat";

	//SQL
	@ConfigDesc("Activating table-based SQL compression")
	public boolean sql_table_parsing_enabled = true;

	//TagCount
	@ConfigDesc("Activating TagCount function")
	public boolean tagcnt_enabled = true;

	//Service request options from client
	@ConfigDesc("search xlog service option - max xlog count to search per request")
	public int req_search_xlog_max_count = 500;

	@ConfigDesc("Path to telegraf config xml file")
	public String input_telegraf_config_file = CONF_DIR + "scouter-telegraf.xml";

    @ConfigDesc("Deprecated use the telegraf config view instead. This value may be ignored.")
	public boolean input_telegraf_enabled = true;
    @ConfigDesc("Deprecated use the telegraf config view instead. This value may be ignored.")
	public boolean input_telegraf_debug_enabled = false;
	@ConfigDesc("Deprecated use the telegraf config view instead. This value may be ignored.")
	public boolean input_telegraf_delta_counter_normalize_default = true;
	@ConfigDesc("Deprecated use the telegraf config view instead. This value may be ignored.")
	public int input_telegraf_delta_counter_normalize_default_seconds = 30;
	@ConfigDesc("Deprecated use the telegraf config view instead. This value may be ignored.")
	public int telegraf_object_deadtime_ms = 35000;

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"Telegraf http input of the $measurement$ enabled.\n" +
			"$measurement$ is a variable to the measurement name of the line protocol.\n" +
			"eg) input_telegraf_$redis_keyspace$_enabled=true")
	public boolean input_telegraf_$measurement$_enabled = true;

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"print telegraf line protocol of the $measurement$ to STDOUT")
	public boolean input_telegraf_$measurement$_debug_enabled = false;

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"If set, only the metric matching to this tag value is handled.\n" +
			"It can have multiple values. comma separator means 'or' condition. eg) cpu:cpu-total,cpu:cpu0\n" +
			"It also have not(!) condition. eg) cpu:!cpu-total")
    @ConfigValueType(value = ValueType.COMMA_COLON_SEPARATED_VALUE,
            strings = {"filtering tag name(reqiured)", "filtering tag value(reqiured)"},
            booleans = {true, true}
    )
	public String input_telegraf_$measurement$_tag_filter = "";

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"which fields of $measurement$ are mapped to scouter's counter.\n" +
			"format: {line-protocol field name}:{scouter counter name}:{display name?}:{unit?}:{hasTotal?}:{normalize sec?}\n" +
			"It can have multiple values.\n" +
			" - {scouter counter name} can be defined in combination with the line protocol's tag variables.\n" +
			"For example, if the value of 'tag1' is 'disk01' and the value of 'tag2' is 'bin', the counter name defined as 'scouter-du-$tag1$-$tag2$' is 'scouter-du-disk01-bin'.\n" +
			" eg)used_memory:tg-redis-used-memory,used_memory_rss:redis-used-memory-rss,redis used rss,bytes:true\n" +
			" eg)cpu:cpu-$cpu-no$ -- this example shows counter definition with tag value.\n" +
			"If {line-protocol field name} is started with '&' or '&&', then It works as delta counter\n" +
			"When specified as a delta type, the difference in values per second is stored. and the counter name ends with '_delta'\n" +
			"double '&&' means BOTH type. AS BOTH type, the value and the difference value both are stored.\n" +
			" - {normalize sec} applies only to a delta counter if the counter is a 'BOTH' type counter. (This value can have min 4 to max 60)")
	@ConfigValueType(value = ValueType.COMMA_COLON_SEPARATED_VALUE,
			strings = {"line protocol field\n(reqiured)", "mapping counter name\n(reqiured)", "display name", "unit", "totalizable\ndefault true", "norm. sec.\ndefault 30"},
			booleans = {true, true, false, false, false, false}
			)
	public String input_telegraf_$measurement$_counter_mappings = "";

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"define an obj Family prefix. objectType is defined with some tags.\n" +
			"see input_telegraf_$measurement$_objFamily_append_tags option.")
	public String input_telegraf_$measurement$_objFamily_base = "";

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"this tags's value is appended to objFamily_base.\nIt can have multiple values. eg)tag1,tag2")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String input_telegraf_$measurement$_objFamily_append_tags = "";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"define an objectType prefix. objectType is defined with some tags.\n" +
			"see input_telegraf_$measurement$_objType_prepend(or append)_tags option.")
	public String input_telegraf_$measurement$_objType_base = "";

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"this tags's value is prepended to objType_base.\nIt can have multiple values. eg)tag1,tag2")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String input_telegraf_$measurement$_objType_prepend_tags = "scouter_obj_type_prefix";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"this tags's value is appended to objType_base.\nIt can have multiple values. eg)tag1,tag2")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String input_telegraf_$measurement$_objType_append_tags = "";

	@ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"this tags's value is object type's icon file name that the scouter client have. eg)redis")
	public String input_telegraf_$measurement$_objType_icon = "";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"define an objectName prefix. objectName is defined with some tags.\n" +
			"see input_telegraf_$measurement$_objName_append_tags option.")
	public String input_telegraf_$measurement$_objName_base = "";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"this tags's value is appended to objName_base.\n" +
			"It can have multiple values. eg)tag1,tag2")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String input_telegraf_$measurement$_objName_append_tags = "";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"tag name to define host")
	public String input_telegraf_$measurement$_host_tag = "host";

    @ConfigDesc("[This option is just a sample. Change $measurement$ to your measurement name like $cpu$.]\n" +
			"which host value defined with $measurement$_host_tag option is mapped to scouter's host.\n" +
			"It can have multiple values. eg)hostValue1:scouterHost1,hostValue2:scouterHost2")
	@ConfigValueType(value = ValueType.COMMA_COLON_SEPARATED_VALUE,
			strings = {"telegraf host name(reqiured)", "scouter host name(reqiured)"},
			booleans = {true, true}
	)
	public String input_telegraf_$measurement$_host_mappings = "";

	//Visitor Hourly
	public boolean visitor_hourly_count_enabled = true;
```


## Java agent options

 * **Useful options of java agent**
   (It's not every option. you can get whole options on the source code that the file name is Configure.java.)
  * you can use the variable name as the property name you want to set in configuration file.
   ```(aka in the file scouter.conf by default)```

   * example
   ```properties
   net_colector_ip = 127.0.0.1
   _hook_spring_rest_enabled = true
   trace_interservice_enabled
   ```
   
```java
    //Network
    @ConfigDesc("UDP local IP")
    public String net_local_udp_ip = null;
    @ConfigDesc("UDP local Port")
    public int net_local_udp_port;
    @ConfigDesc("Collector IP")
    public String net_collector_ip = "127.0.0.1";
    @ConfigDesc("Collector UDP Port")
    public int net_collector_udp_port = NetConstants.SERVER_UDP_PORT;
    @ConfigDesc("Collector TCP Port")
    public int net_collector_tcp_port = NetConstants.SERVER_TCP_PORT;
    @ConfigDesc("Collector TCP Session Count")
    public int net_collector_tcp_session_count = 1;
    @ConfigDesc("Collector TCP Socket Timeout(ms)")
    public int net_collector_tcp_so_timeout_ms = 60000;
    @ConfigDesc("Collector TCP Connection Timeout(ms)")
    public int net_collector_tcp_connection_timeout_ms = 3000;
    @ConfigDesc("UDP Buffer Size")
    public int net_udp_packet_max_bytes = 60000;
    @ConfigDesc("UDP Collection Interval(ms)")
    public long net_udp_collection_interval_ms = 100;

    //Object
    @ConfigDesc("Deprecated. It's just an alias of monitoring_group_type which overrides this value.")
    public String obj_type = "";
    @ConfigDesc("monitoring group type, commonly named as system name and a monitoring type.\neg) ORDER-JVM, WAREHOUSE-LINUX ...")
    public String monitoring_group_type = "";
    @ConfigDesc("Object Name")
    public String obj_name = "";
    @ConfigDesc("Host Type")
    public String obj_host_type = "";
    @ConfigDesc("Host Name")
    public String obj_host_name = "";
    @ConfigDesc("Activating for using object name as PID")
    public boolean obj_name_auto_pid_enabled = false;
    @ConfigDesc("Redefining DS, RP type according to main object")
    public boolean obj_type_inherit_to_child_enabled = false;
    @ConfigDesc("Activating collect sub counters using JMX")
    public boolean jmx_counter_enabled = false;

    //profile
    @ConfigDesc("Http Query String profile")
    public boolean profile_http_querystring_enabled;
    @ConfigDesc("Http Header profile")
    public boolean profile_http_header_enabled;
    @ConfigDesc("Service URL prefix for Http header profile")
    public String profile_http_header_url_prefix = "/";

    @ConfigDesc("http header names for profiling with comma separator")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String profile_http_header_keys = "";

    @ConfigDesc("Http Parameter profile")
    public boolean profile_http_parameter_enabled;
    @ConfigDesc("Service URL prefix for Http parameter profile")
    public String profile_http_parameter_url_prefix = "/";
    @ConfigDesc("spring controller method parameter profile")
    public boolean profile_spring_controller_method_parameter_enabled = true;

//    @Deprecated
//    @ConfigDesc("Activating profile summary function")
//    public boolean profile_summary_mode_enabled = false;

    @ConfigDesc("Profiling the memory usage of each method")
    public boolean profile_thread_cputime_enabled = false;
    @ConfigDesc("Profiling the memory usage of each service")
    public boolean profile_thread_memory_usage_enabled = true;
    @ConfigDesc("ThreadStack profile for open socket")
    public boolean profile_socket_open_fullstack_enabled = false;
    @ConfigDesc("ThreadStack profile for a certain port of open socket")
    public int profile_socket_open_fullstack_port = 0;
    @ConfigDesc("SQL Map profile")
    public boolean profile_sqlmap_name_enabled = true;
    @ConfigDesc("DBConnection profile")
    public boolean profile_connection_open_enabled = true;
    @ConfigDesc("Activating stack information profile in opening DB connection")
    public boolean profile_connection_open_fullstack_enabled = false;
    @ConfigDesc("AutoCommit profile")
    public boolean profile_connection_autocommit_status_enabled = false;
    @ConfigDesc("Method profile")
    public boolean profile_method_enabled = true;
    @ConfigDesc("Profile Buffer Size")
    public int profile_step_max_count = 1024;
    @ConfigDesc("Profile Buffer Size")
    public int profile_step_max_keep_in_memory_count = 2048;
    @ConfigDesc("Stack profile in occurrence of service error")
    public boolean profile_fullstack_service_error_enabled = false;
    @ConfigDesc("Stack profile in occurrence of apicall error")
    public boolean profile_fullstack_apicall_error_enabled = false;
    @ConfigDesc("Stack profile in occurrence of sql error")
    public boolean profile_fullstack_sql_error_enabled = false;
    @ConfigDesc("Stack profile in occurrence of commit error")
    public boolean profile_fullstack_sql_commit_enabled = false;
    @ConfigDesc("Stack profile in occurrence of sql error")
    public boolean profile_fullstack_hooked_exception_enabled = false;

    @ConfigDesc("Stack profile in occurrence of redis error")
    public boolean profile_fullstack_redis_error_enabled = false;
    @ConfigDesc("make unknown redis key stringify by force. (using new String(byte[])")
    public boolean profile_redis_key_forcibly_stringify_enabled = false;

    @ConfigDesc("Number of stack profile lines in occurrence of error")
    public int profile_fullstack_max_lines = 0;

    @ConfigDesc("Escaping literal parameters for normalizing the query")
    public boolean profile_sql_escape_enabled = true;
    @ConfigDesc("")
    public boolean _profile_fullstack_sql_connection_enabled = false;
    @ConfigDesc("")
    public boolean _profile_fullstack_sql_execute_debug_enabled = false;
    @ConfigDesc("")
    public boolean profile_fullstack_rs_leak_enabled = false;
    @ConfigDesc("")
    public boolean profile_fullstack_stmt_leak_enabled = false;

    @ConfigDesc("Profile elastic search full query.\nIt need more payload and disk usage.")
    public boolean profile_elasticsearch_full_query_enabled = false;

    @ConfigDesc("profile reactor's important checkpoint")
    public boolean profile_reactor_checkpoint_enabled = true;
    @ConfigDesc("profile reactor's another checkpoints")
    public boolean profile_reactor_more_checkpoint_enabled = false;

    //Trace
    @ConfigDesc("User ID based(0 : Remote IP Address, 1 : Cookie(JSESSIONID), 2 : Cookie(SCOUTER), 3 : Header) \n - able to set value for 1.Cookie and 3.Header \n - refer to 'trace_user_session_key'")
    public int trace_user_mode = 2; // 0:Remote IP, 1:JSessionID, 2:Scouter Cookie, 3:Header
    @ConfigDesc("Setting a cookie expired time for SCOUTER cookie when trace_user_mode is 2")
    public int trace_scouter_cookie_max_age = Integer.MAX_VALUE;
    @ConfigDesc("Setting a cookie path for SCOUTER cookie when trace_user_mode is 2")
    public String trace_user_cookie_path = "/";

    @ConfigDesc("Tracing background thread socket")
    public boolean trace_background_socket_enabled = true;
    @ConfigDesc("Adding assigned header value to the service name")
    public String trace_service_name_header_key;
    @ConfigDesc("Adding assigned get parameter to the service name")
    public String trace_service_name_get_key;
    @ConfigDesc("Adding assigned post parameter to the service name")
    public String trace_service_name_post_key;
    @ConfigDesc("Active Thread Warning Time(ms)")
    public long trace_activeserivce_yellow_time = 3000;
    @ConfigDesc("Active Thread Fatal Time(ms)")
    public long trace_activeservice_red_time = 8000;
    @ConfigDesc("Identifying header key of Remote IP")
    public String trace_http_client_ip_header_key = "";
    @ConfigDesc("Activating gxid connection in HttpTransfer")
    public boolean trace_interservice_enabled = true;
    @ConfigDesc("")
    public String _trace_interservice_gxid_header_key = "X-Scouter-Gxid";
    @ConfigDesc("")
    public boolean trace_response_gxid_enabled = false;
    @ConfigDesc("")
    public String _trace_interservice_callee_header_key = "X-Scouter-Callee";
    @ConfigDesc("")
    public String _trace_interservice_caller_header_key = "X-Scouter-Caller";
    @ConfigDesc("")
    public String _trace_interservice_caller_obj_header_key = "X-Scouter-Caller-Obj";
    @ConfigDesc("")
    public String _trace_interservice_callee_obj_header_key = "X-Scouter-Callee-Obj";
    @ConfigDesc("JSession key for user ID")
    public String trace_user_session_key = "JSESSIONID";
    @ConfigDesc("")
    public boolean _trace_auto_service_enabled = false;
    @ConfigDesc("")
    public boolean _trace_auto_service_backstack_enabled = true;
    @ConfigDesc("Activating trace DB2")
    public boolean trace_db2_enabled = true;

    @Deprecated
    @ConfigDesc("Deprecated!")
    public boolean trace_webserver_enabled = false;
    @Deprecated
    @ConfigDesc("Deprecated!")
    public String trace_webserver_name_header_key = "X-Forwarded-Host";
    @Deprecated
    @ConfigDesc("Deprecated!")
    public String trace_webserver_time_header_key = "X-Forwarded-Time";

    @ConfigDesc("measure queuing time from load balancer, reverse proxy, web server...\n if set, you can open Queuing Time view.")
    public boolean trace_request_queuing_enabled = false;
    @ConfigDesc("the name of server that set request start time")
    public String trace_request_queuing_start_host_header = "X-Request-Start-Host";
    @ConfigDesc("set request start time.\n - time format : t=microsecond (or) ts=second.milli")
    public String trace_request_queuing_start_time_header = "X-Request-Start-Time";

    @ConfigDesc("the name of server that set the trace_request_queuing_start_2nd_time_header")
    public String trace_request_queuing_start_2nd_host_header = "X-Request-Start-2nd-Host";
    @ConfigDesc("set request passing time measured by 2nd layered server.\n - time format : t=microsecond (or) ts=second.milli")
    public String trace_request_queuing_start_2nd_time_header = "X-Request-Start-2nd-Time";

    @ConfigDesc("")
    public int _trace_fullstack_socket_open_port = 0;
    @ConfigDesc("")
    public int _trace_sql_parameter_max_count = 128;
    @ConfigDesc("max length of bound sql parameter on profile view(< 500)")
    public int trace_sql_parameter_max_length = 20;
    @ConfigDesc("")
    public String trace_delayed_service_mgr_filename = "setting_delayed_service.properties";
    @ConfigDesc("")
    public boolean trace_rs_leak_enabled = false;
    @ConfigDesc("")
    public boolean trace_stmt_leak_enabled = false;

    //Dir
    @ConfigDesc("Plugin directory")
    public File plugin_dir = new File(agent_dir_path + "/plugin");
    @ConfigDesc("Dump directory")
    public File dump_dir = new File(agent_dir_path + "/dump");
    //public File mgr_agent_lib_dir = new File("./_scouter_");

    //Manager
    @ConfigDesc("")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String mgr_static_content_extensions = "js, htm, html, gif, png, jpg, css";
    @ConfigDesc("")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String mgr_log_ignore_ids = "";

    //Auto dump options when active service is exceed the set threshold count.
    @ConfigDesc("Activating auto dump - append dumps onto the dump file in dump directory.")
    public boolean autodump_enabled = false;
    @ConfigDesc("Auto dump trigger point (dump when exceeding this active service count)")
    public int autodump_trigger_active_service_cnt = 10000;
    @ConfigDesc("Minimum interval(ms) for operating auto dump function - hard min : 5000")
    public long autodump_interval_ms = 30000;
    @ConfigDesc("Auto dump level (1 : ThreadDump, 2 : active service, 3 : thread list)")
    public int autodump_level = 1; // 1:ThreadDump, 2:ActiveService, 3:ThreadList

    //Auto dump options about the thread on stuck
    @ConfigDesc("Dump when a thread are running over this time - 0 is disabled")
    public int autodump_stuck_thread_ms = 0;
    @ConfigDesc("")
    public int autodump_stuck_check_interval_ms = 10000;

    //Auto dump options on exceeded process cpu
    @ConfigDesc("Enable the function to generate dump file when this process cpu is over than the set threshold")
    public boolean autodump_cpu_exceeded_enabled = false;
    @ConfigDesc("Threshold of cpu to generate dump file")
    public int autodump_cpu_exceeded_threshold_pct = 90;
    @ConfigDesc("Threshold of over-cpu-threshold duration")
    public int autodump_cpu_exceeded_duration_ms = 30000;
    @ConfigDesc("Dump file generation interval")
    public int autodump_cpu_exceeded_dump_interval_ms = 3000;
    @ConfigDesc("value of how many dump is generated.")
    public int autodump_cpu_exceeded_dump_cnt = 3;

    //XLog
    @Deprecated
    @ConfigDesc("(deprecated) XLog Ignore Time\n - for backward compatibility. Use xlog_sampling_xxx options instead")
    public int xlog_lower_bound_time_ms = 0;

    //XLog error marking
    @ConfigDesc("Leave an error message at XLog in case of over fetching. (fetch count)")
    public int xlog_error_jdbc_fetch_max = 10000;
    @ConfigDesc("Leave an error message at XLog in case of over timing query. (ms)")
    public int xlog_error_sql_time_max_ms = 30000;
    @ConfigDesc("Leave an error message at XLog when UserTransaction's begin/end unpaired")
    public boolean xlog_error_check_user_transaction_enabled = true;
    @ConfigDesc("mark as error on xlog flag if SqlException is occured.")
    public boolean xlog_error_on_sqlexception_enabled = true;
    @ConfigDesc("mark as error on xlog flag if Api call errors are occured.")
    public boolean xlog_error_on_apicall_exception_enabled = true;
    @ConfigDesc("mark as error on xlog flag if redis error is occured.")
    public boolean xlog_error_on_redis_exception_enabled = true;
    @ConfigDesc("mark as error on xlog flag if elasticsearc error is occured.")
    public boolean xlog_error_on_elasticsearch_exception_enabled = true;
    @ConfigDesc("mark as error on xlog flag if mongodb error is occured.")
    public boolean xlog_error_on_mongodb_exception_enabled = true;

    //XLog hard sampling options
    @ConfigDesc("XLog hard sampling mode enabled\n - for the best performance but it affects all statistics data")
    public boolean _xlog_hard_sampling_enabled = false;
    @ConfigDesc("XLog hard sampling rate(%) - discard data over the percentage")
    public int _xlog_hard_sampling_rate_pct = 10;

    //XLog soft sampling options
    @ConfigDesc("XLog sampling - ignore global consequent sampling. the commencement service's sampling option affects it's children.")
    public boolean ignore_global_consequent_sampling = false;
    @ConfigDesc("XLog sampling - The service of this patterns can be unsampled by the sampling rate even if parent call is sampled and on tracing.")
    public String xlog_consequent_sampling_ignore_patterns= "";

    @ConfigDesc("XLog sampling exclude patterns.")
    public String xlog_sampling_exclude_patterns = "";

    @ConfigDesc("XLog sampling mode enabled")
    public boolean xlog_sampling_enabled = false;
    @ConfigDesc("XLog sampling but discard profile only not XLog.")
    public boolean xlog_sampling_only_profile = false;
    @ConfigDesc("XLog sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_sampling_step1_ms = 100;
    @ConfigDesc("XLog sampling step1 percentage(%)")
    public int xlog_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_sampling_step2_ms = 1000;
    @ConfigDesc("XLog sampling step2 percentage(%)")
    public int xlog_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_sampling_step3_ms = 3000;
    @ConfigDesc("XLog sampling step3 percentage(%)")
    public int xlog_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog sampling over step3 percentage(%)")
    public int xlog_sampling_over_rate_pct = 100;


    //XLog sampling for service patterns options
    @ConfigDesc("XLog patterned sampling mode enabled")
    public boolean xlog_patterned_sampling_enabled = false;
    @ConfigDesc("XLog patterned sampling service patterns\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_patterned_sampling_service_patterns = "";

    @ConfigDesc("XLog patterned sampling but discard profile only not XLog.")
    public boolean xlog_patterned_sampling_only_profile = false;
    @ConfigDesc("XLog patterned sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_patterned_sampling_step1_ms = 100;
    @ConfigDesc("XLog patterned sampling step1 percentage(%)")
    public int xlog_patterned_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog patterned sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_patterned_sampling_step2_ms = 1000;
    @ConfigDesc("XLog patterned sampling step2 percentage(%)")
    public int xlog_patterned_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog patterned sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_patterned_sampling_step3_ms = 3000;
    @ConfigDesc("XLog patterned sampling step3 percentage(%)")
    public int xlog_patterned_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog patterned sampling over step3 percentage(%)")
    public int xlog_patterned_sampling_over_rate_pct = 100;

    //XLog patterned sampling options for another sampling group
    @ConfigDesc("XLog patterned sampling mode enabled")
    public boolean xlog_patterned2_sampling_enabled = false;
    @ConfigDesc("XLog patterned sampling service patterns\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_patterned2_sampling_service_patterns = "";

    @ConfigDesc("XLog patterned sampling but discard profile only not XLog.")
    public boolean xlog_patterned2_sampling_only_profile = false;
    @ConfigDesc("XLog patterned sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_patterned2_sampling_step1_ms = 100;
    @ConfigDesc("XLog patterned sampling step1 percentage(%)")
    public int xlog_patterned2_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog patterned sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_patterned2_sampling_step2_ms = 1000;
    @ConfigDesc("XLog patterned sampling step2 percentage(%)")
    public int xlog_patterned2_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog patterned sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_patterned2_sampling_step3_ms = 3000;
    @ConfigDesc("XLog patterned sampling step3 percentage(%)")
    public int xlog_patterned2_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog patterned sampling over step3 percentage(%)")
    public int xlog_patterned2_sampling_over_rate_pct = 100;

    //XLog patterned sampling options for another sampling group
    @ConfigDesc("XLog patterned sampling mode enabled")
    public boolean xlog_patterned3_sampling_enabled = false;
    @ConfigDesc("XLog patterned sampling service patterns\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_patterned3_sampling_service_patterns = "";

    @ConfigDesc("XLog patterned sampling but discard profile only not XLog.")
    public boolean xlog_patterned3_sampling_only_profile = false;
    @ConfigDesc("XLog patterned sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_patterned3_sampling_step1_ms = 100;
    @ConfigDesc("XLog patterned sampling step1 percentage(%)")
    public int xlog_patterned3_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog patterned sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_patterned3_sampling_step2_ms = 1000;
    @ConfigDesc("XLog patterned sampling step2 percentage(%)")
    public int xlog_patterned3_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog patterned sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_patterned3_sampling_step3_ms = 3000;
    @ConfigDesc("XLog patterned sampling step3 percentage(%)")
    public int xlog_patterned3_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog patterned sampling over step3 percentage(%)")
    public int xlog_patterned3_sampling_over_rate_pct = 100;

    //XLog patterned sampling options for another sampling group
    @ConfigDesc("XLog patterned sampling mode enabled")
    public boolean xlog_patterned4_sampling_enabled = false;
    @ConfigDesc("XLog patterned sampling service patterns\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_patterned4_sampling_service_patterns = "";

    @ConfigDesc("XLog patterned sampling but discard profile only not XLog.")
    public boolean xlog_patterned4_sampling_only_profile = false;
    @ConfigDesc("XLog patterned sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_patterned4_sampling_step1_ms = 100;
    @ConfigDesc("XLog patterned sampling step1 percentage(%)")
    public int xlog_patterned4_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog patterned sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_patterned4_sampling_step2_ms = 1000;
    @ConfigDesc("XLog patterned sampling step2 percentage(%)")
    public int xlog_patterned4_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog patterned sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_patterned4_sampling_step3_ms = 3000;
    @ConfigDesc("XLog patterned sampling step3 percentage(%)")
    public int xlog_patterned4_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog patterned sampling over step3 percentage(%)")
    public int xlog_patterned4_sampling_over_rate_pct = 100;

    //XLog patterned sampling options for another sampling group
    @ConfigDesc("XLog patterned sampling mode enabled")
    public boolean xlog_patterned5_sampling_enabled = false;
    @ConfigDesc("XLog patterned sampling service patterns\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_patterned5_sampling_service_patterns = "";

    @ConfigDesc("XLog patterned sampling but discard profile only not XLog.")
    public boolean xlog_patterned5_sampling_only_profile = false;
    @ConfigDesc("XLog patterned sampling bound millisecond - step1(lowest : range - from 0 to here)")
    public int xlog_patterned5_sampling_step1_ms = 100;
    @ConfigDesc("XLog patterned sampling step1 percentage(%)")
    public int xlog_patterned5_sampling_step1_rate_pct = 3;
    @ConfigDesc("XLog patterned sampling bound millisecond - step2(range - from step1 to here)")
    public int xlog_patterned5_sampling_step2_ms = 1000;
    @ConfigDesc("XLog patterned sampling step2 percentage(%)")
    public int xlog_patterned5_sampling_step2_rate_pct = 10;
    @ConfigDesc("XLog patterned sampling bound millisecond - step3(highest : range - from step2 to here)")
    public int xlog_patterned5_sampling_step3_ms = 3000;
    @ConfigDesc("XLog patterned sampling step3 percentage(%)")
    public int xlog_patterned5_sampling_step3_rate_pct = 30;
    @ConfigDesc("XLog patterned sampling over step3 percentage(%)")
    public int xlog_patterned5_sampling_over_rate_pct = 100;

    //XLog discard options
    @ConfigDesc("XLog discard service patterns\nNo XLog data, but apply to TPS and summary.\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_discard_service_patterns = "";
    @ConfigDesc("Do not discard error even if it's discard pattern.")
    public boolean xlog_discard_service_show_error = true;

    @ConfigDesc("XLog fully discard service patterns\nNo XLog data, No apply to TPS and summary.\neg) /user/{userId}<GET>,/device/*")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String xlog_fully_discard_service_patterns = "";

    //Alert
    @ConfigDesc("Limited length of alert message")
    public int alert_message_length = 3000;
    @ConfigDesc("Minimum interval(ms) in fetching the same alert")
    public long alert_send_interval_ms = 10000;
    @ConfigDesc("PermGen usage for send alert")
    public int alert_perm_warning_pct = 90;

    //Log
    @ConfigDesc("")
    public boolean _log_asm_enabled;
    @ConfigDesc("")
    public boolean _log_udp_xlog_enabled;
    @ConfigDesc("")
    public boolean _log_udp_object_enabled;
    @ConfigDesc("")
    public boolean _log_udp_counter_enabled;
    @ConfigDesc("")
    public boolean _log_datasource_lookup_enabled = true;
    @ConfigDesc("")
    public boolean _log_background_sql = false;
    @ConfigDesc("Log directory")
    public String log_dir = "";
    @ConfigDesc("Retaining log according to date")
    public boolean log_rotation_enabled = true;
    @ConfigDesc("Keeping period of log")
    public int log_keep_days = 7;
    @ConfigDesc("")
    public boolean _trace = false;
    @ConfigDesc("")
    public boolean _trace_use_logger = false;

    //Hook
    @ConfigDesc("Method set for argument hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_args_patterns = "";

    @ConfigDesc("Method set for return hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_return_patterns = "";

    @ConfigDesc("Method set for constructor hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_constructor_patterns = "";

    @ConfigDesc("Method set for dbconnection hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_connection_open_patterns = "";

    @ConfigDesc("Method set for getconnection hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_get_connection_patterns = "";

    
    @ConfigDesc("IntialContext Class Set")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_context_classes = "javax/naming/InitialContext";

    @ConfigDesc("Method set for method hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_method_patterns = "";

    @ConfigDesc("Prefix without Method hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_method_ignore_prefixes = "get,set";

    @ConfigDesc("Class set without Method hookingt")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_method_ignore_classes = "";

    @ConfigDesc("")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_method_exclude_patterns = "";

    @ConfigDesc("Activating public Method hooking")
    public boolean hook_method_access_public_enabled = true;
    @ConfigDesc("Activating private Method hooking")
    public boolean hook_method_access_private_enabled = false;
    @ConfigDesc("Activating protected Method hooking")
    public boolean hook_method_access_protected_enabled = false;
    @ConfigDesc("Activating none Method hooking")
    public boolean hook_method_access_none_enabled = false;
    @ConfigDesc("Activating lambda Method hooking")
    public boolean hook_method_lambda_enable = true;

    @ConfigDesc("Method set for service hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_service_patterns = "";

    @ConfigDesc("hooking service name use a 1st string parameter or class & method name")
    public boolean hook_service_name_use_1st_string_enabled = true;

    @ConfigDesc("Method set for apicall hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_apicall_patterns = "";

    @ConfigDesc("Method set for apicallinfo hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_apicall_info_patterns = "";

    @ConfigDesc("Method set for jsp hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jsp_patterns = "";

    @ConfigDesc("Method set for preparestatement hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jdbc_pstmt_classes = "";

    @ConfigDesc("Method set for statement hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jdbc_stmt_classes = "";

    @ConfigDesc("Method set for resultset hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jdbc_rs_classes = "";

    @ConfigDesc("Method set for dbconnection wrapping")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jdbc_wrapping_driver_patterns = "";

    @ConfigDesc("Exception class patterns - These will seem as error on xlog view.\n (ex) my.app.BizException,my.app.exception.*Exception")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_exception_class_patterns = "";

    @ConfigDesc("Exception class exclude patterns")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_exception_exclude_class_patterns = "";

    @ConfigDesc("Exception handler patterns\n - exceptions passed to these methods are treated as error on xlog view.\n   (ex) my.app.myHandler.handleException")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_exception_handler_method_patterns = "";

    @ConfigDesc("Exception handler exclude class name patterns(can not include star-* in patterns)\n - (ex) my.app.MyManagedException,MyBizException")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_exception_handler_exclude_class_patterns = "";

    @ConfigDesc("Hook for supporting async servlet")
    public boolean hook_async_servlet_enabled = true;

    @ConfigDesc("startAsync impl. method patterns")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_async_servlet_start_patterns = "";

    @ConfigDesc("asyncContext dispatch impl. method patterns")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_async_context_dispatch_patterns = "";

    @ConfigDesc("spring async execution submit patterns")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_spring_async_submit_patterns = "";

    @ConfigDesc("spring async execution hook enabled")
    public boolean hook_spring_async_enabled = true;

    @Deprecated
    @ConfigDesc("Deprecated. use hook_async_callrunnable_enabled")
    public boolean hook_async_callrunnable_enable = true;

    @ConfigDesc("Hook callable and runnable for tracing async processing.\n It hook only 'hook_async_callrunnable_scan_prefixes' option contains pacakage or classes")
    public boolean hook_async_callrunnable_enabled = true;

    @ConfigDesc("scanning range prefixes for hooking callable, runnable implementations and lambda expressions.\n usually your application package.\n 2 or more packages can be separated by commas.")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_async_callrunnable_scan_package_prefixes = "";

    @ConfigDesc("redis key setting patterns.\n refer to org.springframework.data.redis.core.AbstractOperations#rawKey")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String _hook_redis_set_key_patterns = "";

    @ConfigDesc("hook threadpool executor for tracing async processing.")
    public boolean hook_async_thread_pool_executor_enabled = true;

    @ConfigDesc("hystrix execution hook enabled")
    public boolean hook_hystrix_enabled = false;

    @ConfigDesc("")
    public String hook_add_fields = "";
    @ConfigDesc("")
    public boolean _hook_serivce_enabled = true;
    @ConfigDesc("")
    public boolean _hook_dbsql_enabled = true;
    @ConfigDesc("")
    public boolean _hook_dbconn_enabled = true;
    @ConfigDesc("")
    public boolean _hook_cap_enabled = true;
    @ConfigDesc("")
    public boolean _hook_methods_enabled = true;
    @ConfigDesc("")
    public boolean _hook_apicall_enabled = true;
    @ConfigDesc("")
    public boolean _hook_socket_enabled = true;
    @ConfigDesc("")
    public boolean _hook_jsp_enabled = true;
    @ConfigDesc("")
    public boolean _hook_async_enabled = true;
    @ConfigDesc("")
    public boolean _hook_usertx_enabled = true;
    @ConfigDesc("")
    public boolean _hook_spring_rest_enabled = true;
    @ConfigDesc("")
    public boolean _hook_redis_enabled = true;
    @ConfigDesc("")
    public boolean _hook_kafka_enabled = true;
    @ConfigDesc("")
    public boolean _hook_elasticsearch_enabled = true;
    @ConfigDesc("")
    public boolean hook_mongodb_enabled = false;
    @ConfigDesc("")
    public boolean _hook_rabbit_enabled = true;
    @ConfigDesc("")
    public boolean _hook_reactive_enabled = true;
    @ConfigDesc("")
    public boolean _hook_coroutine_enabled = true;
    @ConfigDesc("")
    public boolean _hook_coroutine_debugger_hook_enabled = false;
    @ConfigDesc("")
    public boolean _hook_thread_name_enabled = false;

    @ConfigDesc("")
    public String _hook_direct_patch_classes = "";

    @ConfigDesc("")
    public String _hook_boot_prefix = null;
    @ConfigDesc("for warning a big Map type object that have a lot of entities.\n It may increase system load. be careful to enable this option.")
    public boolean _hook_map_impl_enabled = false;
    @ConfigDesc("")
    public int _hook_map_impl_warning_size = 50000;

    //Control
    @ConfigDesc("Activating Reject service")
    public boolean control_reject_service_enabled = false;
    @ConfigDesc("Minimum count of rejecting active service")
    public int control_reject_service_max_count = 10000;
    @ConfigDesc("Activating Reject URL")
    public boolean control_reject_redirect_url_enabled = false;
    @ConfigDesc("Reject Text")
    public String control_reject_text = "too many request!!";
    @ConfigDesc("Reject URL")
    public String control_reject_redirect_url = "/error.html";

    // Counter
    @ConfigDesc("Activating collect counter")
    public boolean counter_enabled = true;
    @ConfigDesc("think time (ms) of recent user")
    public long counter_recentuser_valid_ms = DateUtil.MILLIS_PER_FIVE_MINUTE;
    @ConfigDesc("Path to file creation directory of process ID file")
    public String counter_object_registry_path = "/tmp/scouter";
    @ConfigDesc("Activating custom jmx")
    public boolean counter_custom_jmx_enabled = false;
    @ConfigDesc("Activating interaction counter")
    public boolean counter_interaction_enabled = false;

    // SFA(Stack Frequency Analyzer)
    @ConfigDesc("Activating period threaddump function")
    public boolean sfa_dump_enabled = false;
    @ConfigDesc("SFA thread dump Interval(ms)")
    public int sfa_dump_interval_ms = 10000;

    //PSTS(Preiodical Stacktrace Step)
    @ConfigDesc("Activating periodical stacktrace step (write fixed interval thread dump on a profile)")
    public boolean _psts_enabled = false;
    @ConfigDesc("PSTS(periodical stacktrace step) thread dump Interval(ms) - hard min limit 2000")
    public int _psts_dump_interval_ms = 10000;
    public boolean _psts_progressive_reactor_thread_trace_enabled = true;

    //Summary
    @ConfigDesc("Activating summary function")
    public boolean summary_enabled = true;
    @ConfigDesc("")
    public boolean _summary_connection_leak_fullstack_enabled = false;
    @ConfigDesc("")
    public int _summary_service_max_count = 5000;
    @ConfigDesc("")
    public int _summary_sql_max_count = 5000;
    @ConfigDesc("")
    public int _summary_api_max_count = 5000;
    @ConfigDesc("")
    public int _summary_ip_max_count = 5000;
    @ConfigDesc("")
    public int _summary_useragent_max_count = 5000;
    @ConfigDesc("")
    public int _summary_error_max_count = 500;
    @ConfigDesc("")
    public int _summary_enduser_nav_max_count = 5000;
    @ConfigDesc("")
    public int _summary_enduser_ajax_max_count = 5000;
    @ConfigDesc("")
    public int _summary_enduser_error_max_count = 5000;
```

## Host agent options
 * **Useful options of host agent**
   (It's not every option. you can get whole options on the source code that the file name is Configure.java.)
  * you can use the variable name as the property name you want to set in configuration file.
   ```(aka in the file scouter.conf by default)```

   * example
   ```properties
   net_collector_ip = 127.0.0.1
   cpu_alert_enabled = false
   ```
   
```java
	//Network
	@ConfigDesc("UDP local IP")
	public String net_local_udp_ip = null;
	@ConfigDesc("UDP local Port")
	public int net_local_udp_port;
	@ConfigDesc("Collector IP")
	public String net_collector_ip = "127.0.0.1";
	@ConfigDesc("Collector UDP Port")
	public int net_collector_udp_port = NetConstants.SERVER_UDP_PORT;
	@ConfigDesc("Collector TCP Port")
	public int net_collector_tcp_port = NetConstants.SERVER_TCP_PORT;
	@ConfigDesc("Collector TCP Session Count")
	public int net_collector_tcp_session_count = 1;
	@ConfigDesc("Collector TCP Socket Timeout(ms)")
	public int net_collector_tcp_so_timeout_ms = 60000;
	@ConfigDesc("Collector TCP Connection Timeout(ms)")
	public int net_collector_tcp_connection_timeout_ms = 3000;
	@ConfigDesc("UDP Buffer Size")
	public int net_udp_packet_max_bytes = 60000;

	//Object
	@ConfigDesc("Deprecated. It's just an alias of monitoring_group_type which overrides this value.")
	public String obj_type = "";
	@ConfigDesc("monitoring group type, commonly named as system name and a monitoring type.\neg) ORDER-JVM, WAREHOUSE-LINUX ...")
	public String monitoring_group_type = "";
	@ConfigDesc("Object Name")
	public String obj_name = "";

	//Manager
	@ConfigDesc("")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public StringSet mgr_log_ignore_ids = new StringSet();

	//Counter
	@ConfigDesc("Activating collect counter")
	public boolean counter_enabled = true;
	@ConfigDesc("Path to file reading directory of java process ID file")
	public String counter_object_registry_path = "/tmp/scouter";

	@ConfigDesc("Activating netstat counter - too many sockets(ESTABLISHED, TIME_WAIT...) may cause heavy cpu load.")
	public boolean counter_netstat_enabled = true;

	//Log
	@ConfigDesc("")
	public boolean log_udp_object = false;
	@ConfigDesc("Retaining log according to date")
	public boolean log_rotation_enabled = true;
	@ConfigDesc("Log directory")
	public String log_dir = "./logs";
	@ConfigDesc("Keeping period of log")
	public int log_keep_days = 365;

	//Disk
	public boolean disk_alert_enabled = true;
	public int disk_warning_pct = 70;
	public int disk_fatal_pct = 90;
	public StringSet disk_ignore_names = new StringSet();

	//Cpu
	public boolean cpu_alert_enabled = true;
	public long cpu_check_period_ms = 300000;
	public long cpu_alert_interval_ms = 30000;
	public int cpu_warning_pct = 70;
	public int cpu_fatal_pct = 90;
	public int cpu_warning_history = 3;
	public int cpu_fatal_history = 3;
	public int _cpu_value_avg_sec = 10;

	//Memory
	public boolean mem_alert_enabled = false;
	public long mem_alert_interval_ms = 30000;
	public int mem_warning_pct = 80;
	public int mem_fatal_pct = 90;

```
