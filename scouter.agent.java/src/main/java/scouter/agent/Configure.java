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
import scouter.agent.util.JarUtil;
import scouter.lang.conf.ConfObserver;
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.ConfigValueType;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.conf.ValueType;
import scouter.lang.counters.CounterConstants;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

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

public class Configure extends Thread {
    public static boolean JDBC_REDEFINED = false;
    private static final Configure instance;
    private long last_load_time = -1;
    public Properties property = new Properties();
    private boolean running = true;
    private File propertyFile;
    long last_check = 0;
    public static String agent_dir_path;

    static {
        File jarFile = JarUtil.getThisJarFile();
        if (jarFile == null) {
            agent_dir_path = new File("./").getAbsolutePath();
        } else {
            agent_dir_path = jarFile.getParent();
        }

        instance = new Configure();
        instance.setDaemon(true);
        instance.setName(ThreadUtil.getName(instance));
        instance.start();
    }

    public static final Configure getInstance() {
        return instance;
    }

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

    @ConfigDesc("is it on kube env. auto detecting.")
    public boolean kube = isKube();
    public boolean obj_host_name_follow_host_agent = true;

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

    @ConfigDesc("turn off profile")
    public boolean profile_off = false;

    @ConfigDesc("Profiling the memory usage of each method")
    public boolean profile_thread_cputime_enabled = false;
    @ConfigDesc("Profiling the memory usage of each service")
    public boolean profile_thread_memory_usage_enabled = false;
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
    @ConfigDesc("reactor's important checkpoint scan depth")
    public int profile_reactor_checkpoint_search_depth = 5;
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
    @ConfigDesc("propagate b3 header")
    public boolean trace_propagete_b3_header = true;
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
    @ConfigDesc("Thread dump json format - only for virtual thread")
    public boolean thread_dump_json_format = false;
    //public File mgr_agent_lib_dir = new File("./_scouter_");
    @ConfigDesc("Script plugin enabled")
    public boolean plugin_enabled = true;

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

    @ConfigDesc("ends sxlog profile when it exceeds profile_force_end_stuck_millis.")
    public boolean profile_force_end_stuck_service = true;
    @ConfigDesc("alert when forcibly ends xlog profile.")
    public boolean profile_force_end_stuck_alert = true;
    @ConfigDesc("stuck service millis for forcibly ends xlog profile")
    public int profile_force_end_stuck_millis = 300000;
    @ConfigDesc("ignore metering when forcibly ends xlog profile.")
    public boolean profile_force_end_stuck_ignore_metering = false;

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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_sampling_rate_precision = 1;
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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_patterned_sampling_rate_precision = 1;
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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_patterned2_sampling_rate_precision = 1;
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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_patterned3_sampling_rate_precision = 1;
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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_patterned4_sampling_rate_precision = 1;
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
    @ConfigDesc("rate precision, default 1 means 1%. use 10 for 0.1%.")
    public int xlog_patterned5_sampling_rate_precision = 1;
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
    @ConfigDesc("Activating anonymous Method hooking")
    public boolean hook_method_anonymous_enable = true;

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

    @ConfigDesc("scouter weaver support hook enabled")
    public boolean hook_scouter_weaver_enabled = true;

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
    @ConfigDesc("PSTS(periodical stacktrace step) thread dump min time. it dumps only the elapsed time is over than this option")
    public int _psts_dump_min_ms = 1000;
    @ConfigDesc("PSTS(periodical stacktrace step) thread dump max count in 1 interval")
    public int _psts_dump_max_count = 100;
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

    //EndUser
    @ConfigDesc("Path to jsp to collect enduser data")
    public String enduser_trace_endpoint_url = "/_scouter_browser.jsp";

    //Experimental(ignoreset)
    public boolean __experimental = false;
    public boolean __control_connection_leak_autoclose_enabled = false;
    public boolean __ip_dummy_test = false;

    public Set<String> _profile_http_header_keys = null;

    //internal variables
    private String objExtType = "";
    private String objDetectedType = "";
    private int objHash;
    private String objName;
    private int objHostHash;
    private String objHostName;
    private Set<String> static_contents = new HashSet<String>();
    private StringSet log_ignore_set = new StringSet();
    private String[] _hook_method_ignore_prefix = null;
    private int _hook_method_ignore_prefix_len = 0;
    private int hook_signature;
    private StringSet _hook_method_ignore_classes = new StringSet();
    private int enduser_perf_endpoint_hash = HashUtil.hash(enduser_trace_endpoint_url);
    private StringSet custom_jmx_set = new StringSet();

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

