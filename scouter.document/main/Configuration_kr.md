# Configuration
[![Englsh](https://img.shields.io/badge/language-English-orange.svg)](Configuration.md) ![Korean](https://img.shields.io/badge/language-Korean-blue.svg)

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
//Dir
@ConfigDesc("Store directory of database")
public String db_dir = "./database";
@ConfigDesc("Path to log directory")
public String log_dir = "./logs";
@ConfigDesc("Path to plugin directory")
public String plugin_dir = "./plugin";
@ConfigDesc("Path to client related directory")
public String client_dir = "./client";

//Log
@ConfigDesc("Retaining log according to date")
public boolean log_rotation_enabled = true;
@ConfigDesc("Keeping period of log")
public int log_keep_days = 31;

//Network
@ConfigDesc("UDP Port")
public int net_udp_listen_port = 6100;
@ConfigDesc("TCP Port")
public int net_tcp_listen_port = 6100;
@ConfigDesc("Http Port for scouter-pulse")
public int net_http_port = 6180;

//Manager
@ConfigDesc("Activating automatic deletion function in the database")
public boolean mgr_purge_enabled = true;
@ConfigDesc("Automatic deletion only for XLog data")
public boolean mgr_purge_only_xlog_enabled = false;
@ConfigDesc("Condition of disc usage for automatic deletion")
public int mgr_purge_disk_usage_pct = 80;
@ConfigDesc("Retaining date for automatic deletion")
public int mgr_purge_keep_days = 0;

//GeoIP
@ConfigDesc("Activating IP-based city/country extraction")
public boolean geoip_enabled = true;
@ConfigDesc("Path to GeoIP data file")
public String geoip_data_city_file = CONF_DIR + "GeoLiteCity.dat";
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
@ConfigDesc("Collector IP")
public String net_collector_ip = "127.0.0.1";
@ConfigDesc("Collector UDP Port")
public int net_collector_udp_port = 6100;
@ConfigDesc("Collector TCP Port")
public int net_collector_tcp_port = 6100;

@ConfigDesc("Activating SQL literals")
public boolean profile_sql_escape_enabled = true;

//profile
@ConfigDesc("Http Query String profile")
public boolean profile_http_querystring_enabled;
@ConfigDesc("Http Header profile")
public boolean profile_http_header_enabled;
@ConfigDesc("Service URL prefix for Http header profile")
public String profile_http_header_url_prefix = "/";
@ConfigDesc("Calculating CPU time by profile")
public boolean profile_thread_cputime_enabled = false;

//Trace
@ConfigDesc("Adding assigned header value to the service name")
public String trace_service_name_header_key;
@ConfigDesc("Adding assigned get parameter to the service name")
public String trace_service_name_get_key;
@ConfigDesc("Adding assigned post parameter to the service name")
public String trace_service_name_post_key;

@ConfigDesc("warning color marking threshold duration(ms) on active service view")
public long trace_activeserivce_yellow_time = 3000;
@ConfigDesc("fatal color marking threshold duration(ms) on active service view")
public long trace_activeservice_red_time = 8000;

@ConfigDesc("Identifying header key of Remote IP")
public String trace_http_client_ip_header_key = "";

@ConfigDesc("Activating gxid connection in HttpTransfer")
public boolean trace_interservice_enabled = false;

@ConfigDesc("Session key for user counting")
public String trace_user_session_key = "JSESSIONID";

@ConfigDesc("")
public String trace_delayed_service_mgr_filename = "setting_delayed_service.properties";

//Dir
@ConfigDesc("Plugin directory")
public File plugin_dir = new File(agent_dir_path + "/plugin");
@ConfigDesc("Dump directory")
public File dump_dir = new File(agent_dir_path + "/dump");
//public File mgr_agent_lib_dir = new File("./_scouter_");

//Auto dump options when active service count is exceed the set threshold.
@ConfigDesc("Activating auto dump - append dumps onto the dump file in dump directory.")
public boolean autodump_enabled = false;
@ConfigDesc("Auto dump trigger point (dump when exceeding this active service count)")
public int autodump_trigger_active_service_cnt = 10000;
@ConfigDesc("Minimum interval(ms) for operating auto dump function - hard min : 5000")
public long autodump_interval_ms = 30000;
@ConfigDesc("Auto dump level (1 : ThreadDump, 2 : active service, 3 : thread list)")
public int autodump_level = 1; // 1:ThreadDump, 2:ActiveService, 3:ThreadList

//generate thread dump if stucked threads exsist
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

//Log
@ConfigDesc("Log directory")
public String log_dir = "";
@ConfigDesc("Retaining log according to date")
public boolean log_rotation_enabled = true;
@ConfigDesc("Keeping period of log")
public int log_keep_days = 7;

//Detect spring Rest url
@ConfigDesc("use @RequestMapping value as service name on a spring REST web appplicaiton.")
public boolean _hook_spring_rest_enabled = false;

//Hook method
@ConfigDesc("Method set for method hooking")
public String hook_method_patterns = "";

@ConfigDesc("Prefix ignrore Method hooking")
public String hook_method_ignore_prefixes = "get,set";
@ConfigDesc("Class set without Method hookingt")
public String hook_method_ignore_classes = "";
@ConfigDesc("")
public String hook_method_exclude_patterns = "";
@ConfigDesc("Activating public Method hooking")
public boolean hook_method_access_public_enabled = true;
@ConfigDesc("Activating private Method hooking")
public boolean hook_method_access_private_enabled = false;
@ConfigDesc("Activating protected Method hooking")
public boolean hook_method_access_protected_enabled = false;
@ConfigDesc("Activating default Method hooking")
public boolean hook_method_access_none_enabled = false;

//this option should be used only if the apllication is non-servlet.
//In case of servlet web application, detect HttpServlet.service() method as hook-service-patterns automatically.
@ConfigDesc("Method set for service hooking")
public String hook_service_patterns = "";

//Hook options for pulgin
@ConfigDesc("Method set for argument hooking")
public String hook_args_patterns = "";
@ConfigDesc("Method set for return hooking")
public String hook_return_patterns = "";
@ConfigDesc("Method set for constructor hooking")
public String hook_constructor_patterns = "";

//XLog
@ConfigDesc("XLog Ignore Time - (deprecated) for backward compatibility. Use xlog_sampling_xxx options instead")
public int xlog_lower_bound_time_ms = 0;
@ConfigDesc("Leave an error message at XLog in case of over fetching. (fetch count)")
public int xlog_error_jdbc_fetch_max = 10000;
@ConfigDesc("Leave an error message at XLog in case of over timing query. (ms)")
public int xlog_error_sql_time_max_ms = 30000;
@ConfigDesc("Leave an error message at XLog when UserTransaction's begin/end unpaired")
public boolean xlog_error_check_user_transaction_enabled = true;

//XLog hard sampling options
@ConfigDesc("XLog hard sampling mode enabled - for the best performance but it affects all statistics data")
public boolean _xlog_hard_sampling_enabled = false;
@ConfigDesc("XLog hard sampling rate(%) - discard data over the percentage")
public int _xlog_hard_sampling_rate_pct = 10;

//XLog soft sampling options
@ConfigDesc("XLog sampling mode enabled")
public boolean xlog_sampling_enabled = false;
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

// SFA(Stack Frequency Analyzer)
@ConfigDesc("Activating period threaddump function")
public boolean sfa_dump_enabled = false;
@ConfigDesc("SFA thread dump Interval(ms)")
public int sfa_dump_interval_ms = 10000;

//miscellaneous
@ConfigDesc("User ID based(0 : Remote Address, 1 : JSessionID, 2 : Scouter Cookie)")
public int trace_user_mode = 2; // 0:Remote IP, 1:JSessionID, 2:SetCookie

@ConfigDesc("Path to file creation directory of process ID file")
public String counter_object_registry_path = "/tmp/scouter";

@ConfigDesc("think time (ms) of recent user")
public long counter_recentuser_valid_ms = DateUtil.MILLIS_PER_FIVE_MINUTE;

@ConfigDesc("PermGen usage for send alert")
public int alert_perm_warning_pct = 90;

@ConfigDesc("")
public String mgr_static_content_extensions = "js, htm, html, gif, png, jpg, css";
```

## Host agent options
 * **Useful options of host agent**
   (It's not every option. you can get whole options on the source code that the file name is Configure.java.)
  * you can use the variable name as the property name you want to set in configuration file.
   ```(aka in the file scouter.conf by default)```

   * example
   ```properties
   net_colector_ip = 127.0.0.1
   cpu_alert_enabled = false
   ```
   
```java
//Network
@ConfigDesc("Collector IP")
public String net_collector_ip = "127.0.0.1";
@ConfigDesc("Collector UDP Port")
public int net_collector_udp_port = 6100;
@ConfigDesc("Collector TCP Port")
public int net_collector_tcp_port = 6100;

//Log
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
