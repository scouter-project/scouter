/*
 *  Copyright 2015 LG CNS.
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
 *
 */
package scouter.server.term;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Util {

	public static int getProcessPID() {
		RuntimeMXBean o = ManagementFactory.getRuntimeMXBean();
		String nm = o.getName();
		int x = nm.indexOf("@");
		try {
			if (x > 0)
				return Integer.parseInt(nm.substring(0, x));
		} catch (Exception e) {
		}
		return -1;
	}

	public static String[] getCmdSplit(String s) {
		ArrayList<String> str = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		boolean quatation = false;
		boolean backslash = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (quatation) {
				if (c == '"') {
					quatation = false;
				} else {
					sb.append(c);
				}
				continue;
			}
			if (backslash) {
				backslash = false;
				sb.append(c);
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (sb.length() > 0) {
					str.add(sb.toString());
					sb = new StringBuffer();
				}
				continue;
			}
			switch (c) {
			case '\\':
				backslash = true;
				break;
			case '"':
				if (sb.length() == 0) {
					quatation = true;
				} else {
					sb.append(c);
				}
				break;
			default:
				sb.append(c);
				break;
			}
		}
		if (sb.length() > 0) {
			str.add(sb.toString());
		}
		return str.toArray(new String[str.size()]);
	}

	static DecimalFormat format = new DecimalFormat("#,##0");

	public static String format(long l) {
		return format.format(l);
	}

	public static String format(Object a) {
		if (a == null)
			return "";
		if (a instanceof Number)
			return format(((Number) a).longValue());
		else
			return a.toString();
	}

	public static String limit(String s, int len) {
		if(s==null || s.length() <= len)
			return s;
		
		return s.substring(0, len)+"...";
	}
}
