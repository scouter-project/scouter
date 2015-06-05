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
 *  For this class
 *     many of method names
 *     basic ideas 
 *  are  from org.apache.commons.lang3.StringUtils.java
 *  
 */
package scouter.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class StringUtil {

	public static String[] tokenizer(String target, String delim) {
		if (target == null || target.length() == 0) {
			return null;
		}
		StringTokenizer nizer = new StringTokenizer(target, delim);
		ArrayList<String> arr = new ArrayList<String>();
		while (nizer.hasMoreTokens()) {
			String s = trimEmpty(nizer.nextToken());
			if (s.length() > 0) {
				arr.add(s);
			}
		}
		return arr.toArray(new String[arr.size()]);
	}

	public static String lowerFirst(String str) {
		if (isEmpty(str)) {
			return str;
		}
		char[] buffer = str.toCharArray();
		buffer[0] = Character.toLowerCase(buffer[0]);
		return new String(buffer);
	}

	public static String upperFirst(String str) {
		if (isEmpty(str)) {
			return str;
		}
		char[] buffer = str.toCharArray();
		buffer[0] = Character.toUpperCase(buffer[0]);
		return new String(buffer);
	}

	public static String erase(String str, String delim) {
		if (str == null || delim == null)
			return str;
		StringTokenizer tor = new StringTokenizer(str, delim);
		StringBuffer sb = new StringBuffer(str.length());
		while (tor.hasMoreTokens()) {
			sb.append(tor.nextToken());
		}
		return sb.toString();
	}

	public static String limiting(String s, int max) {
		if (s == null)
			return null;
		if (s.length() > max)
			return s.substring(0, max);
		return s;
	}

	public static String trimEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	public static String trim(String s) {
		return s == null ? null : s.trim();
	}

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}

	public static boolean isNotEmpty(String s) {
		return s != null && s.length() > 0;
	}

	public static String[] split(String s, char c) {
		ArrayList<String> arr = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				if (sb.length() > 0) {
					arr.add(sb.toString());
					sb = new StringBuilder();
				}
			} else {
				sb.append(s.charAt(i));
			}
		}
		if (sb.length() > 0) {
			arr.add(sb.toString());
			sb = new StringBuilder();
		}
		return arr.toArray(new String[arr.size()]);
	}

	public static String[] split(String s, String delim) {
		ArrayList<String> arr = new ArrayList<String>();
		while (s.length() > 0) {
			int pos = s.indexOf(delim);
			if (pos < 0) {
				arr.add(s);
				s = "";
			} else if (pos > 0) {
				arr.add(s.substring(0, pos));
				s = s.substring(pos + delim.length());
			} else {
				s = s.substring(delim.length());
			}
		}
		return arr.toArray(new String[arr.size()]);
	}

	public static String trimToEmpty(String x) {
		return x == null ? "" : x.trim();
	}

	public static String removeWhitespace(String str) {
		if (isEmpty(str)) {
			return str;
		}
		int strLength = str.length();
		StringBuffer sb = new StringBuffer(strLength);
		for (int i = 0; i < strLength; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				sb.append(str.charAt(i));
			}
		}
		if (sb.length() == strLength)
			return str;
		return sb.toString();
	}

	public static String remove(String str, char ch) {
		if (isEmpty(str)) {
			return str;
		}
		int strLength = str.length();
		StringBuffer sb = new StringBuffer(strLength);
		for (int i = 0; i < strLength; i++) {
			if (str.charAt(i) != ch) {
				sb.append(str.charAt(i));
			}
		}
		if (sb.length() == strLength)
			return str;
		return sb.toString();
	}

	public static String strip(String str, String striped) {
		if (isEmpty(str)) {
			return str;
		}
		StringBuffer sb = new StringBuffer(str.length());
		StringTokenizer niz = new StringTokenizer(str, striped);
		while (niz.hasMoreTokens()) {
			sb.append(niz.nextToken());
		}
		return sb.toString();
	}

	public static String leftPad(String str, int size) {
		if (str == null || str.length() >= size) {
			return str;
		}
		StringBuffer sb = new StringBuffer(size);
		int len = size - str.length();
		for (int i = 0; i < len; i++) {
			sb.append(' ');
		}
		sb.append(str);
		return sb.toString();
	}

	public static void main(String[] args) {
		String s = "aaa = bbb ccc";
		String[] ss = divKeyValue(s, "=");
		System.out.println(ArrayUtil.toString(ss));
	}

	public static String cutLastString(String className, char delim) {
		int x = className.lastIndexOf(delim);
		if (x > 0)
			return className.substring(x + 1);
		return className;
	}

	public static String[] divKeyValue(String line, String delim) {
		int x = line.indexOf(delim);
		if (x < 0)
			return new String[] { line };
		String s1 = line.substring(0, x - 1);
		String s2 = line.substring(x + 1);
		return new String[] { s1, s2 };
	}

	public static String toString(StringEnumer enu) {
		if (enu.hasMoreElements()) {
			StringBuffer sb = new StringBuffer();
			sb.append("[").append(enu.nextString());
			while (enu.hasMoreElements()) {
				sb.append(", ").append(enu.nextString());
			}
			sb.append("]");
			return sb.toString();
		} else {
			return "[]";
		}
	}

	public static String toString(IntEnumer enu) {
		if (enu.hasMoreElements()) {
			StringBuffer sb = new StringBuffer();
			sb.append("[").append(enu.nextInt());
			while (enu.hasMoreElements()) {
				sb.append(", ").append(enu.nextInt());
			}
			sb.append("]");
			return sb.toString();
		} else {
			return "[]";
		}
	}

	public static String toString(LongEnumer enu) {
		if (enu.hasMoreElements()) {
			StringBuffer sb = new StringBuffer();
			sb.append("[").append(enu.nextLong());
			while (enu.hasMoreElements()) {
				sb.append(", ").append(enu.nextLong());
			}
			sb.append("]");
			return sb.toString();
		} else {
			return "[]";
		}
	}

	public static String truncate(String str, int len) {
		return str == null || str.length() <= len ? str : str.substring(0, len);
	}
}
