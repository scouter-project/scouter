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

package scouter.net;

import java.util.HashSet;
import java.util.Set;

public class RequestCmd {
	public static final String CLOSE = "CLOSE";
	public static final String LOGIN = "LOGIN";
	public static final String INTERNAL_LOGIN = "INTERNAL_LOGIN";
	public static final String CHECK_LOGIN = "CHECK_LOGIN";
	public static final String CHECK_SESSION = "CHECK_SESSION";
	public static final String GET_LOGIN_LIST = "GET_LOGIN_LIST";
	//
	public static final String OBJECT_INFO = "OBJECT_INFO";
	public static final String OBJECT_THREAD_LIST = "OBJECT_THREAD_LIST";
	public static final String OBJECT_THREAD_DETAIL = "OBJECT_THREAD_DETAIL";
	public static final String OBJECT_THREAD_CONTROL = "OBJECT_THREAD_CONTROL";
	public static final String OBJECT_ENV = "OBJECT_ENV";
	public static final String OBJECT_CLASS_LIST = "OBJECT_CLASS_LIST";
	public static final String OBJECT_LOAD_CLASS_BY_STREAM = "OBJECT_LOAD_CLASS_BY_STREAM";
	public static final String OBJECT_CLASS_DESC = "OBJECT_CLASS_DESC";
	public static final String OBJECT_CHECK_RESOURCE_FILE = "OBJECT_CHECK_RESOURCE_FILE";
	public static final String OBJECT_DOWNLOAD_JAR = "OBJECT_DOWNLOAD_JAR";
	public static final String OBJECT_STAT_LIST = "OBJECT_STAT_LIST";
	public static final String OBJECT_RESET_CACHE = "OBJECT_RESET_CACHE";
	public static final String OBJECT_SET_KUBE_SEQ = "OBJECT_SET_KUBE_SEQ";
	public static final String OBJECT_ACTIVE_SERVICE_LIST = "OBJECT_ACTIVE_SERVICE_LIST";
	public static final String OBJECT_ACTIVE_SERVICE_LIST_GROUP = "OBJECT_ACTIVE_SERVICE_LIST_GROUP";
	public static final String OBJECT_TODAY_FULL_LIST = "OBJECT_TODAY_FULL_LIST";
	public static final String OBJECT_REMOVE = "OBJECT_REMOVE";
	public static final String OBJECT_HEAPHISTO = "OBJECT_HEAPHISTO";
	public static final String OBJECT_THREAD_DUMP = "OBJECT_THREAD_DUMP";

	public static final String TRIGGER_ACTIVE_SERVICE_LIST = "TRIGGER_ACTIVE_SERVICE_LIST";
	public static final String TRIGGER_THREAD_DUMP = "TRIGGER_THREAD_DUMP"; //java and golang pprof cpu profile as plain text
    public static final String TRIGGER_THREAD_DUMPS_FROM_CONDITIONS = "TRIGGER_THREAD_DUMPS_FROM_CONDITIONS";
	public static final String TRIGGER_THREAD_LIST = "TRIGGER_THREAD_LIST";
	public static final String TRIGGER_HEAPHISTO = "TRIGGER_HEAPHISTO";

	public static final String TRIGGER_BLOCK_PROFILE = "TRIGGER_BLOCK_PROFILE"; //golang pprof block profile as plain text
	public static final String TRIGGER_MUTEX_PROFILE = "TRIGGER_MUTEX_PROFILE"; //golang pprof mutex profile as plain text

    public static final String TRIGGER_DUMP_REASON = "TRIGGER_DUMP_REASON";
    public static final String TRIGGER_DUMP_REASON_TYPE_CPU_EXCEEDED = "TRIGGER_DUMP_REASON_TYPE_CPU_EXCEEDED";

	public static final String OBJECT_SYSTEM_GC = "OBJECT_SYSTEM_GC";
	public static final String OBJECT_DUMP_FILE_LIST = "OBJECT_DUMP_FILE_LIST";
	public static final String OBJECT_DUMP_FILE_DETAIL = "OBJECT_DUMP_FILE_DETAIL";

	public static final String OBJECT_CALL_HEAP_DUMP = "OBJECT_CALL_HEAP_DUMP";
	public static final String OBJECT_LIST_HEAP_DUMP = "OBJECT_LIST_HEAP_DUMP";
	public static final String OBJECT_DOWNLOAD_HEAP_DUMP = "OBJECT_DOWNLOAD_HEAP_DUMP";
	public static final String OBJECT_DELETE_HEAP_DUMP = "OBJECT_DELETE_HEAP_DUMP";

