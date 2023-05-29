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

import scouter.lang.DeltaType;
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.ConfigValueType;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.conf.ValueType;
import scouter.lang.conf.ValueTypeDesc;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.server.http.model.CounterProtocol;
import scouter.server.support.telegraf.TgConfig;
import scouter.server.support.telegraf.TgmConfig;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.StrMatch;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringLinkedSet;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Configure extends Thread {

	private static Configure instance = null;
	public final static String CONF_DIR = "./conf/";

    private static JAXBContext jaxbContext;
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(TgConfig.class);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    //telegraf input internal variables
	private static final String TELEGRAF_INPUT_MEASUREMENT_PREFIX = "input_telegraf_$";
	private static final int TELEGRAF_INPUT_MEASUREMENT_PREFIX_LENGTH = TELEGRAF_INPUT_MEASUREMENT_PREFIX.length();
	private static final String TELEGRAF_INPUT_MEASUREMENT_ENABLED_POSTFIX = "_enabled";
	private static final String TELEGRAF_INPUT_MEASUREMENT_DEBUG_ENABLED_POSTFIX = "_debug_enabled";

	private static final String TELEGRAF_INPUT_MEASUREMENT_TAG_FILTER_POSTFIX = "_tag_filter";
	private static final String TELEGRAF_INPUT_MEASUREMENT_COUNTER_MAPPINGS_POSTFIX = "_counter_mappings";

	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_FAMILY_BASE_POSTFIX = "_objFamily_base";
	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_FAMILY_APPEND_TAGS_POSTFIX = "_objFamily_append_tags";

	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_BASE_POSTFIX = "_objType_base";
	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_PREPEND_TAGS_POSTFIX = "_objType_prepend_tags";
	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_APPEND_TAGS_POSTFIX = "_objType_append_tags";
	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_ICON_POSTFIX = "_objType_icon";

	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_NAME_BASE_POSTFIX = "_objName_base";
	private static final String TELEGRAF_INPUT_MEASUREMENT_OBJ_NAME_APPEND_TAGS = "_objName_append_tags";
	private static final String TELEGRAF_INPUT_MEASUREMENT_HOST_TAG_POSTFIX = "_host_tag";
	private static final String TELEGRAF_INPUT_MEASUREMENT_HOST_MAPPINGS_POSTFIX = "_host_mappings";

	//telegraf config (arranged)
	public Map<String, ScouterTgMtConfig> telegrafInputConfigMap = new ConcurrentHashMap<String, ScouterTgMtConfig>();
	@Deprecated
	public Map<String, ScouterTgMtConfig> telegrafInputConfigMapDeprecated = new ConcurrentHashMap<String, ScouterTgMtConfig>();

	//telegraf config (original)
	public TgConfig telegrafOriginalConfig = new TgConfig();

	public final static synchronized Configure getInstance() {
		if (instance == null) {
			instance = new Configure();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	public final static synchronized Configure newInstanceForTestCase() {
		if (instance != null) {
			instance.running = false;
		}
		instance = new Configure();
		instance.setDaemon(true);
		instance.setName(ThreadUtil.getName(instance));
		instance.start();
		return instance;
	}

	//SERVER
	@ConfigDesc("Server ID")
	public String server_id = SysJMX.getHostName();

	//Log
	public int log_test_rate = 0;

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
	@ConfigDesc("Logging some ObjectPack")
	public String log_udp_some_object = "";
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
	@ConfigDesc("Script plugin enabled")
	public boolean plugin_enabled = true;
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

	private Configure() {
		reload(false);
	}

	/**
	 * @deprecated
	 */
	private Configure(boolean b) {
	}

	private long last_load_time = -1;
	private long last_load_time_tg = -1;

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

	public File getTgFile() {
		return new File(input_telegraf_config_file);
	}

	long last_check = 0;

	public synchronized void reload(boolean force) {
		long now = System.currentTimeMillis();
		if (force == false && now < last_check + 3000) return;
		last_check = now;

        boolean configRenewed = reloadConfig();
        reloadTgConfig(configRenewed);
    }

    private void reloadTgConfig(boolean configRenewed) {
        File tgFile = getTgFile();
        if (!tgFile.exists() || tgFile.lastModified() != last_load_time_tg) {
            //for backward compatibility
            if (!tgFile.exists() && configRenewed) {
                telegrafInputConfigMap = telegrafInputConfigMapDeprecated;

            } else if (tgFile.canRead()) {
                try {
                    telegrafOriginalConfig = (TgConfig) unmarshaller.unmarshal(tgFile);
                    last_load_time_tg = tgFile.lastModified();
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
                applyTelegrafInputConfigNew();
            }
        }
    }

    public String getTgConfigContents() {
        File tgFile = getTgFile();
        if (tgFile.exists() && tgFile.canRead()) {
            String tgContents = FileUtil.load(tgFile, "utf-8");
            return tgContents;

        } else if (!tgFile.exists()) {
			return TgConfig.getSampleContents();
		}
        return "";
    }

    public boolean saveTgConfigContents(String text) {
        File tgFile = getTgFile();
        return FileUtil.saveText(tgFile, text);
    }

    private boolean reloadConfig() {
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
            property = ConfigValueUtil.replaceSysProp(temp);
            applyConfig();
            applyTelegrafInputConfig();
            return true;
        }

        return false;
    }

    public static boolean WORKABLE = true;

	private void applyConfig() {
		this.log_test_rate = getInt("log_test_rate", 0);

		this.xlog_queue_size = getInt("xlog_queue_size", 10000);
		this.profile_queue_size = getInt("profile_queue_size", 1000);
		this.log_tcp_action_enabled = getBoolean("log_tcp_action_enabled", false);

		this.xlog_sampling_matcher_gxid_keep_memory_count = getInt("xlog_sampling_matcher_gxid_keep_memory_count", 500000);
		this.xlog_sampling_matcher_xlog_keep_memory_count = getInt("xlog_sampling_matcher_xlog_keep_memory_count", 100000);
		this.xlog_sampling_matcher_xlog_keep_memory_millis = getInt("xlog_sampling_matcher_xlog_keep_memory_millis", 5000);

		this.xlog_sampling_matcher_profile_keep_memory_count = getInt("xlog_sampling_matcher_profile_keep_memory_count", 5000);
		this.xlog_sampling_matcher_profile_keep_memory_secs = getInt("xlog_sampling_matcher_profile_keep_memory_secs", 5);

		this.net_udp_listen_ip = getValue("net_udp_listen_ip", "0.0.0.0");
		this.net_udp_listen_port = getInt("net_udp_listen_port", NetConstants.SERVER_UDP_PORT);
		this.net_tcp_listen_ip = getValue("net_tcp_listen_ip", "0.0.0.0");
		this.net_tcp_listen_port = getInt("net_tcp_listen_port", NetConstants.SERVER_TCP_PORT);
		this.net_tcp_client_so_timeout_ms = getInt("net_tcp_client_so_timeout_ms", 8000);
		this.net_tcp_agent_so_timeout_ms = getInt("net_tcp_agent_so_timeout_ms", 60000);
		this.net_tcp_agent_keepalive_interval_ms = getInt("net_tcp_agent_keepalive_interval_ms", 5000);
		this.net_tcp_get_agent_connection_wait_ms = getInt("net_tcp_get_agent_connection_wait_ms", 1000);
		this.net_http_server_enabled = getBoolean("net_http_server_enabled", false);
		this.net_http_port = getInt("net_http_port", NetConstants.SERVER_HTTP_PORT);
		this.net_http_extweb_dir = getValue("net_http_extweb_dir", "./extweb");
		this.net_http_api_enabled = getBoolean("net_http_api_enabled", false);
		this.net_http_api_swagger_enabled = getBoolean("net_http_api_swagger_enabled", false);
		this.net_http_api_swagger_host_ip = getValue("net_http_api_swagger_host_ip", "");
		this.net_http_api_cors_allow_origin = getValue("net_http_api_cors_allow_origin", "*");
		this.net_http_api_cors_allow_credentials = getValue("net_http_api_cors_allow_credentials", "true");

		this.net_webapp_tcp_client_pool_size = getInt("net_webapp_tcp_client_pool_size", 12);
		this.net_webapp_tcp_client_pool_timeout = getInt("net_webapp_tcp_client_pool_timeout", net_tcp_client_so_timeout_ms);

		this.net_http_api_auth_ip_enabled = getBoolean("net_http_api_auth_ip_enabled", false);
		this.net_http_api_auth_ip_header_key = getValue("net_http_api_auth_ip_header_key", "");
		this.net_http_api_auth_session_enabled = getBoolean("net_http_api_auth_session_enabled", false);
		this.net_http_api_session_timeout = getInt("net_http_api_session_timeout", 3600*24);
		this.net_http_api_auth_bearer_token_enabled = getBoolean("net_http_api_auth_bearer_token_enabled", false);
		this.net_http_api_gzip_enabled = getBoolean("net_http_api_gzip_enabled", true);

		this.net_http_api_allow_ips = getValue("net_http_api_allow_ips", "localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1");
		this.allowIpExact = Stream.of(net_http_api_allow_ips.split(",")).collect(Collectors.toSet());
		if (allowIpExact.size() > 0) {
			this.allowIpMatch = this.allowIpExact.stream().filter(v -> v.contains("*")).map(StrMatch::new).collect(Collectors.toList());
		} else {
			this.allowIpMatch = Collections.emptyList();
		}

		this.server_id = getValue("server_id", SysJMX.getHostName());
		this.db_dir = getValue("db_dir", "./database");
		this.log_dir = getValue("log_dir", "./logs");
		this.plugin_dir = getValue("plugin_dir", "./plugin");
		this.plugin_enabled = getBoolean("plugin_enabled", true);
		this.client_dir = getValue("client_dir", "./client");
		this.temp_dir = getValue("temp_dir", "./tempdata");

		this.object_deadtime_ms = getInt("object_deadtime_ms", 8000);
		this.object_inactive_alert_level = getInt("object_inactive_alert_level", 0);
		this.object_zipkin_deadtime_ms = getInt("object_zipkin_deadtime_ms", 300 * 1000);

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
		this.log_udp_interaction_counter = getBoolean("log_udp_interaction_counter", false);
		this.log_udp_xlog = getBoolean("log_udp_xlog", false);
		this.log_udp_profile = getBoolean("log_udp_profile", false);
		this.log_udp_text = getBoolean("log_udp_text", false);
		this.log_udp_alert = getBoolean("log_udp_alert", false);
		this.log_udp_object = getBoolean("log_udp_object", false);
		this.log_udp_some_object = getValue("log_udp_some_object", "");
		this.log_udp_status = getBoolean("log_udp_status", false);
		this.log_udp_stack = getBoolean("log_udp_stack", false);
		this.log_udp_summary = getBoolean("log_udp_summary", false);
		this.log_udp_batch = getBoolean("log_udp_batch", false);
		this.log_service_handler_list = getBoolean("log_service_handler_list", false);
		this.log_udp_span = getBoolean("log_udp_span", false);

		this.log_index_traversal_warning_count = getInt("log_index_traversal_warning_count", 100);

		this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
		this.log_keep_days = getInt("log_keep_days", 31);
		this.log_sql_parsing_fail_enabled = getBoolean("log_sql_parsing_fail_enabled", false);
		this._trace = getBoolean("_trace", false);

		this._auto_5m_sampling = getBoolean("_auto_5m_sampling", true);

		this.xlog_realtime_lower_bound_ms = getInt("xlog_realtime_lower_bound_ms", 0);
		this.xlog_pasttime_lower_bound_ms = getInt("xlog_pasttime_lower_bound_ms", 0);
		this.mgr_purge_enabled = getBoolean("mgr_purge_enabled", true);
		this.mgr_purge_disk_usage_pct = getInt("mgr_purge_disk_usage_pct", 80);

		this.mgr_purge_keep_days = getInt("mgr_purge_keep_days", 10);
		this.mgr_purge_profile_keep_days = getInt("mgr_purge_profile_keep_days", 10);
		if(mgr_purge_profile_keep_days == 0) mgr_purge_profile_keep_days = this.mgr_purge_keep_days;

		this.mgr_purge_xlog_without_profile_keep_days = getInt("mgr_purge_xlog_without_profile_keep_days", mgr_purge_profile_keep_days*3);
		this.mgr_purge_xlog_keep_days = getInt("mgr_purge_xlog_keep_days", mgr_purge_profile_keep_days * 3);
		if(mgr_purge_xlog_keep_days == 0) mgr_purge_xlog_keep_days = this.mgr_purge_xlog_without_profile_keep_days;

		this.mgr_purge_counter_keep_days = getInt("mgr_purge_counter_keep_days", mgr_purge_keep_days * 7);

		this.mgr_purge_realtime_counter_keep_days = getInt("mgr_purge_realtime_counter_keep_days", mgr_purge_counter_keep_days);
		this.mgr_purge_tag_counter_keep_days = getInt("mgr_purge_tag_counter_keep_days", mgr_purge_counter_keep_days);
		this.mgr_purge_visitor_counter_keep_days = getInt("mgr_purge_visitor_counter_keep_days", mgr_purge_counter_keep_days);

		this.mgr_purge_daily_text_days = getInt("mgr_purge_daily_text_days",
				Math.max(mgr_purge_tag_counter_keep_days * 2, mgr_purge_xlog_keep_days * 2));

		this.mgr_purge_sum_data_days = getInt("mgr_purge_sum_data_days", mgr_purge_sum_data_days);

		this.mgr_text_db_daily_service_enabled = getBoolean("mgr_text_db_daily_service_enabled", false);
		this.mgr_text_db_daily_api_enabled = getBoolean("mgr_text_db_daily_api_enabled", false);
		this.mgr_text_db_daily_ua_enabled = getBoolean("mgr_text_db_daily_ua_enabled", false);

		this._mgr_text_db_index_default_mb = getInt("_mgr_text_db_index_default_mb", 1);
		this._mgr_text_db_index_service_mb = getInt("_mgr_text_db_index_service_mb", 1);
		this._mgr_text_db_index_api_mb = getInt("_mgr_text_db_index_api_mb", 1);
		this._mgr_text_db_index_ua_mb = getInt("_mgr_text_db_index_ua_mb", 1);
		this._mgr_text_db_index_login_mb = getInt("_mgr_text_db_index_login_mb", 1);
		this._mgr_text_db_index_desc_mb = getInt("_mgr_text_db_index_desc_mb", 1);
		this._mgr_text_db_index_hmsg_mb = getInt("_mgr_text_db_index_hmsg_mb", 1);
		this._mgr_text_db_daily_index_mb = getInt("_mgr_text_db_daily_index_mb", 1);

		this._mgr_kv_store_index_default_mb = getInt("_mgr_kv_store_index_default_mb", 8);
		this._mgr_xlog_id_index_mb = getInt("_mgr_xlog_id_index_mb", 1);

		this.ext_link_name = getValue("ext_link_name", "scouter-paper");
		this.ext_link_url_pattern = getValue("ext_link_url_pattern", "http://my-scouter-paper-ip:6188/index.html#/paper?&address=localhost&port=6188&realtime=false&xlogElapsedTime=8000&instances=$[objHashes]&from=$[from]&to=$[to]&layout=my-layout-template-01");

		this._net_udp_worker_thread_count = getInt("_net_udp_worker_thread_count", 3);
		this.geoip_data_city_file = getValue("geoip_data_city_file", CONF_DIR + "GeoLiteCity.dat");
		this.geoip_enabled = getBoolean("geoip_enabled", true);

		//this.xlog_profile_save_lower_bound_ms = getInt("xlog_profile_save_lower_bound_ms", 0);
		this.sql_table_parsing_enabled = getBoolean("sql_table_parsing_enabled", true);

		this.mgr_log_ignore_ids = getStringSet("mgr_log_ignore_ids", ",");

		this.tagcnt_enabled = getBoolean("tagcnt_enabled", true);
		
		this.visitor_hourly_count_enabled = getBoolean("visitor_hourly_count_enabled", true);
		
		this.net_tcp_service_pool_size = getInt("net_tcp_service_pool_size", 100);

		this.req_search_xlog_max_count = getInt("req_search_xlog_max_count", 500);

		this.input_telegraf_enabled = getBoolean("input_telegraf_enabled", true);
		this.input_telegraf_debug_enabled = getBoolean("input_telegraf_debug_enabled", false);

		this.input_telegraf_delta_counter_normalize_default = getBoolean("input_telegraf_delta_counter_normalize_default", true);
		this.input_telegraf_delta_counter_normalize_default_seconds = getInt("input_telegraf_delta_counter_normalize_default_seconds", 30);

        this.telegraf_object_deadtime_ms = getInt("telegraf_object_deadtime_ms", 35000);

		this.input_telegraf_$measurement$_enabled = getBoolean("input_telegraf_$measurement$_enabled", true);
		this.input_telegraf_$measurement$_debug_enabled = getBoolean("input_telegraf_$measurement$_debug_enabled", false);
		this.input_telegraf_$measurement$_tag_filter = getValue("input_telegraf_$measurement$_tag_filter", "");
		this.input_telegraf_$measurement$_counter_mappings = getValue("input_telegraf_$measurement$_counter_mappings", "");
		this.input_telegraf_$measurement$_objFamily_base = getValue("input_telegraf_$measurement$_objFamily_base", "");
		this.input_telegraf_$measurement$_objFamily_append_tags = getValue("input_telegraf_$measurement$_objFamily_append_tags", "");
		this.input_telegraf_$measurement$_objType_base = getValue("input_telegraf_$measurement$_objType_base", "");
		this.input_telegraf_$measurement$_objType_prepend_tags = getValue("input_telegraf_$measurement$_objType_prepend_tags", "scouter_obj_type_prefix");
		this.input_telegraf_$measurement$_objType_append_tags = getValue("input_telegraf_$measurement$_objType_append_tags", "");
		this.input_telegraf_$measurement$_objType_icon = getValue("input_telegraf_$measurement$_objType_icon", "");
		this.input_telegraf_$measurement$_objName_base = getValue("input_telegraf_$measurement$_objName_base", "");
		this.input_telegraf_$measurement$_objName_append_tags = getValue("input_telegraf_$measurement$_objName_append_tags", "");
		this.input_telegraf_$measurement$_host_tag = getValue("input_telegraf_$measurement$_host_tag", "host");
		this.input_telegraf_$measurement$_host_mappings = getValue("input_telegraf_$measurement$_host_mappings", "");

		ConfObserver.exec();
	}

	/**
	 * Configuration for telegraf input
	 */
	protected void applyTelegrafInputConfig() {
		Map<String, ScouterTgMtConfig> tConfigMap = new HashMap<String, ScouterTgMtConfig>();

		for (Map.Entry<Object, Object> e : property.entrySet()) {
			String key = (String) e.getKey();
			String value = (String) e.getValue();
			if(value == null) {
				continue;
			}

			//eg) input_telegraf_$redis_keyspace$_enabled
			if (key.startsWith(TELEGRAF_INPUT_MEASUREMENT_PREFIX)) { //start with "input_telegraf_$"
				String simplifiedKey = key.substring(TELEGRAF_INPUT_MEASUREMENT_PREFIX_LENGTH); //redis_keyspace$_enabled
				int secondDollar = simplifiedKey.indexOf("$_");
				String measurement = simplifiedKey.substring(0, secondDollar); //redis_keyspace
				String postfix = simplifiedKey.substring(secondDollar + 1);


				ScouterTgMtConfig tConfig = tConfigMap.get(measurement);
				if (tConfig == null) {
					tConfig = new ScouterTgMtConfig(measurement);
					tConfig.setObjTypePrependTags(Arrays.asList(input_telegraf_$measurement$_objType_prepend_tags));
					tConfigMap.put(measurement, tConfig);
				}

				if (TELEGRAF_INPUT_MEASUREMENT_ENABLED_POSTFIX.equals(postfix)) {
					try {
						tConfig.setEnabled(Boolean.parseBoolean(value));
					} catch (Exception ignored) {}

				} else if (TELEGRAF_INPUT_MEASUREMENT_DEBUG_ENABLED_POSTFIX.equals(postfix)) {
					try {
						tConfig.setDebugEnabled(Boolean.parseBoolean(value));
					} catch (Exception ignored) {}

				} else if (TELEGRAF_INPUT_MEASUREMENT_TAG_FILTER_POSTFIX.equals(postfix)) {
					String[] mappings = StringUtil.split(value, ',');
					if (mappings.length == 0) {
						continue;
					}

					Map<String, List<String>> tagFilterMap = new HashMap<String, List<String>>();
					for (String mapping : mappings) {
						String[] kv = StringUtil.split(mapping, ':');
						if (kv.length != 2) {
							Logger.println("CFG003", "Abnormal line protocol tag mapping config.");
							continue;
						}
						List list = tagFilterMap.get(kv[0]);
						if (list == null) {
							list = new ArrayList();
							tagFilterMap.put(kv[0], list);
						}
						list.add(kv[1]);
					}
					tConfig.setTagFilter(tagFilterMap);

				} else if (TELEGRAF_INPUT_MEASUREMENT_COUNTER_MAPPINGS_POSTFIX.equals(postfix)) {
					String[] counterMappings = StringUtil.split(value, ',');
					if (counterMappings == null || counterMappings.length == 0) {
						continue;
					}
					//format: {line-protocol field name}:{scouter counter name}:{display name?}:{unit?}:{hasTotal?}
					Map<String, CounterProtocol> counterMappingMap = new HashMap<String, CounterProtocol>();
					for (String counterMapping : counterMappings) {
						CounterProtocol counter = new CounterProtocol();
						String[] split = StringUtil.splitByWholeSeparatorPreserveAllTokens(counterMapping, ':');
						if (split.length >= 2) {
							String originName = split[0];
							String originName0 = originName;
							if (originName.charAt(0) == '&') {
                                if (originName.charAt(1) == '&') {
									counter.setDeltaType(DeltaType.BOTH);
									originName0 = originName.substring(2);
                                } else {
									counter.setDeltaType(DeltaType.DELTA);
									originName0 = originName.substring(1);
								}
							}
							counterMappingMap.put(originName0, counter);
							counter.setName(split[1]);
						}
						if (split.length >= 3) {
							if (StringUtil.isNotEmpty(split[2])) {
								counter.setDisplayName(split[2]);
							} else {
								counter.setDisplayName(split[1]);
							}
						} else {
							counter.setDisplayName(split[1]);
						}
						if (split.length >= 4) {
							counter.setUnit(split[3]);
						} else {
							counter.setUnit("");
						}
						if (split.length >= 5) {
							try {
								counter.setTotal(Boolean.parseBoolean(split[4]));
							} catch (Exception ignored) {}
						}
						if (split.length >= 6) {
							int normalizeSec = 0;
							try {
								normalizeSec = Integer.valueOf(split[5]);
							} catch (Exception ignored) {}
							counter.setNormalizeSec(normalizeSec);
						}
					}
					tConfig.setCounterMapping(counterMappingMap);

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_FAMILY_BASE_POSTFIX.equals(postfix)) {
					tConfig.setObjFamilyBase(value);

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_FAMILY_APPEND_TAGS_POSTFIX.equals(postfix)) {
					String[] tags = StringUtil.split(value, ',');
					if (tags == null || tags.length == 0) {
						continue;
					}
					tConfig.setObjFamilyAppendTags(Arrays.asList(tags));

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_BASE_POSTFIX.equals(postfix)) {
					tConfig.setObjTypeBase(value);

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_PREPEND_TAGS_POSTFIX.equals(postfix)) {
					if (StringUtil.isEmpty(value)) {
						value = input_telegraf_$measurement$_objType_prepend_tags; //default
					}
					String[] tags = StringUtil.split(value, ',');
					if (tags == null || tags.length == 0) {
						continue;
					}
					tConfig.setObjTypePrependTags(Arrays.asList(tags));

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_APPEND_TAGS_POSTFIX.equals(postfix)) {
					String[] tags = StringUtil.split(value, ',');
					if (tags == null || tags.length == 0) {
						continue;
					}
					tConfig.setObjTypeAppendTags(Arrays.asList(tags));

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_TYPE_ICON_POSTFIX.equals(postfix)) {
					tConfig.setObjTypeIcon(value);

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_NAME_BASE_POSTFIX.equals(postfix)) {
					tConfig.setObjNameBase(value);

				} else if (TELEGRAF_INPUT_MEASUREMENT_OBJ_NAME_APPEND_TAGS.equals(postfix)) {
					String[] tags = StringUtil.split(value, ',');
					if (tags == null || tags.length == 0) {
						continue;
					}
					tConfig.setObjNameAppendTags(Arrays.asList(tags));

				} else if (TELEGRAF_INPUT_MEASUREMENT_HOST_TAG_POSTFIX.equals(postfix)) {
					tConfig.setHostTag(value);

				} else if (TELEGRAF_INPUT_MEASUREMENT_HOST_MAPPINGS_POSTFIX.equals(postfix)) {
					String[] hostMappings = StringUtil.split(value, ',');
					if (hostMappings == null || hostMappings.length == 0) {
						continue;
					}
					Map<String, String> hostMappingMap = new HashMap<String, String>();
					for (String hostAndMap : hostMappings) {
						String[] split = StringUtil.split(hostAndMap, ':');
						if (split.length == 2) {
							hostMappingMap.put(split[0], split[1]);
						}
					}
					tConfig.setHostMapping(hostMappingMap);

				}
			}
		}

        telegrafInputConfigMapDeprecated = tConfigMap;
//		for (Map.Entry<String, ScouterTgMtConfig> tConfigEntry : tConfigMap.entrySet()) {
//			telegrafInputConfigMapDeprecated.put(tConfigEntry.getKey(), tConfigEntry.getValue());
//		}
	}

	/**
	 * Configuration for telegraf input
	 */
	protected void applyTelegrafInputConfigNew() {
		Map<String, ScouterTgMtConfig> tConfigMap = new HashMap<>();
		if (telegrafOriginalConfig == null) {
			return;
		}

		/*
		 overwrite to old options for backward compatability
		 */
		input_telegraf_enabled = telegrafOriginalConfig.enabled;
        if (input_telegraf_enabled) {
            net_http_server_enabled = true;
        }
		input_telegraf_debug_enabled = telegrafOriginalConfig.debugEnabled;
		input_telegraf_delta_counter_normalize_default = telegrafOriginalConfig.deltaCounterNormalizeDefault;
		input_telegraf_delta_counter_normalize_default_seconds = telegrafOriginalConfig.deltaCounterNormalizeDefaultSeconds;
		telegraf_object_deadtime_ms = telegrafOriginalConfig.objectDeadtimeMs;

		for (TgmConfig baseMtConfig : telegrafOriginalConfig.measurements) {
			String measurementName = baseMtConfig.measurementName;
			ScouterTgMtConfig scouterMtConfig = tConfigMap.get(measurementName);
			if (scouterMtConfig == null) {
				scouterMtConfig = new ScouterTgMtConfig(measurementName);
				tConfigMap.put(measurementName, scouterMtConfig);
			}

			scouterMtConfig.setEnabled(baseMtConfig.enabled);
			scouterMtConfig.setDebugEnabled(baseMtConfig.debugEnabled);

			scouterMtConfig.setTagFilter(baseMtConfig.tagFilters.stream().collect(Collectors.toMap(
			        f -> f.tag,
                    f -> f.match,
                    (v1, v2) -> { v1.addAll(v2); return v1; },
                    HashMap::new))
			);

            baseMtConfig.tagFilters.stream()
                    .collect(Collectors.groupingBy(f -> f.tag))
                    .entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
            ;

            scouterMtConfig.setObjFamilyBase(baseMtConfig.objFamilyBase);
			scouterMtConfig.setObjFamilyAppendTags(baseMtConfig.objFamilyAppendTags);

			scouterMtConfig.setObjTypeBase(baseMtConfig.objTypeBase);
			scouterMtConfig.setObjTypePrependTags(baseMtConfig.objTypePrependTags);
			scouterMtConfig.setObjTypeAppendTags(baseMtConfig.objTypeAppendTags);
			scouterMtConfig.setObjTypeIcon(baseMtConfig.objTypeIcon);

			scouterMtConfig.setObjNameBase(baseMtConfig.objNameBase);
			scouterMtConfig.setObjNameAppendTags(baseMtConfig.objNameAppendTags);

			scouterMtConfig.setHostTag(baseMtConfig.hostTag);
			scouterMtConfig.setHostMapping(baseMtConfig.hostMappings.stream()
					.collect(Collectors.toMap(m -> m.telegraf, m -> m.scouter))
			);

            scouterMtConfig.setCounterMapping(baseMtConfig.counterMappings.stream()
                    .collect(Collectors.toMap(m -> m.tgFieldName, CounterProtocol::of))
            );
        }

		//for backward compatability
		for (Map.Entry<String, ScouterTgMtConfig> deprecatedConfigEntry : telegrafInputConfigMapDeprecated.entrySet()) {
			if (!tConfigMap.containsKey(deprecatedConfigEntry.getKey())) {
                tConfigMap.put(deprecatedConfigEntry.getKey(), deprecatedConfigEntry.getValue());
			}
		}

        telegrafInputConfigMap = tConfigMap;
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
			if (file.getParentFile().exists() == false) {
				file.getParentFile().mkdirs();
			}
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

	private static HashSet<String> ignoreSet = new HashSet<String>();

	static {
		ignoreSet.add("property");
        ignoreSet.add("telegrafInputConfigMap");
		ignoreSet.add("allowIpExact");
		ignoreSet.add("allowIpMatch");
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
	
	public StringKeyLinkedMap<String> getConfigureDesc() {
		return ConfigValueUtil.getConfigDescMap(this);
	}

	public StringKeyLinkedMap<ValueType> getConfigureValueType() {
		return ConfigValueUtil.getConfigValueTypeMap(this);
	}

	public StringKeyLinkedMap<ValueTypeDesc> getConfigureValueTypeDesc() {
		return ConfigValueUtil.getConfigValueTypeDescMap(this);
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

	public static void main(String[] args) {
		StringKeyLinkedMap<ValueType> map = new Configure().getConfigureValueType();
		System.out.println(map);

//		Configure o = new Configure(true);
//		StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(o);
//		StringKeyLinkedMap<String> descMap = ConfigValueUtil.getConfigDescMap(o);
//		StringEnumer enu = defMap.keys();
//		while (enu.hasMoreElements()) {
//			String key = enu.nextString();
//			if (ignoreSet.contains(key))
//				continue;
//			System.out.println(key + " : " + ConfigValueUtil.toValue(defMap.get(key) + (descMap.containsKey(key) ? " (" + descMap.get(key) + ")" : "")));
//		}
	}


}
