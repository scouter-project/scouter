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
 * 
 *  For this class
 *     many of method names
 *     basic ideas 
 *  are  from org.apache.commons.lang3.StringUtils.java
 *  
 */
package scouter.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

public class StringUtil {

	public static String firstWord(String target, String delim) {
		if (target == null || target.length() == 0) {
			return "";
		}
		StringTokenizer nizer = new StringTokenizer(target, delim);
		while (nizer.hasMoreTokens()) {
			return trimEmpty(nizer.nextToken());
		}
		return "";
	}

	public static String lastWord(String target, String delim) {
		if (target == null || target.length() == 0 || delim.length() == 0) {
			return "";
		}
		String value = "";
		StringTokenizer nizer = new StringTokenizer(target, delim);
		while (nizer.hasMoreTokens()) {
			value = trimEmpty(nizer.nextToken());
		}
		return value;
	}

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

	public static String trim(String s) {
		return s == null ? null : s.trim();
	}

	public static String trimEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	public static String trimToEmpty(String s) {
		return trimEmpty(s);
	}

	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
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

	public static ArrayList<String> splitAsList(String s, char c) {
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
		return arr;
	}

	public static String[] splitByWholeSeparatorPreserveAllTokens(String s, char c) {
		ArrayList<String> arr = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				if (sb.length() > 0) {
					arr.add(sb.toString());
					sb = new StringBuilder();
				} else {
					arr.add("");
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

	public static HashSet<String> splitAndTrimToSet(String s, char c, boolean toUpper) {
		HashSet<String> set = new HashSet<String>();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				if (sb.length() > 0) {
					set.add(toUpper ? sb.toString().toUpperCase().trim() : sb.toString().trim());
					sb = new StringBuilder();
				}
			} else {
				sb.append(s.charAt(i));
			}
		}
		if (sb.length() > 0) {
			set.add(toUpper ? sb.toString().toUpperCase().trim() : sb.toString().trim());
		}
		return set;
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

	public static String cutLastString(String className, char delim) {
		int x = className.lastIndexOf(delim);
		if (x > 0)
			return className.substring(x + 1);
		return className;
	}

	public static String removeLastString(String className, char delim) {
		int x = className.lastIndexOf(delim);
		if (x > 0)
			return className.substring(0, x);
		return className;
	}

	public static String[] divKeyValue(String line, String delim) {
		int x = line.indexOf(delim);
		if (x < 0)
			return new String[] { line };
		String s1 = line.substring(0, x );
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

	public static String rpad(String str, int len) {
		if (str == null) {
			return padding(len, ' ');
		}
		int slen = str.length();
		if (slen >= len)
			return str;
		return str + padding(len - slen, ' ');
	}

	public static String lpad(String str, int len) {
		if (str == null) {
			return padding(len, ' ');
		}
		int slen = str.length();
		if (slen >= len)
			return str;
		return padding(len - slen, ' ') + str;
	}

	public static String padding(int len, char ch) {
		StringBuffer sb = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			sb.append(ch);
		}
		return sb.toString();
	}

	public static int getWordCount(String text, String word) {
		int n = 0;
		int x = text.indexOf(word);
		while (x >= 0) {
			n++;
			x = text.indexOf(word, x + word.length());
		}
		return n;
	}
	
	public static String stripSideChar(String str, char ch) {
		if (isEmpty(str) || str.length() <= 1) {
			return str;
		}
		if (str.charAt(0) == ch && str.charAt(str.length() - 1) == ch) {
			return str.substring(1, str.length() -1);
		} else if (str.charAt(0) == ch) {
			return str.substring(1);
		} else if (str.charAt(str.length() - 1) == ch) {
			return str.substring(0, str.length() -1);
		}
		return str;
	}

	public static String emptyToDefault(String text, String defaultText) {
		if (StringUtil.isEmpty(StringUtil.trimToEmpty(text))) {
			return defaultText;
		} else {
			return text;
		}
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		} catch(NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}
}