	public static final String OBJECT_CALL_CPU_PROFILE = "OBJECT_CALL_CPU_PROFILE"; //golang pprof cpu profile as protobuf
	public static final String OBJECT_CALL_BLOCK_PROFILE = "OBJECT_CALL_BLOCK_PROFILE"; //golang pprof block profile as protobuf
	public static final String OBJECT_CALL_MUTEX_PROFILE = "OBJECT_CALL_MUTEX_PROFILE"; //golang pprof mutex profile as protobuf

	public static final String OBJECT_LIST_REAL_TIME = "OBJECT_LIST_REAL_TIME";
	public static final String OBJECT_LIST_LOAD_DATE = "OBJECT_LIST_LOAD_DATE";
	public static final String OBJECT_REMOVE_INACTIVE = "OBJECT_REMOVE_INACTIVE";
	public static final String OBJECT_REMOVE_IN_MEMORY = "OBJECT_REMOVE_IN_MEMORY";

	public static final String OBJECT_FILE_SOCKET = "OBJECT_FILE_SOCKET";
	public static final String OBJECT_SOCKET = "SOCKET";

	public static final String SERVER_VERSION = "SERVER_VERSION";
	public static final String SERVER_LOG_LIST = "SERVER_LOG_LIST";
	public static final String SERVER_LOG_DETAIL = "SERVER_LOG_DETAIL";

	// host request
	public static final String HOST_TOP = "HOST_TOP";
	public static final String HOST_PROCESS_DETAIL = "HOST_PROCESS_DETAIL";
	public static final String HOST_DISK_USAGE = "HOST_DISK_USAGE";
	public static final String HOST_NET_STAT = "HOST_NET_STAT";
	public static final String HOST_WHO = "HOST_WHO";
	public static final String HOST_MEMINFO = "HOST_MEMINFO";

	// kvm request
	public static final String KVM_NET_PERF = "KVM_NET_PERF";
	public static final String KVM_DISK_PERF = "KVM_DISK_PERF";

	public static final String SERVER_THREAD_LIST = "SERVER_THREAD_LIST";
	public static final String SERVER_THREAD_DETAIL = "SERVER_THREAD_DETAIL";
	public static final String SERVER_ENV = "SERVER_ENV";
	public static final String SERVER_STATUS = "SERVER_STATUS";
	public static final String SERVER_TIME = "SERVER_TIME";
	public static final String SERVER_DB_LIST = "SERVER_DB_LIST";
	public static final String SERVER_DB_DELETE = "SERVER_DB_DELETE";
	public static final String REMOTE_CONTROL = "REMOTE_CONTROL";
	public static final String REMOTE_CONTROL_ALL = "REMOTE_CONTROL_ALL";
	public static final String CHECK_JOB = "CHECK_JOB";

	// //////////////////////////////////////
	public static final String TRANX_REAL_TIME = "TRANX_REAL_TIME";
	public static final String TRANX_LOAD_TIME = "TRANX_LOAD_TIME";
	public static final String XLOG_READ_BY_TXID = "XLOG_READ_BY_TXID";
	public static final String XLOG_READ_BY_GXID = "XLOG_READ_BY_GXID";
	public static final String XLOG_LOAD_BY_TXIDS = "XLOG_LOAD_BY_TXIDS";
	public static final String XLOG_LOAD_BY_GXID = "XLOG_LOAD_BY_GXID";
	public static final String TRANX_PROFILE = "TRANX_PROFILE";
	public static final String TRANX_PROFILE_FULL = "TRANX_PROFILE_FULL";
	public static final String TRANX_REAL_TIME_GROUP = "TRANX_REAL_TIME_GROUP";
	public static final String TRANX_REAL_TIME_GROUP_LATEST = "TRANX_REAL_TIME_GROUP_LATEST";
	public static final String TRANX_LOAD_TIME_GROUP = "TRANX_LOAD_TIME_GROUP";
	public static final String TRANX_LOAD_TIME_GROUP_V2 = "TRANX_LOAD_TIME_GROUP_V2";
	public static final String QUICKSEARCH_XLOG_LIST = "QUICKSEARCH_XLOG_LIST";
	public static final String SEARCH_XLOG_LIST = "SEARCH_XLOG_LIST";