    public File getPropertyFile() {
        if (propertyFile != null) {
            return propertyFile;
        }
        String s = System.getProperty("scouter.config", agent_dir_path + "/conf/scouter.conf");
        propertyFile = new File(s.trim());
        return propertyFile;
    }

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
        this.profile_spring_controller_method_parameter_enabled = getBoolean("profile_spring_controller_method_parameter_enabled", true);
        //this.profile_summary_mode_enabled = getBoolean("profile_summary_mode_enabled", false);

        this.profile_http_parameter_url_prefix = getValue("profile_http_parameter_url_prefix", "/");
        this.profile_http_header_url_prefix = getValue("profile_http_header_url_prefix", "/");
        this.profile_http_header_keys = getValue("profile_http_header_keys", "");
        this._profile_http_header_keys = StringUtil.splitAndTrimToSet(this.profile_http_header_keys, ',', true);

        this.xlog_lower_bound_time_ms = getInt("xlog_lower_bound_time_ms", 0);

        this.trace_service_name_header_key = getValue("trace_service_name_header_key", null);
        this.trace_service_name_get_key = getValue("trace_service_name_get_key");
        this.trace_service_name_post_key = getValue("trace_service_name_post_key");
        this.dump_dir = new File(getValue("dump_dir", agent_dir_path + "/dump"));
        this.thread_dump_json_format = getBoolean("thread_dump_json_format", false);
        try {
            this.dump_dir.mkdirs();
        } catch (Exception e) {
        }
//		this.mgr_agent_lib_dir = new File(getValue("mgr_agent_lib_dir", "./_scouter_"));
//		try {
//			this.mgr_agent_lib_dir.mkdirs();
//		} catch (Exception e) {
//		}
        this.plugin_dir = new File(getValue("plugin_dir", agent_dir_path + "/plugin"));
        this.plugin_enabled = getBoolean("plugin_enabled", true);

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
        this.autodump_stuck_check_interval_ms = getInt("autodump_stuck_check_interval_ms", 10000);

        this.profile_force_end_stuck_service = getBoolean("profile_force_end_stuck_service", false);
        this.profile_force_end_stuck_alert = getBoolean("profile_force_end_stuck_alert", true);
        this.profile_force_end_stuck_millis = getInt("profile_force_end_stuck_millis", 300000);

        this.autodump_cpu_exceeded_enabled = getBoolean("autodump_cpu_exceeded_enabled", false);
        this.autodump_cpu_exceeded_threshold_pct = getInt("autodump_cpu_exceeded_threshold_pct", 90);
        this.autodump_cpu_exceeded_duration_ms = getInt("autodump_cpu_exceeded_duration_ms", 30000);
        this.autodump_cpu_exceeded_dump_interval_ms = getInt("autodump_cpu_exceeded_dump_interval_ms", 3000);
        this.autodump_cpu_exceeded_dump_cnt = getInt("autodump_cpu_exceeded_dump_cnt", 3);

        this.mgr_static_content_extensions = getValue("mgr_static_content_extensions", "js, htm, html, gif, png, jpg, css");

        this.profile_off = getBoolean("profile_off", false);
        this.profile_thread_cputime_enabled = getBoolean("profile_thread_cputime_enabled", false);
        this.profile_thread_memory_usage_enabled = getBoolean("profile_thread_memory_usage_enabled", false);
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
        this.hook_get_connection_patterns = getValue("hook_get_connection_patterns","");
        
        this._log_datasource_lookup_enabled = getBoolean("_log_datasource_lookup_enabled", true);
        this.profile_connection_open_enabled = getBoolean("profile_connection_open_enabled", true);
        this._summary_connection_leak_fullstack_enabled = getBoolean("_summary_connection_leak_fullstack_enabled", false);
        this.hook_method_patterns = getValue("hook_method_patterns", "");
        this.hook_method_exclude_patterns = getValue("hook_method_exclude_patterns", "");
        this.hook_method_access_public_enabled = getBoolean("hook_method_access_public_enabled", true);
        this.hook_method_access_protected_enabled = getBoolean("hook_method_access_protected_enabled", false);
        this.hook_method_access_private_enabled = getBoolean("hook_method_access_private_enabled", false);
        this.hook_method_access_none_enabled = getBoolean("hook_method_access_none_enabled", false);
        this.hook_method_lambda_enable = getBoolean("hook_method_lambda_enable", true);
        this.hook_method_anonymous_enable = getBoolean("hook_method_anonymous_enable", true);

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
        this.hook_service_name_use_1st_string_enabled = getBoolean("hook_service_name_use_1st_string_enabled", true);
        this.hook_apicall_patterns = getValue("hook_apicall_patterns", "");
        this.hook_apicall_info_patterns = getValue("hook_apicall_info_patterns", "");
        this.hook_jsp_patterns = getValue("hook_jsp_patterns", "");

