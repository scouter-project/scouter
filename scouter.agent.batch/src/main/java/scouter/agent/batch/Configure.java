/*
 *  Copyright 2016 the original author or authors. 
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
package scouter.agent.batch;

import scouter.agent.batch.util.JarUtil;
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.ConfigValueType;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.conf.ValueType;
import scouter.lang.counters.CounterConstants;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

public class Configure {
	public static final String CONFIG_SCOUTER_ENABLED = "scouter_enabled";
	public static final String VM_SCOUTER_ENABLED = "scouter.enabled";
	public static boolean JDBC_REDEFINED = false;
	private static Configure instance = null;
	
	public Properties property = new Properties();
    private File propertyFile;
    public static String agent_dir_path;
    static {
    	File jarFile = JarUtil.getThisJarFile();
    	if(jarFile != null){
    		agent_dir_path = jarFile.getParent();
    	}else{
    		agent_dir_path = new File(".").getAbsolutePath();
    	}
    }

    public final static synchronized Configure getInstance() {
		if (instance == null) {
			instance = new Configure();
		}
		return instance;
	}
    
    // Scouter enable/disable
    @ConfigDesc("Enable scouter monitor")
    public boolean scouter_enabled = true;
    public boolean scouter_stop = false;
    
    // Standalone
    @ConfigDesc("Standalone mode")
    public boolean scouter_standalone = false;
    
    // Batch basic configuration
    @ConfigDesc("Batch ID type(class,args, props)")
    public String batch_id_type = "class"; // Class, Args, Props 
    @ConfigDesc("Batch ID(args-index number, props-key string)")
    public String batch_id = "";
        
    // SQL
    @ConfigDesc("Collect sql statistics")
    public boolean sql_enabled = true;
    @ConfigDesc("SQL max count")
    public int sql_max_count = 100;
    @ConfigDesc("Method set for preparestatement hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
    public String hook_jdbc_pstmt_classes = "";
    @ConfigDesc("Method set for statement hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String hook_jdbc_stmt_classes = "";
    @ConfigDesc("Method set for resultset hooking")
    @ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String hook_jdbc_rs_classes = "";    
    
    // SFA(Stack Frequency Analyzer) Thread Dump
    @ConfigDesc("Stack dump collector")
	public boolean sfa_dump_enabled = true;
    @ConfigDesc("Stack dump interval(ms)")
	public int sfa_dump_interval_ms = 10000;
    @ConfigDesc("Stack dump filter(,)")
	public String [] sfa_dump_filter = null;
    @ConfigDesc("Stack dump directory")
	public File sfa_dump_dir = new File(agent_dir_path + "/dump");
    @ConfigDesc("Add Stack dump header")
	public boolean sfa_dump_header_exists = true;
	
	// dump send time
    @ConfigDesc("Batch elapsed time(millisecond) to send SFA dump file to scouter server")
	public long sfa_dump_send_elapsed_ms = 30000L;
    @ConfigDesc("Batch elapsed time(millisecond) to send batch log to scouter server")
	public long batch_log_send_elapsed_ms = 30000L;
	
    // Thread Live Check추가
    @ConfigDesc("Thread check interval time(millisecond)")
    public long thread_check_interval_ms = 1000L;
    
	//Network
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
    @ConfigDesc("Local UDP Port")
 	public int net_local_udp_port = NetConstants.LOCAL_UDP_PORT;
    @ConfigDesc("UDP Buffer Size")
	public int net_udp_packet_max_bytes = 60000;
    @ConfigDesc("UDP Collection Interval(ms)")
	public long net_udp_collection_interval_ms = 100;
    @ConfigDesc("Stack Log TCP Session Count")
	public int net_tcp_stack_session_count = 1;
    
	//Object
    @ConfigDesc("Object Type")
	public String obj_type = CounterConstants.BATCH;
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

	//Dir
    @ConfigDesc("Plugin directory")
	public File plugin_dir = new File(agent_dir_path + "/plugin");
	//public File mgr_agent_lib_dir = new File("./_scouter_");

	//Log
    @ConfigDesc("")
	public boolean _log_asm_enabled;
	
    @ConfigDesc("Log directory")
	public String log_dir ="";
    @ConfigDesc("Retaining log according to date")
	public boolean log_rotation_enabled =true;
    @ConfigDesc("Keeping period of log")
	public int log_keep_days =7;
    @ConfigDesc("Leave sbr log even when not in standalone mode")
	public boolean sbr_log_make = false;
    @ConfigDesc("")
	public boolean _trace = false;
    @ConfigDesc("")
    public boolean _trace_use_logger = false;

	//internal variables
	private int objHash;
	private String objName;
	private int objHostHash;
	private String objHostName;

	private StringSet log_ignore_set = new StringSet();

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
		reload();
	}
	
	private Configure(boolean b) {
	}
	
	public File getPropertyFile() {
		if (propertyFile != null) {
			return propertyFile;
		}
		String s = System.getProperty("scouter.config", agent_dir_path + "/conf/scouter.batch.conf");
		propertyFile = new File(s.trim());
		return propertyFile;
	}

	public void reload() {
		File file = getPropertyFile();
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
		
		Logger.println(CONFIG_SCOUTER_ENABLED + "=" + this.scouter_enabled);
		Logger.println("scouter_standalone=" + this.scouter_standalone);
		Logger.println("sql_enabled=" + this.sql_enabled);
		Logger.println("sfa_dump_enabled=" + this.sql_enabled);
		if(sfa_dump_enabled){
			Logger.println("sfa_dump_dir=" + this.sfa_dump_dir.getAbsolutePath());
		}
	}

	private void apply() {
		// enable or disable
		this.scouter_enabled = getBoolean(CONFIG_SCOUTER_ENABLED, true);
		if(getValue(VM_SCOUTER_ENABLED) != null){
			this.scouter_enabled = getBoolean(VM_SCOUTER_ENABLED, true);
		}
		
		// standalone mode
		this.scouter_standalone = getBoolean("scouter_standalone", true);
		
		// start for batch
		this.batch_id_type = getValue("batch_id_type", "class");
		if("class".equals(this.batch_id_type)){
			this.batch_id = getValue("batch_id", "");
		}else if("args".equals(this.batch_id_type)){
			this.batch_id = getValue("batch_id", "0");		
		}else if("props".equals(this.batch_id_type)){
			this.batch_id = getValue("batch_id", "JobId");			
		}
		
		// SQL
		this.sql_max_count = getInt("sql_max_count", 100);
		
		// Batch Dump
		this.sfa_dump_interval_ms = getInt("sfa_dump_interval_ms", 10000);
		if (this.sfa_dump_interval_ms < 5000) {
			this.sfa_dump_interval_ms = 5000;
		}
		String value = getValue("sfa_dump_filter");
		if(value != null){
			String [] arrs = StringUtil.split(value, ',');
			if(arrs != null && arrs.length > 0){
				ArrayList<String> lists = new ArrayList<String>();
				for(String line:arrs){
					line = line.trim();
					if(line.length() == 0){
						continue;
					}
					lists.add(line);
				}
				if(lists.size() > 0){
					this.sfa_dump_filter = new String[lists.size()];
					for(int i=0; i < lists.size();i++){
						this.sfa_dump_filter[i] = lists.get(i);
					}
				}
			}
		}
		
		this.sfa_dump_enabled = getBoolean("sfa_dump_enabled", true);
		if(this.sfa_dump_enabled){
			value = getValue("sfa_dump_dir", agent_dir_path + "/dump");
			File dir = new File(value);
			if(!dir.exists()){
				try {
					dir.mkdirs();
				}catch(Exception ex){}
			}
			
			if(dir.isFile()){
				this.sfa_dump_enabled = false;				
				System.err.println("sfa_dump_dir(" + dir.getAbsolutePath() + ") is file");				
			}
			
			if(!dir.canWrite()){
				this.sfa_dump_enabled = false;
				System.err.println("sfa_dump_dir(" + dir.getAbsolutePath() + ") can't write");				
			}
			
			this.sfa_dump_dir = dir;
			this.sfa_dump_header_exists = getBoolean("sfa_dump_header_exists", true);
		}
		this.sfa_dump_send_elapsed_ms = getLong("sfa_dump_send_elapsed_ms", 30000L);
		this.batch_log_send_elapsed_ms = getLong("batch_log_send_elapsed_ms", 30000L);
		
		this.thread_check_interval_ms = getLong("thread_check_interval_ms", 1000L);
		
		this.plugin_dir = new File(getValue("plugin_dir", agent_dir_path + "/plugin"));
		
		this.net_udp_packet_max_bytes = getInt("net_udp_packet_max_bytes", 60000);
		this.net_collector_ip = getValue("net_collector_ip", "127.0.0.1");
		this.net_collector_udp_port = getInt("net_collector_udp_port", NetConstants.SERVER_UDP_PORT);
		this.net_collector_tcp_port = getInt("net_collector_tcp_port", NetConstants.SERVER_TCP_PORT);
		this.net_collector_tcp_session_count = getInt("net_collector_tcp_session_count", 1, 1);
		this.net_collector_tcp_connection_timeout_ms = getInt("net_collector_tcp_connection_timeout_ms", 3000);
		this.net_collector_tcp_so_timeout_ms = getInt("net_collector_tcp_so_timeout_ms", 60000);

		this.net_local_udp_port = getInt("net_local_udp_port", NetConstants.LOCAL_UDP_PORT);
		this.net_tcp_stack_session_count = getInt("net_tcp_stack_session_count", 1, 1);

		this.sql_enabled = getBoolean("sql_enabled", true);
		this.hook_jdbc_pstmt_classes = getValue("hook_jdbc_pstmt_classes", "");
		this.hook_jdbc_stmt_classes = getValue("hook_jdbc_stmt_classes", "");
		this.hook_jdbc_rs_classes = getValue("hook_jdbc_rs_classes", "");
		this.net_udp_collection_interval_ms = getInt("net_udp_collection_interval_ms", 100);

		this._log_asm_enabled = getBoolean("_log_asm_enabled", false);
		this.obj_type_inherit_to_child_enabled = getBoolean("obj_type_inherit_to_child_enabled", false);
		
		this.log_dir = getValue("log_dir", ".");
		this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
		this.log_keep_days = getInt("log_keep_days", 7, 1);
		this.sbr_log_make = getBoolean("sbr_log_make", false);
		
		this._trace = getBoolean("_trace", false);
        this._trace_use_logger = getBoolean("_trace_use_logger", false);
		this.log_ignore_set = getStringSet("mgr_log_ignore_ids", ",");

		resetObjInfo();
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
	
	public boolean isIgnoreLog(String id) {
		return log_ignore_set.hasKey(id);
	}
	
	
	public synchronized void resetObjInfo() {
		this.obj_type = getValue("obj_type", CounterConstants.BATCH);
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
		this.obj_host_type = getValue("obj_host_type", detected);
		this.obj_host_name = getValue("obj_host_name", SysJMX.getHostName());
		this.objHostName = "/" + this.obj_host_name;
		this.objHostHash = HashUtil.hash(this.objHostName);
		this.obj_name_auto_pid_enabled = getBoolean("obj_name_auto_pid_enabled", false);
		
		this.obj_name = getValue("obj_name", "batch");
		this.objName = this.objHostName + "/" + this.obj_name;
		// make hash value 
		try { this.objHash = HashUtil.hash( objName.getBytes("UTF-8")); }catch(Throwable ex){}
		
		System.setProperty("scouter.objname", this.objName);
		System.setProperty("scouter.objtype", this.obj_type);
		System.setProperty("scouter.dir", agent_dir_path);
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

	private static HashSet<String> ignoreSet = new HashSet<String>();
	static {
		ignoreSet.add("property");
		ignoreSet.add("__experimental");
	}
	
}