	// /////////////////////////////////////
	public static final String COUNTER_PAST_TIME = "COUNTER_PAST_TIME";
	public static final String COUNTER_PAST_TIME_ALL = "COUNTER_PAST_TIME_ALL";
	public static final String COUNTER_PAST_TIME_TOT = "COUNTER_PAST_TIME_TOT";
	public static final String COUNTER_PAST_TIME_GROUP = "COUNTER_PAST_TIME_GROUP";

	public static final String COUNTER_PAST_DATE = "COUNTER_PAST_DATE";
	public static final String COUNTER_PAST_DATE_ALL = "COUNTER_PAST_DATE_ALL";
	public static final String COUNTER_PAST_DATE_TOT = "COUNTER_PAST_DATE_TOT";
	public static final String COUNTER_PAST_DATE_GROUP = "COUNTER_PAST_DATE_GROUP";
	public static final String COUNTER_PAST_LONGDATE_GROUP = "COUNTER_PAST_LONGDATE_GROUP";

	public static final String COUNTER_PAST_LONGDATE_ALL = "COUNTER_PAST_LONGDATE_ALL";
	public static final String COUNTER_PAST_LONGDATE_TOT = "COUNTER_PAST_LONGDATE_TOT";

	public static final String COUNTER_REAL_TIME = "COUNTER_REAL_TIME";
	public static final String COUNTER_REAL_TIME_ALL = "COUNTER_REAL_TIME_ALL";
	public static final String COUNTER_REAL_TIME_TOT = "COUNTER_REAL_TIME_TOT";
	public static final String COUNTER_REAL_TIME_OBJECT_ALL = "COUNTER_REAL_TIME_OBJECT_ALL";
	public static final String COUNTER_REAL_TIME_OBJECT_TYPE_ALL = "COUNTER_REAL_TIME_OBJECT_TYPE_ALL";
	public static final String COUNTER_REAL_TIME_MULTI = "COUNTER_REAL_TIME_MULTI";
	public static final String COUNTER_REAL_TIME_GROUP = "COUNTER_REAL_TIME_GROUP";
	public static final String COUNTER_REAL_TIME_ALL_MULTI = "COUNTER_REAL_TIME_ALL_MULTI";

	public static final String INTR_COUNTER_REAL_TIME_BY_OBJ = "INTR_COUNTER_REAL_TIME_BY_OBJ";

	public static final String COUNTER_TODAY = "COUNTER_TODAY";
	public static final String COUNTER_TODAY_ALL = "COUNTER_TODAY_ALL";
	public static final String COUNTER_TODAY_TOT = "COUNTER_TODAY_TOT";
	public static final String COUNTER_TODAY_GROUP = "COUNTER_TODAY_GROUP";

	public static final String ACTIVESPEED_REAL_TIME = "ACTIVESPEED_REAL_TIME";
	public static final String ACTIVESPEED_REAL_TIME_GROUP = "ACTIVESPEED_REAL_TIME_GROUP";
	public static final String ACTIVESPEED_GROUP_REAL_TIME = "ACTIVESPEED_GROUP_REAL_TIME";
	public static final String ACTIVESPEED_GROUP_REAL_TIME_GROUP = "ACTIVESPEED_GROUP_REAL_TIME_GROUP";
	public static final String SHOW_REAL_TIME_STRING = "SHOW_REAL_TIME_STRING";
	public static final String COUNTER_MAP_REAL_TIME = "COUNTER_MAP_REAL_TIME";

	public static final String ALERT_REAL_TIME = "ALERT_REAL_TIME";
	public static final String ALERT_LOAD_TIME = "ALERT_LOAD_TIME";
	public static final String ALERT_DAILY_COUNT = "ALERT_DAILY_COUNT";
	public static final String ALERT_TITLE_COUNT = "ALERT_TITLE_COUNT";
	public static final String GET_COUNTER_EXIST_DAYS = "GET_COUNTER_EXIST_DAYS";

	public static final String GET_TEXT = "GET_TEXT";
	public static final String GET_TEXT_100 = "GET_TEXT_100";
	public static final String GET_TEXT_PACK = "GET_TEXT_PACK";
	public static final String GET_TEXT_ANY_TYPE= "GET_TEXT_ANY_TYPE";