        this.hook_jdbc_pstmt_classes = getValue("hook_jdbc_pstmt_classes", "");
        this.hook_jdbc_stmt_classes = getValue("hook_jdbc_stmt_classes", "");
        this.hook_jdbc_rs_classes = getValue("hook_jdbc_rs_classes", "");
        this.hook_jdbc_wrapping_driver_patterns = getValue("hook_jdbc_wrapping_driver_patterns", "");
        this.hook_exception_class_patterns = getValue("hook_exception_class_patterns", "");
        this.hook_exception_exclude_class_patterns = getValue("hook_exception_exclude_class_patterns", "");
        if(StringUtil.isEmpty(this.hook_exception_exclude_class_patterns)) {
            //recover of previous version typo
            this.hook_exception_exclude_class_patterns = getValue("hook_exception_exlude_class_patterns", "");
        }
        this.hook_exception_handler_method_patterns = getValue("hook_exception_handler_method_patterns", "");
        this.hook_exception_handler_exclude_class_patterns = getValue("hook_exception_handler_exclude_class_patterns", "");
        if(StringUtil.isEmpty(this.hook_exception_handler_exclude_class_patterns)) {
            //recover of previous version typo
            this.hook_exception_handler_exclude_class_patterns = getValue("hook_exception_hanlder_exclude_class_patterns", "");
        }

        this.hook_async_servlet_enabled = getBoolean("_hook_async_servlet_enabled", true);

        this.hook_async_context_dispatch_patterns = getValue("hook_async_context_dispatch_patterns", "");
        this.hook_async_servlet_start_patterns = getValue("hook_async_servlet_start_patterns", "");

        this.hook_spring_async_submit_patterns = getValue("hook_spring_async_submit_patterns", "");
        this.hook_spring_async_enabled = getBoolean("hook_spring_async_enabled", true);

        this.hook_async_callrunnable_enable = getBoolean("hook_async_callrunnable_enable", true);
        this.hook_async_callrunnable_enabled =
                StringUtil.isEmpty(getValue("hook_async_callrunnable_enabled", ""))
                ? hook_async_callrunnable_enable
                : getBoolean("hook_async_callrunnable_enabled", true);

        this.hook_async_callrunnable_scan_package_prefixes = getValue("hook_async_callrunnable_scan_package_prefixes", "");

        this._hook_redis_set_key_patterns = getValue("_hook_redis_set_key_patterns", "");

        this.hook_async_thread_pool_executor_enabled = getBoolean("hook_async_thread_pool_executor_enabled", true);

        this.hook_hystrix_enabled = getBoolean("hook_hystrix_enabled", true);

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
        if (this.profile_step_max_count < 128)
            this.profile_step_max_count = 128;

        profile_step_max_keep_in_memory_count = getInt("profile_step_max_keep_in_memory_count", 2048);
        if (this.profile_step_max_count < 128)
            this.profile_step_max_count = 128;

        this._log_background_sql = getBoolean("_log_background_sql", false);
        this.profile_fullstack_service_error_enabled = getBoolean("profile_fullstack_service_error_enabled", false);
        this.profile_fullstack_apicall_error_enabled = getBoolean("profile_fullstack_apicall_error_enabled", false);
        this.profile_fullstack_sql_error_enabled = getBoolean("profile_fullstack_sql_error_enabled", false);
        this.profile_fullstack_sql_commit_enabled = getBoolean("profile_fullstack_sql_commit_enabled", false);
        this.profile_fullstack_hooked_exception_enabled = getBoolean("profile_fullstack_hooked_exception_enabled", false);
        this.profile_fullstack_redis_error_enabled = getBoolean("profile_fullstack_redis_error_enabled", false);
        this.profile_redis_key_forcibly_stringify_enabled = getBoolean("profile_redis_key_forcibly_stringify_enabled", false);

        this.profile_fullstack_max_lines = getInt("profile_fullstack_max_lines", 0);
        this.profile_fullstack_rs_leak_enabled = getBoolean("profile_fullstack_rs_leak_enabled", false);
        this.profile_fullstack_stmt_leak_enabled = getBoolean("profile_fullstack_stmt_leak_enabled", false);

        this.profile_elasticsearch_full_query_enabled = getBoolean("profile_elasticsearch_full_query_enabled", false);

