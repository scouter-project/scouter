/**
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

package scouter;

import scouter.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Scouter version class
 */
public class Version {

	/*
	 * AGENT_VERSION = 20140523 AGENT_BUILD = 1.0.0 SERVER_VERSION = 20140523
	 * SERVER_BUILD = 1.0.0 CLIENT_VERSION = 20140523 CLIENT_BUILD = 1.0.0
	 */

	/**
	 * AGENT
	 * @return
	 */
	public static String getVersion() {
		Properties properties = getProperties();
		if (properties != null) {
			return properties.get("VERSION").toString();
		} else {
			return "";
		}
	}

	public static String getBuildVer() {
		Properties properties = getProperties();
		if (properties != null) {
			return properties.get("BUILD").toString();
		} else {
			return "";
		}
	}

	public static String getAgentFullVersion() {
		return getVersion()+" " +getBuildVer() ;
	}

	// -- SERVER --
	public static String getServerFullVersion() {
		return getVersion()+" " +getBuildVer() ;
	}

	// -- SERVER --
	public static String getServerRecommendedClientVersion() {
		Properties properties = getProperties();
		if (properties != null) {
			return properties.getProperty("CLIENT_VERSION", "");
		} else {
			return "";
		}
	}

	// -- CLIENT --
	public static String getClientFullVersion() {
		return  getVersion()+" " +getBuildVer() ;
	}

	public static void main(String[] args) {
		System.out.println("Agent Ver. = " + getAgentFullVersion());
		System.out.println("Server Ver. = " + getServerFullVersion());
		System.out.println("Client Ver. = " + getClientFullVersion());
	}

	private static Properties getProperties() {
		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = Version.class
					.getResourceAsStream("/scouter/v.properties");
			properties.load(is);
		} catch (IOException e) {
			return null;
		} finally {
			FileUtil.close(is);
		}
		return properties;
	}

	public static int versionCompare(String str1, String str2) {
		if(str1 == null && str2 == null)
			return 0;
		if(str1 == null)
            return -1;
		if(str2 == null)
            return 1;
		str1 = numonly(str1);
		str2 = numonly(str2);
        String[] thisParts = str1.split("\\.");
        String[] thatParts = str2.split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            long thisPart = i < thisParts.length ?
                Long.parseLong(thisParts[i]) : 0;
            long thatPart = i < thatParts.length ?
            	Long.parseLong(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
	}
	
	private static String numonly(String t) {
		char[] c = t.toCharArray();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < c.length; i++) {
			switch (c[i]) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
				sb.append(c[i]);
			}
		}
		return sb.toString();
	}
}