	public static final String GET_GLOBAL_KV = "GET_GLOBAL_KV";
	public static final String SET_GLOBAL_KV = "SET_GLOBAL_KV";
	public static final String SET_GLOBAL_TTL = "SET_GLOBAL_TTL";
	public static final String GET_CUSTOM_KV = "GET_CUSTOM_KV";
	public static final String SET_CUSTOM_KV = "SET_CUSTOM_KV";
	public static final String SET_CUSTOM_TTL = "SET_CUSTOM_TTL";
	public static final String GET_GLOBAL_KV_BULK = "GET_GLOBAL_KV_BULK";
	public static final String SET_GLOBAL_KV_BULK = "SET_GLOBAL_KV_BULK";
	public static final String GET_CUSTOM_KV_BULK = "GET_CUSTOM_KV_BULK";
	public static final String SET_CUSTOM_KV_BULK = "SET_CUSTOM_KV_BULK";

	public static final String GET_CONFIGURE_SERVER = "GET_CONFIGURE_SERVER";
	public static final String SET_CONFIGURE_SERVER = "SET_CONFIGURE_SERVER";
	public static final String LIST_CONFIGURE_SERVER = "LIST_CONFIGURE_SERVER";
	public static final String GET_CONFIGURE_WAS = "GET_CONFIGURE_WAS";
	public static final String SET_CONFIGURE_WAS = "SET_CONFIGURE_WAS";
	public static final String LIST_CONFIGURE_WAS = "LIST_CONFIGURE_WAS";
	public static final String REDEFINE_CLASSES = "REDEFINE_CLASSES";
	public static final String CONFIGURE_DESC = "CONFIGURE_DESC";
	public static final String CONFIGURE_VALUE_TYPE = "CONFIGURE_VALUE_TYPE";
	public static final String CONFIGURE_VALUE_TYPE_DESC = "CONFIGURE_VALUE_TYPE_DESC";
	public static final String GET_CONFIGURE_TELEGRAF = "GET_CONFIGURE_TELEGRAF";
	public static final String SET_CONFIGURE_TELEGRAF = "SET_CONFIGURE_TELEGRAF";
	public static final String GET_CONFIGURE_COUNTERS_SITE = "GET_CONFIGURE_COUNTERS_SITE";
	public static final String SET_CONFIGURE_COUNTERS_SITE = "SET_CONFIGURE_COUNTERS_SITE";

	public static final String GET_ALERT_SCRIPTING_CONTETNS = "GET_ALERT_SCRIPTING_CONTETNS";
	public static final String GET_ALERT_SCRIPTING_CONFIG_CONTETNS = "GET_ALERT_SCRIPTING_CONFIG_CONTETNS";
	public static final String SAVE_ALERT_SCRIPTING_CONTETNS = "SAVE_ALERT_SCRIPTING_CONTETNS";
	public static final String SAVE_ALERT_SCRIPTING_CONFIG_CONTETNS = "SAVE_ALERT_SCRIPTING_CONFIG_CONTETNS";
	public static final String GET_ALERT_SCRIPT_LOAD_MESSAGE = "GET_ALERT_SCRIPT_LOAD_MESSAGE";
	public static final String GET_ALERT_REAL_COUNTER_DESC = "GET_ALERT_REAL_COUNTER_DESC";

	public static final String GET_PLUGIN_HELPER_DESC = "GET_PLUGIN_HELPER_DESC";

	public static final String GET_XML_COUNTER = "GET_XML_COUNTER";

	public static final String CLUSTER_TEST1 = "CLUSTER_TEST1";

	public static final String EXPORT_OBJECT_TIME_COUNTER = "EXPORT_OBJECT_TIME_COUNTER";
	public static final String EXPORT_OBJECT_REGULAR_COUNTER = "EXPORT_OBJECT_REGULAR_COUNTER";
	public static final String EXPORT_APP_SUMMARY = "EXPORT_APP_SUMMARY";

	public static final String GET_STACK_ANALYZER = "GET_STACK_ANALYZER";
	public static final String GET_STACK_INDEX = "GET_STACK_INDEX";
	public static final String PSTACK_ON = "PSTACK_ON";