        this.profile_reactor_checkpoint_enabled = getBoolean("profile_reactor_checkpoint_enabled", true);
        this.profile_reactor_checkpoint_search_depth = getInt("profile_reactor_checkpoint_search_depth", 5);
        this.profile_reactor_more_checkpoint_enabled = getBoolean("profile_reactor_more_checkpoint_enabled", false);

        this.net_udp_collection_interval_ms = getInt("net_udp_collection_interval_ms", 100);

        this.trace_http_client_ip_header_key = getValue("trace_http_client_ip_header_key", "");
        this.trace_interservice_enabled = getBoolean("trace_interservice_enabled", true);
        this.trace_propagete_b3_header = getBoolean("trace_propagete_b3_header", true);
        this.trace_response_gxid_enabled = getBoolean("trace_response_gxid_enabled", false);
        this._trace_interservice_gxid_header_key = getValue("_trace_interservice_gxid_header_key", "X-Scouter-Gxid");
        this._trace_interservice_callee_header_key = getValue("_trace_interservice_callee_header_key", "X-Scouter-Callee");
        this._trace_interservice_caller_header_key = getValue("_trace_interservice_caller_header_key", "X-Scouter-Caller");
        this._trace_interservice_caller_obj_header_key = getValue("_trace_interservice_caller_obj_header_key", "X-Scouter-Caller-Obj");
        this._trace_interservice_callee_obj_header_key = getValue("_trace_interservice_callee_obj_header_key", "X-Scouter-Callee-Obj");
        this.profile_connection_open_fullstack_enabled = getBoolean("profile_connection_open_fullstack_enabled", false);
        this.profile_connection_autocommit_status_enabled = getBoolean("profile_connection_autocommit_status_enabled", false);
        this.trace_user_mode = getInt("trace_user_mode", 2);
        this.trace_scouter_cookie_max_age = getInt("trace_scouter_cookie_max_age", Integer.MAX_VALUE);
        this.trace_user_cookie_path = getValue("trace_user_cookie_path", "/");
        this.trace_user_session_key = getValue("trace_user_session_key", "JSESSIONID");
        this._trace_auto_service_enabled = getBoolean("_trace_auto_service_enabled", false);
        this._trace_auto_service_backstack_enabled = getBoolean("_trace_auto_service_backstack_enabled", true);
        this.counter_enabled = getBoolean("counter_enabled", true);
        this._hook_serivce_enabled = getBoolean("_hook_serivce_enabled", true);
        this._hook_dbsql_enabled = getBoolean("_hook_dbsql_enabled", true);
        this._hook_dbconn_enabled = getBoolean("_hook_dbconn_enabled", true);
        this._hook_cap_enabled = getBoolean("_hook_cap_enabled", true);
        this._hook_methods_enabled = getBoolean("_hook_methods_enabled", true);
        this._hook_apicall_enabled = getBoolean("_hook_apicall_enabled", true);
        this._hook_socket_enabled = getBoolean("_hook_socket_enabled", true);
        this._hook_jsp_enabled = getBoolean("_hook_jsp_enabled", true);
        this._hook_async_enabled = getBoolean("_hook_async_enabled", true);
        this.trace_db2_enabled = getBoolean("trace_db2_enabled", true);
        this._hook_usertx_enabled = getBoolean("_hook_usertx_enabled", true);
        this._hook_spring_rest_enabled = getBoolean("_hook_spring_rest_enabled", true);
        this._hook_redis_enabled = getBoolean("_hook_redis_enabled", true);
        this._hook_kafka_enabled = getBoolean("_hook_kafka_enabled", true);
        this._hook_elasticsearch_enabled = getBoolean("_hook_elasticsearch_enabled", true);
        this.hook_mongodb_enabled = getBoolean("hook_mongodb_enabled", false);
        this._hook_rabbit_enabled = getBoolean("_hook_rabbit_enabled", true);
        this._hook_reactive_enabled = getBoolean("_hook_reactive_enabled", true);
        this._hook_coroutine_enabled = getBoolean("_hook_coroutine_enabled", true);
        this._hook_coroutine_debugger_hook_enabled = getBoolean("_hook_coroutine_debugger_hook_enabled", false);
        this._hook_thread_name_enabled = getBoolean("_hook_thread_name_enabled", false);

        this._hook_direct_patch_classes = getValue("_hook_direct_patch_classes", "");

        this._hook_boot_prefix = getValue("_hook_boot_prefix");
        this._hook_map_impl_enabled = getBoolean("_hook_map_impl_enabled", false);
        this._hook_map_impl_warning_size = getInt("_hook_map_impl_warning_size", 50000);

