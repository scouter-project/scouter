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


import scouter.agent.util.JarUtil;
import scouter.lang.conf.ConfigValueUtil;
import scouter.net.NetConstants;
import scouter.util.FileUtil;
import scouter.util.StringSet;
import scouter.util.StringUtil;

public class Configure {
	public static final String CONFIG_SCOUTER_ENABLED = "scouter_enabled";
	public static final String VM_SCOUTER_ENABLED = "scouter.enabled";
	public static boolean JDBC_REDEFINED = false;
	private static Configure instance = null;
	
	public Properties property = new Properties();
    private File propertyFile;
    public static String agent_dir_path;
    static {
    	agent_dir_path = JarUtil.getThisJarFile().getParent();
    }

    public final static synchronized Configure getInstance() {
		if (instance == null) {
			instance = new Configure();
		}
		return instance;
	}
    
    // Scouter enable/disable
    public boolean scouter_enabled = true;
    public boolean scouter_stop = false;
    
    // Standalone
    public boolean scouter_standalone = false;
    
    // Batch basic configuration
    public String batch_id_type = ""; // Class, Args, Props 
    public String batch_id = "";
        
    // SQL
    public boolean sql_enabled = true;
    public int sql_max_count = 100;
    public String hook_jdbc_pstmt_classes = "";
	public String hook_jdbc_stmt_classes = "";
	public String hook_jdbc_rs_classes = "";    
    
    // SFA(Stack Frequency Analyzer) Thread Dump
	public boolean sfa_dump_enabled = true;
	public int sfa_dump_interval_ms = 10000;
	public String [] sfa_dump_filter = null;
	public File sfa_dump_dir = new File(agent_dir_path + "/dump");
	public boolean sfa_dump_header_exists = true;
	
	// dump send time
	public long dump_send_elapsed_ms = 0L;
	
	//Network
	public String net_collector_ip = "127.0.0.1";
	public int net_collector_udp_port = NetConstants.SERVER_UDP_PORT;
	public int net_collector_tcp_port = NetConstants.SERVER_TCP_PORT;
	public int net_collector_tcp_session_count = 1;
	public int net_collector_tcp_so_timeout_ms = 60000;
	public int net_collector_tcp_connection_timeout_ms = 3000;
	public int net_udp_packet_max_bytes = 60000;
	public long net_udp_collection_interval_ms = 100;

	//Object
	public String obj_type = "batch";
	public String obj_name = "";
	public String obj_host_type = "";
	public String obj_host_name = "";
	public boolean obj_name_auto_pid_enabled = false;
	public boolean obj_type_inherit_to_child_enabled = false;

	//Dir
	public File plugin_dir = new File(agent_dir_path + "/plugin");
	//public File mgr_agent_lib_dir = new File("./_scouter_");

	//Log
	public boolean _log_asm_enabled;
	
	public String log_dir ="";
	public boolean log_rotation_enabled =true;
	public int log_keep_days =7;
	public boolean _trace = false;
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
		this.obj_name = getValue("obj_name", "batch");
		// end for batch
		
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
		
		this.dump_send_elapsed_ms = getLong("dump_send_elapsed_ms", 0L);
		
		this.plugin_dir = new File(getValue("plugin_dir", agent_dir_path + "/plugin"));
		
		this.net_udp_packet_max_bytes = getInt("net_udp_packet_max_bytes", 60000);
		this.net_collector_ip = getValue("net_collector_ip", "127.0.0.1");
		this.net_collector_udp_port = getInt("net_collector_udp_port", NetConstants.SERVER_UDP_PORT);
		this.net_collector_tcp_port = getInt("net_collector_tcp_port", NetConstants.SERVER_TCP_PORT);
		this.net_collector_tcp_session_count = getInt("net_collector_tcp_session_count", 1, 1);
		this.net_collector_tcp_connection_timeout_ms = getInt("net_collector_tcp_connection_timeout_ms", 3000);
		this.net_collector_tcp_so_timeout_ms = getInt("net_collector_tcp_so_timeout_ms", 60000);

		
		this.sql_enabled = getBoolean("sql_enabled", true);
		this.hook_jdbc_pstmt_classes = getValue("hook_jdbc_pstmt_classes", "");
		this.hook_jdbc_stmt_classes = getValue("hook_jdbc_stmt_classes", "");
		this.hook_jdbc_rs_classes = getValue("hook_jdbc_rs_classes", "");
		this.net_udp_collection_interval_ms = getInt("net_udp_collection_interval_ms", 100);

		this._log_asm_enabled = getBoolean("_log_asm_enabled", false);
		this.obj_type_inherit_to_child_enabled = getBoolean("obj_type_inherit_to_child_enabled", false);
		
		this.log_dir = getValue("log_dir", "");
		this.log_rotation_enabled = getBoolean("log_rotation_enabled", true);
		this.log_keep_days = getInt("log_keep_days", 7);
        this._trace = getBoolean("_trace", false);
        this._trace_use_logger = getBoolean("_trace_use_logger", false);
		this.log_ignore_set = getStringSet("mgr_log_ignore_ids", ",");

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
	private static HashSet<String> ignoreSet = new HashSet<String>();
	static {
		ignoreSet.add("property");
		ignoreSet.add("__experimental");
	}
}