	// RDB Request
	public static final String ACTIVE_QUERY_LIST = "ACTIVE_QUERY_LIST";
	public static final String EXIST_QUERY_LIST = "EXIST_QUERY_LIST";
	public static final String LOAD_QUERY_LIST = "LOAD_QUERY_LIST";
	public static final String LOCK_LIST = "LOCK_LIST";
	public static final String DB_PROCESS_DETAIL = "DB_PROCESS_DETAIL";
	public static final String DB_EXPLAIN_PLAN = "DB_EXPLAIN_PLAN";
	public static final String DB_PROCESS_LIST = "DB_PROCESS_LIST";
	public static final String DB_VARIABLES = "DB_VARIABLES";
	public static final String DB_KILL_PROCESS = "DB_KILL_PROCESS";
	public static final String GET_INTERVAL_SNAPSHOT_TASK = "GET_INTERVAL_SNAPSHOT_TASK";
	public static final String CHANGE_INTERVAL_SNAPSHOT_TASK = "CHANGE_INTERVAL_SNAPSHOT_TASK";
	public static final String SCHEMA_SIZE_STATUS = "SCHEMA_SIZE_STATUS";
	public static final String TABLE_SIZE_STATUS = "TABLE_SIZE_STATUS";
	public static final String INNODB_STATUS = "INNODB_STATUS";
	public static final String GET_QUERY_INTERVAL = "GET_QUERY_INTERVAL";
	public static final String SET_QUERY_INTERVAL = "SET_QUERY_INTERVAL";
	public static final String SLAVE_STATUS = "SLAVE_STATUS";
	public static final String EXPLAIN_PLAN_FOR_THREAD = "EXPLAIN_PLAN_FOR_THREAD";
	public static final String USE_DATABASE = "USE_DATABASE";

	// Maria Plugin
	public static final String DB_REALTIME_CONNECTIONS = "DB_REALTIME_CONNECTIONS";
	public static final String DB_REALTIME_ACTIVITY = "DB_REALTIME_ACTIVITY";
	public static final String DB_DAILY_ACTIVITY = "DB_DAILY_ACTIVITY";
	public static final String DB_REALTIME_RESPONSE_TIME = "DB_REALTIME_RESPONSE_TIME";
	public static final String DB_REALTIME_HIT_RATIO = "DB_REALTIME_HIT_RATIO";
	public static final String DB_DAILY_CONNECTIONS = "DB_DAILY_CONNECTIONS";
	public static final String DB_DIGEST_TABLE = "DB_DIGEST_TABLE";
	public static final String DB_MAX_TIMER_WAIT_THREAD = "DB_MAX_TIMER_WAIT_THREAD";
	public static final String DB_LOAD_DIGEST_COUNTER = "DB_LOAD_DIGEST_COUNTER";
	public static final String DB_LAST_DIGEST_TABLE = "DB_LAST_DIGEST_TABLE";

	public static final int APPLY_CONFIGURE_WAS_RESULT_OK = 200;
	public static final int APPLY_CONFIGURE_WAS_RESULT_RUNNING = 300;

	public static final String APACHE_SERVER_STATUS = "APACHE_SERVER_STATUS";
	public static final String DUMP_APACHE_STATUS = "DUMP_APACHE_STATUS";

	public static final String REDIS_INFO = "REDIS_INFO";

	public static final String DEBUG_SERVER = "DEBUG_SERVER";
	public static final String DEBUG_AGENT = "DEBUG_AGENT";

	public static final String REALTIME_SERVICE_GROUP = "REALTIME_SERVICE_GROUP";

	public static final String STATUS_AROUND_VALUE = "STATUS_AROUND_VALUE";

	// MANAGE ACCOUNT
	public static final String LIST_ACCOUNT = "LIST_ACCOUNT";
	public static final String ADD_ACCOUNT = "ADD_ACCOUNT";
	public static final String CHECK_ACCOUNT_ID = "CHECK_ACCOUNT_ID";
	public static final String EDIT_ACCOUNT = "EDIT_ACCOUNT";
	public static final String REMOVE_ACCOUNT = "REMOVE_ACCOUNT";
	public static final String LIST_ACCOUNT_GROUP = "LIST_ACCOUNT_GROUP";
	public static final String GET_GROUP_POLICY_ALL = "GET_GROUP_POLICY_ALL";
	public static final String EDIT_GROUP_POLICY = "EDIT_GROUP_POLICY";
	public static final String ADD_ACCOUNT_GROUP = "ADD_ACCOUNT_GROUP";

	// MANAGE COUNTER
	public static final String DEFINE_OBJECT_TYPE = "DEFINE_OBJECT_TYPE";
	public static final String EDIT_OBJECT_TYPE = "EDIT_OBJECT_TYPE";