        this.counter_recentuser_valid_ms = getLong("counter_recentuser_valid_ms", DateUtil.MILLIS_PER_FIVE_MINUTE);
        this.counter_object_registry_path = getValue("counter_object_registry_path", "/tmp/scouter");
        this.counter_custom_jmx_enabled = getBoolean("counter_custom_jmx_enabled", false);
        this.counter_interaction_enabled = getBoolean("counter_interaction_enabled", false);
        this.custom_jmx_set = getStringSet("custom_jmx_set", "||");
        this.sfa_dump_enabled = getBoolean("sfa_dump_enabled", false);
        this.sfa_dump_interval_ms = getInt("sfa_dump_interval_ms", 10000);

        this._psts_enabled = getBoolean("_psts_enabled", false);
        this._psts_dump_interval_ms = getInt("_psts_dump_interval_ms", 10000);
        this._psts_dump_min_ms = getInt("_psts_dump_min_ms", 1000);
        this._psts_dump_max_count = getInt("_psts_dump_max_count", 100);
        this._psts_progressive_reactor_thread_trace_enabled = getBoolean("_psts_progressive_reactor_dump_enabled", true);

        // 웹시스템으로 부터 WAS 사이의 성능과 어떤 웹서버가 요청을 보내 왔는지를 추적하는 기능을 ON/OFF하고
        // 관련 키정보를 지정한다.
        this.trace_webserver_enabled = getBoolean("trace_webserver_enabled", false);
        this.trace_webserver_name_header_key = getValue("trace_webserver_name_header_key", "X-Forwarded-Host");
        this.trace_webserver_time_header_key = getValue("trace_webserver_time_header_key", "X-Forwarded-Time");

        this.trace_request_queuing_enabled = getBoolean("trace_request_queuing_enabled", false);
        this.trace_request_queuing_start_host_header = getValue("trace_request_queuing_start_host_header", "X-Request-Start-Host");
        this.trace_request_queuing_start_time_header = getValue("trace_request_queuing_start_time_header", "X-Request-Start-Time");
        this.trace_request_queuing_start_2nd_host_header = getValue("trace_request_queuing_start_2nd_host_header", "X-Request-Start-2nd-Host");
        this.trace_request_queuing_start_2nd_time_header = getValue("trace_request_queuing_start_2nd_time_header", "X-Request-Start-2nd-Time");

        this.trace_rs_leak_enabled = getBoolean("trace_rs_leak_enabled", false);
        this.trace_stmt_leak_enabled = getBoolean("trace_stmt_leak_enabled", false);

        this.trace_delayed_service_mgr_filename = getValue("trace_delayed_service_mgr_filename", "setting_delayed_service.properties");

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
        this.__control_connection_leak_autoclose_enabled = getBoolean("__control_connection_leak_autoclose_enabled", false);

        //For testing
        this.__ip_dummy_test = getBoolean("__ip_dummy_test", false);

        this.alert_perm_warning_pct = getInt("alert_perm_warning_pct", 90);
        this.alert_message_length = getInt("alert_message_length", 3000);
        this.alert_send_interval_ms = getInt("alert_send_interval_ms", 10000);

        this.xlog_error_jdbc_fetch_max = getInt("xlog_error_jdbc_fetch_max", 10000);
        this.xlog_error_sql_time_max_ms = getInt("xlog_error_sql_time_max_ms", 30000);
        this.xlog_error_on_sqlexception_enabled = getBoolean("xlog_error_on_sqlexception_enabled", true);
        this.xlog_error_on_apicall_exception_enabled = getBoolean("xlog_error_on_apicall_exception_enabled", true);
        this.xlog_error_on_redis_exception_enabled = getBoolean("xlog_error_on_redis_exception_enabled", true);
        this.xlog_error_on_elasticsearch_exception_enabled = getBoolean("xlog_error_on_elasticsearch_exception_enabled", true);
        this.xlog_error_on_mongodb_exception_enabled = getBoolean("xlog_error_on_mongodb_exception_enabled", true);

        this._log_asm_enabled = getBoolean("_log_asm_enabled", false);
        this.obj_type_inherit_to_child_enabled = getBoolean("obj_type_inherit_to_child_enabled", false);
        this.jmx_counter_enabled = getBoolean("jmx_counter_enabled", false);
        this._profile_fullstack_sql_connection_enabled = getBoolean("_profile_fullstack_sql_connection_enabled", false);
        this._profile_fullstack_sql_execute_debug_enabled = getBoolean("_profile_fullstack_sql_execute_debug_enabled", false);
        this._trace_fullstack_socket_open_port = getInt("_trace_fullstack_socket_open_port", 0);
        this._trace_sql_parameter_max_count = getInt("_trace_sql_parameter_max_count", 128);
        this.trace_sql_parameter_max_length = Math.min(getInt("trace_sql_parameter_max_length", 20), 500);
        this.log_dir = getValue("log_dir", "");
        this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
        this.log_keep_days = getInt("log_keep_days", 7);
        this._trace = getBoolean("_trace", false);
        this._trace_use_logger = getBoolean("_trace_use_logger", false);

