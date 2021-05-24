/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.framework.configure;

import scouter.lang.conf.ConfObserver;
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.ConfigValueType;
import scouter.lang.conf.ConfigValueUtil;
import scouter.lang.conf.ValueType;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.NetConstants;
import scouter.util.FileUtil;
import scouter.util.StrMatch;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StandAloneConfigure extends Thread {
	private static StandAloneConfigure instance = null;
	public final static String CONF_DIR = "./conf/";

	protected final static synchronized StandAloneConfigure getInstance() {
		if (instance == null) {
			instance = new StandAloneConfigure();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	public boolean _trace = false;

	//Network
	@ConfigDesc("Collector connection infos - eg) host:6100:id:pw,host2:6100:id2:pw2")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String net_collector_ip_port_id_pws = "127.0.0.1:6100:admin:admin";

	@ConfigDesc("size of webapp connection pool to collector")
	public int net_webapp_tcp_client_pool_size = 100;
	@ConfigDesc("timeout of web app connection pool to collector")
	public int net_webapp_tcp_client_pool_timeout = 60000;
	@ConfigDesc("So timeout of web app to collector")
	public int net_webapp_tcp_client_so_timeout = 30000;

	@ConfigDesc("Enable api access control by client ip")
	public boolean net_http_api_auth_ip_enabled = false;
	@ConfigDesc("If get api caller's ip from http header.")
	public String net_http_api_auth_ip_header_key;

	@ConfigDesc("Enable api access control by JSESSIONID of Cookie - get session from /user/login.")
	public boolean net_http_api_auth_session_enabled = false;
	@ConfigDesc("api http session timeout(sec)")
	public int net_http_api_session_timeout = 1*3600*24;
	@ConfigDesc("Enable api access control by Bearer token(of Authorization http header) - get access token from /user/loginGetToken.")
	public boolean net_http_api_auth_bearer_token_enabled = false;
	@ConfigDesc("Enable gzip response on api call")
	public boolean net_http_api_gzip_enabled = true;

	@ConfigDesc("api access allow ip addresses")
	@ConfigValueType(ValueType.COMMA_SEPARATED_VALUE)
	public String net_http_api_allow_ips = "localhost,127.0.0.1,0:0:0:0:0:0:0:1,::1";
	public Set<String> allowIpExact;
	public List<StrMatch> allowIpMatch;

	@ConfigDesc("HTTP service port")
	public int net_http_port = NetConstants.WEBAPP_HTTP_PORT;

	@ConfigDesc("user extension web root")
	public String net_http_extweb_dir = "./extweb";

	@ConfigDesc("HTTP API swagger enable option")
	public boolean net_http_api_swagger_enabled = false;

	@ConfigDesc("Swagger option of host's ip or domain to call APIs.")
	public String net_http_api_swagger_host_ip = "";
	@ConfigDesc("API CORS support for Access-Control-Allow-Origin")
	public String net_http_api_cors_allow_origin = "*";
	@ConfigDesc("Access-Control-Allow-Credentials")
	public String net_http_api_cors_allow_credentials = "true";

	@ConfigDesc("Log directory")
	public String log_dir = "./logs";
	@ConfigDesc("Keeping period of log")
	public int log_keep_days = 30;

	@ConfigDesc("temp dir")
	public String temp_dir = "./tempdata";

	private StandAloneConfigure() {
		Properties p = new Properties();
		Map args = new HashMap();
		args.putAll(System.getenv());
		args.putAll(System.getProperties());
		p.putAll(args);
		this.property = p;
		reload(false);
	}

	private StandAloneConfigure(boolean b) {
	}

	private long last_load_time = -1;
	public Properties property = new Properties();

	private boolean running = true;

	@Override
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
		ConfObserver.run();
		return true;
	}

	private void apply() {
		this._trace = getBoolean("_trace", false);

		this.net_collector_ip_port_id_pws = getValue("net_collector_ip_port_id_pws", "127.0.0.1:6100:admin:admin");

		this.net_webapp_tcp_client_pool_size = getInt("net_webapp_tcp_client_pool_size", 100);
		this.net_webapp_tcp_client_pool_timeout = getInt("net_webapp_tcp_client_pool_timeout", 15000);

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

		this.net_http_port = getInt("net_http_port", NetConstants.WEBAPP_HTTP_PORT);
		this.net_http_extweb_dir = getValue("net_http_extweb_dir", "./extweb");

		this.net_http_api_swagger_enabled = getBoolean("net_http_api_swagger_enabled", false);
		this.net_http_api_swagger_host_ip = getValue("net_http_api_swagger_host_ip", "");
		this.net_http_api_cors_allow_origin = getValue("net_http_api_cors_allow_origin", "*");
		this.net_http_api_cors_allow_credentials = getValue("net_http_api_cors_allow_credentials", "true");


		this.log_dir = getValue("log_dir", "./logs");
		this.log_keep_days = getInt("log_keep_days", 30);

		this.temp_dir = getValue("temp_dir", "./tempdata");
	}

	public List<ServerConfig> getServerConfigs() {
		List<ServerConfig> list = Stream.of(this.net_collector_ip_port_id_pws.split(","))
				.map(s -> {
					String val[] = s.split(":");
					return new ServerConfig(val[0], val[1], val[2], val[3]);
				}).collect(Collectors.toList());

		return list;
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
		System.out.println("Configure -Dscouter.config=" + propertyFile);
	}

	public MapValue getKeyValueInfo() {
		StringKeyLinkedMap<Object> defMap = ConfigValueUtil.getConfigDefault(new StandAloneConfigure(true));
		StringKeyLinkedMap<Object> curMap = ConfigValueUtil.getConfigDefault(this);
		MapValue m = new MapValue();
		ListValue nameList = m.newList("key");
		ListValue valueList = m.newList("value");
		ListValue defList = m.newList("default");

		StringEnumer enu = defMap.keys();
		while (enu.hasMoreElements()) {
			String key = enu.nextString();
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

}