	// TAGCNT
	public static final String TAGCNT_DIV_NAMES = "TAGCNT_DIV_NAMES";
	public static final String TAGCNT_TAG_NAMES = "TAGCNT_TAG_NAMES";
	public static final String TAGCNT_TAG_VALUES = "TAGCNT_TAG_VALUES";
	public static final String TAGCNT_TAG_VALUE_DATA = "TAGCNT_TAG_VALUE_DATA";
	public static final String TAGCNT_TAG_ACTUAL_DATA = "TAGCNT_TAG_ACTUAL_DATA";

	// VISITOR
	public static final String VISITOR_REALTIME = "VISITOR_REALTIME";
	public static final String VISITOR_REALTIME_TOTAL = "VISITOR_REALTIME_TOTAL";
	public static final String VISITOR_REALTIME_GROUP = "VISITOR_REALTIME_GROUP";
	public static final String VISITOR_LOADDATE = "VISITOR_LOADDATE";
	public static final String VISITOR_LOADDATE_TOTAL = "VISITOR_LOADDATE_TOTAL";
	public static final String VISITOR_LOADDATE_GROUP = "VISITOR_LOADDATE_GROUP";
	public static final String VISITOR_LOADHOUR_GROUP = "VISITOR_LOADHOUR_GROUP";

	// SUMMARY
	public static final String LOAD_SERVICE_SUMMARY = "LOAD_SERVICE_SUMMARY";
	public static final String LOAD_SQL_SUMMARY = "LOAD_SQL_SUMMARY";
	public static final String LOAD_APICALL_SUMMARY = "LOAD_APICALL_SUMMARY";
	public static final String LOAD_IP_SUMMARY = "LOAD_IP_SUMMARY";
	public static final String LOAD_UA_SUMMARY = "LOAD_UA_SUMMARY";
	public static final String LOAD_SERVICE_ERROR_SUMMARY = "LOAD_SERVICE_ERROR_SUMMARY";
	public static final String LOAD_ALERT_SUMMARY = "LOAD_ALERT_SUMMARY";

	public static final String LOAD_ENDUSER_NAV_SUMMARY = "LOAD_ENDUSER_NAV_SUMMARY";
	public static final String LOAD_ENDUSER_AJAX_SUMMARY = "LOAD_ENDUSER_AJAX_SUMMARY";
	public static final String LOAD_ENDUSER_ERROR_SUMMARY = "LOAD_ENDUSER_ERROR_SUMMARY";
	
	// batch job
	public static final String BATCH_HISTORY_LIST = "BATCH_HISTORY_LIST";
	public static final String BATCH_HISTORY_DETAIL = "BATCH_HISTORY_DETAIL";
	public static final String BATCH_HISTORY_STACK = "BATCH_HISTORY_STACK";
	public static final String BATCH_ACTIVE_STACK = "BATCH_ACTIVE_STACK";
	public static final String OBJECT_BATCH_ACTIVE_LIST = "OBJECT_BATCH_ACTIVE_LIST";
	
	// CUBRID - AGENT
	public static final String CUBRID_DB_REALTIME_DML = "CUBRID_DB_REALTIME_DML";
	public static final String CUBRID_DB_REALTIME_STATUS = "CUBRID_DB_REALTIME_STATUS";
	public static final String CUBRID_ACTIVE_DB_LIST = "CUBRID_ACTIVE_DB_LIST";
	public static final String CUBRID_DB_SERVER_INFO = "CUBRID_DB_SERVER_INFO";
	public static final String CUBRID_DB_PERIOD_MULTI_DATA = "CUBRID_DB_PERIOD_MULTI_DATA";
	public static final String CUBRID_DB_LONG_PERIOD_MULTI_DATA = "CUBRID_DB_LONG_PERIOD_MULTI_DATA";
	public static final String CUBRID_DB_REALTIME_MULTI_DATA = "CUBRID_DB_REALTIME_MULTI_DATA";
	public static final String CUBRID_DB_LONG_TRANSACTION_DATA = "CUBRID_DB_LONG_TRANSACTION_DATA";
	public static final String CUBRID_GET_ALERT_CONFIGURE = "CUBRID_GET_ALERT_CONFIGURE";
	public static final String CUBRID_SET_ALERT_CONFIGURE = "CUBRID_SET_ALERT_CONFIGURE";

	protected static Set<String> freeCmdSet = new HashSet<String>();
	
	static {
		freeCmdSet.add(LOGIN);
		freeCmdSet.add(SERVER_VERSION);
		freeCmdSet.add(SERVER_TIME);
	}
	
	public static boolean isFreeCmd(String cmd) {
		return freeCmdSet.contains(cmd);
	}

}