        this.enduser_trace_endpoint_url = getValue("enduser_trace_endpoint_url", "_scouter_browser.jsp");
        this.enduser_perf_endpoint_hash = HashUtil.hash(this.enduser_trace_endpoint_url);

        this.xlog_error_check_user_transaction_enabled = getBoolean("xlog_error_check_user_transaction_enabled", true);

        this._xlog_hard_sampling_enabled = getBoolean("_xlog_hard_sampling_enabled", false);
        this._xlog_hard_sampling_rate_pct = getInt("_xlog_hard_sampling_rate_pct", 10);

        this.ignore_global_consequent_sampling = getBoolean("ignore_global_consequent_sampling", false);
        this.xlog_consequent_sampling_ignore_patterns = getValue("xlog_consequent_sampling_ignore_patterns", "");

        this.xlog_sampling_exclude_patterns = getValue("xlog_sampling_exclude_patterns", "");

        this.xlog_sampling_enabled = getBoolean("xlog_sampling_enabled", false);
        this.xlog_sampling_rate_precision = getInt("xlog_sampling_rate_precision", 1);
        this.xlog_sampling_only_profile = getBoolean("xlog_sampling_only_profile", false);
        this.xlog_sampling_step1_ms = getInt("xlog_sampling_step1_ms", 100);
        this.xlog_sampling_step1_rate_pct = getInt("xlog_sampling_step1_rate_pct", 3);
        this.xlog_sampling_step2_ms = getInt("xlog_sampling_step2_ms", 1000);
        this.xlog_sampling_step2_rate_pct = getInt("xlog_sampling_step2_rate_pct", 10);
        this.xlog_sampling_step3_ms = getInt("xlog_sampling_step3_ms", 3000);
        this.xlog_sampling_step3_rate_pct = getInt("xlog_sampling_step3_rate_pct", 30);
        this.xlog_sampling_over_rate_pct = getInt("xlog_sampling_over_rate_pct", 100);

        this.xlog_patterned_sampling_enabled = getBoolean("xlog_patterned_sampling_enabled", false);
        this.xlog_patterned_sampling_rate_precision = getInt("xlog_patterned_sampling_rate_precision", 1);
        this.xlog_patterned_sampling_service_patterns = getValue("xlog_patterned_sampling_service_patterns", "");
        this.xlog_patterned_sampling_only_profile = getBoolean("xlog_patterned_sampling_only_profile", false);
        this.xlog_patterned_sampling_step1_ms = getInt("xlog_patterned_sampling_step1_ms", 100);
        this.xlog_patterned_sampling_step1_rate_pct = getInt("xlog_patterned_sampling_step1_rate_pct", 3);
        this.xlog_patterned_sampling_step2_ms = getInt("xlog_patterned_sampling_step2_ms", 1000);
        this.xlog_patterned_sampling_step2_rate_pct = getInt("xlog_patterned_sampling_step2_rate_pct", 10);
        this.xlog_patterned_sampling_step3_ms = getInt("xlog_patterned_sampling_step3_ms", 3000);
        this.xlog_patterned_sampling_step3_rate_pct = getInt("xlog_patterned_sampling_step3_rate_pct", 30);
        this.xlog_patterned_sampling_over_rate_pct = getInt("xlog_patterned_sampling_over_rate_pct", 100);

        this.xlog_patterned2_sampling_enabled = getBoolean("xlog_patterned2_sampling_enabled", false);
        this.xlog_patterned2_sampling_rate_precision = getInt("xlog_patterned2_sampling_rate_precision", 1);
        this.xlog_patterned2_sampling_service_patterns = getValue("xlog_patterned2_sampling_service_patterns", "");
        this.xlog_patterned2_sampling_only_profile = getBoolean("xlog_patterned2_sampling_only_profile", false);
        this.xlog_patterned2_sampling_step1_ms = getInt("xlog_patterned2_sampling_step1_ms", 100);
        this.xlog_patterned2_sampling_step1_rate_pct = getInt("xlog_patterned2_sampling_step1_rate_pct", 3);
        this.xlog_patterned2_sampling_step2_ms = getInt("xlog_patterned2_sampling_step2_ms", 1000);
        this.xlog_patterned2_sampling_step2_rate_pct = getInt("xlog_patterned2_sampling_step2_rate_pct", 10);
        this.xlog_patterned2_sampling_step3_ms = getInt("xlog_patterned2_sampling_step3_ms", 3000);
        this.xlog_patterned2_sampling_step3_rate_pct = getInt("xlog_patterned2_sampling_step3_rate_pct", 30);
        this.xlog_patterned2_sampling_over_rate_pct = getInt("xlog_patterned2_sampling_over_rate_pct", 100);

        this.xlog_patterned3_sampling_enabled = getBoolean("xlog_patterned3_sampling_enabled", false);
        this.xlog_patterned3_sampling_rate_precision = getInt("xlog_patterned3_sampling_rate_precision", 1);
        this.xlog_patterned3_sampling_service_patterns = getValue("xlog_patterned3_sampling_service_patterns", "");
        this.xlog_patterned3_sampling_only_profile = getBoolean("xlog_patterned3_sampling_only_profile", false);
        this.xlog_patterned3_sampling_step1_ms = getInt("xlog_patterned3_sampling_step1_ms", 100);
        this.xlog_patterned3_sampling_step1_rate_pct = getInt("xlog_patterned3_sampling_step1_rate_pct", 3);
        this.xlog_patterned3_sampling_step2_ms = getInt("xlog_patterned3_sampling_step2_ms", 1000);
        this.xlog_patterned3_sampling_step2_rate_pct = getInt("xlog_patterned3_sampling_step2_rate_pct", 10);
        this.xlog_patterned3_sampling_step3_ms = getInt("xlog_patterned3_sampling_step3_ms", 3000);
        this.xlog_patterned3_sampling_step3_rate_pct = getInt("xlog_patterned3_sampling_step3_rate_pct", 30);
        this.xlog_patterned3_sampling_over_rate_pct = getInt("xlog_patterned3_sampling_over_rate_pct", 100);

        this.xlog_patterned4_sampling_enabled = getBoolean("xlog_patterned4_sampling_enabled", false);
        this.xlog_patterned4_sampling_rate_precision = getInt("xlog_patterned4_sampling_rate_precision", 1);
        this.xlog_patterned4_sampling_service_patterns = getValue("xlog_patterned4_sampling_service_patterns", "");
        this.xlog_patterned4_sampling_only_profile = getBoolean("xlog_patterned4_sampling_only_profile", false);
        this.xlog_patterned4_sampling_step1_ms = getInt("xlog_patterned4_sampling_step1_ms", 100);
        this.xlog_patterned4_sampling_step1_rate_pct = getInt("xlog_patterned4_sampling_step1_rate_pct", 3);
        this.xlog_patterned4_sampling_step2_ms = getInt("xlog_patterned4_sampling_step2_ms", 1000);
        this.xlog_patterned4_sampling_step2_rate_pct = getInt("xlog_patterned4_sampling_step2_rate_pct", 10);
        this.xlog_patterned4_sampling_step3_ms = getInt("xlog_patterned4_sampling_step3_ms", 3000);
        this.xlog_patterned4_sampling_step3_rate_pct = getInt("xlog_patterned4_sampling_step3_rate_pct", 30);
        this.xlog_patterned4_sampling_over_rate_pct = getInt("xlog_patterned4_sampling_over_rate_pct", 100);

        this.xlog_patterned5_sampling_enabled = getBoolean("xlog_patterned5_sampling_enabled", false);
        this.xlog_patterned5_sampling_rate_precision = getInt("xlog_patterned5_sampling_rate_precision", 1);
        this.xlog_patterned5_sampling_service_patterns = getValue("xlog_patterned5_sampling_service_patterns", "");
        this.xlog_patterned5_sampling_only_profile = getBoolean("xlog_patterned5_sampling_only_profile", false);
        this.xlog_patterned5_sampling_step1_ms = getInt("xlog_patterned5_sampling_step1_ms", 100);
        this.xlog_patterned5_sampling_step1_rate_pct = getInt("xlog_patterned5_sampling_step1_rate_pct", 3);
        this.xlog_patterned5_sampling_step2_ms = getInt("xlog_patterned5_sampling_step2_ms", 1000);
        this.xlog_patterned5_sampling_step2_rate_pct = getInt("xlog_patterned5_sampling_step2_rate_pct", 10);
        this.xlog_patterned5_sampling_step3_ms = getInt("xlog_patterned5_sampling_step3_ms", 3000);
        this.xlog_patterned5_sampling_step3_rate_pct = getInt("xlog_patterned5_sampling_step3_rate_pct", 30);
        this.xlog_patterned5_sampling_over_rate_pct = getInt("xlog_patterned5_sampling_over_rate_pct", 100);

        this.xlog_discard_service_patterns = getValue("xlog_discard_service_patterns", "");
        this.xlog_discard_service_show_error = getBoolean("xlog_discard_service_show_error", true);
        this.xlog_fully_discard_service_patterns = getValue("xlog_fully_discard_service_patterns", "");

        resetObjInfo();
        setStaticContents();
    }

    public String getObjExtType() {
        return this.objExtType;
    }

    public void setObjExtType(String objExtType) {
        this.objExtType = objExtType;
    }

    public String getObjDetectedType() {
        return this.objDetectedType;
    }

    public void setObjDetectedType(String objDetectedType) {
        this.objDetectedType = objDetectedType;
    }

    public int getObjHash() {
        return this.objHash;
    }

    public String getObjName() {
        return this.objName;
    }

    public int getObjHostHash() {
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

    public StringSet getCustomJmxSet() {
        return this.custom_jmx_set;
    }

    public boolean isIgnoreMethodClass(String classname) {
        return _hook_method_ignore_classes.hasKey(classname);
    }

    public synchronized void resetObjInfo() {
        String detected = ObjTypeDetector.drivedType != null ? ObjTypeDetector.drivedType
                : ObjTypeDetector.objType != null ? ObjTypeDetector.objType : CounterConstants.JAVA;

        this.objDetectedType = detected;
        this.monitoring_group_type = getValue("monitoring_group_type");
        this.obj_type = StringUtil.isEmpty(this.monitoring_group_type) ? getValue("obj_type", detected) : this.monitoring_group_type;
        this.objExtType = ObjTypeDetector.objExtType;

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

        this.kube = getBoolean("kube", isKube());
        this.obj_host_name_follow_host_agent = getBoolean("obj_host_name_follow_host_agent", true);

        String tempObjHostName = getValue("obj_host_name", SysJMX.getHostName());
        if (kube && obj_host_name_follow_host_agent) {
            String nameFromHost = readHostNameFromHostAgent();
            if (StringUtil.isNotEmpty(nameFromHost) && nameFromHost.length() > 1 && nameFromHost.length() < 100) {
                tempObjHostName = nameFromHost;
            }
        }

        this.obj_host_name = tempObjHostName;
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
        System.setProperty("scouter.dir", agent_dir_path);
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
        ignoreSet.add("_profile_http_header_keys");
        //ignoreSet.add("obj_type");
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

    public int getHookSignature() {
        return this.hook_signature;
    }

    private File regRoot = null;

    public void initTmpDir() {
        try {
            if (regRoot != null) {
                return;
            }
            String objReg = counter_object_registry_path;
            File objRegFile = new File(objReg);
            if (!objRegFile.canRead()) {
                objRegFile.mkdirs();
            }
            if (objRegFile.exists()) {
                regRoot = objRegFile;
            } else {
                regRoot = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isKube() {
        Map<String, String> env = System.getenv();
        return !StringUtil.isEmpty(env.get("KUBERNETES_SERVICE_HOST"));
    }

    private String readHostNameFromHostAgent() {
        try {
            initTmpDir();
            File dir = regRoot;
            if (dir == null)
                return null;

            File[] kubeHostSeq = dir.listFiles();
            for (int i = 0; i < kubeHostSeq.length; i++) {
                if (kubeHostSeq[i].isDirectory())
                    continue;
                String name = kubeHostSeq[i].getName();
                if (!name.endsWith(".scouterkubeseq")) {
                    continue;
                }
                int kubeSeq = cintErrorMinusOne(name.substring(0, name.lastIndexOf(".")));
                if (kubeSeq < 0)
                    continue;

                return new String(FileUtil.readAll(kubeHostSeq[i]));
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int cintErrorMinusOne(String value) {
        if (value == null) {
            return -1;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return -1;
            }
        }
    }


    public static void main(String[] args) {
        Configure o = new Configure(true);
        StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(o);
        StringKeyLinkedMap<String> descMap = ConfigValueUtil.getConfigDescMap(o);
        StringEnumer enu = defMap.keys();
        while (enu.hasMoreElements()) {
            String key = enu.nextString();
            if (ignoreSet.contains(key))
                continue;
            System.out.println(key + " : " + ConfigValueUtil.toValue(defMap.get(key)) + (descMap.containsKey(key) ? " (" + descMap.get(key) + ")" : ""));
        }
    }
}
