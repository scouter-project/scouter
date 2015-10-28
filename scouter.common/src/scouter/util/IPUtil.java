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

package scouter.util;

import scouter.io.DataOutputX;

public class IPUtil {
	public static String toString(int ip) {
		return toString(DataOutputX.toBytes(ip));
	}
	public static String toString(byte[] ip) {
		if (ip == null)
			return "0.0.0.0";
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(ip[0] & 0xff);
			sb.append(".");
			sb.append(ip[1] & 0xff);
			sb.append(".");
			sb.append(ip[2] & 0xff);
			sb.append(".");
			sb.append(ip[3] & 0xff);
			return sb.toString();
		} catch (Throwable e) {
			return "0.0.0.0";
		}
	}

	public static byte[] toBytes(String ip) {
		if (ip == null) {
			return empty;
		}
		byte[] result = new byte[4];
		String[] s = StringUtil.split(ip, '.');
		long val;
		try {
			if (s.length != 4)
				return empty;

			for (int i = 0; i < 4; i++) {
				val = Integer.parseInt(s[i]);
				if (val < 0 || val > 0xff)
					return null;
				result[i] = (byte) (val & 0xff);
			}
		} catch (Throwable e) {
			return empty;
		}
		return result;
	}

	public static boolean isOK(byte[] ip){
		return  ip != null && ip.length==4;
	}
	public static boolean isNotLocal(byte[] ip) {
		return isOK(ip) && (ip[0] & 0xff) != 127;
	}

	private static byte[] empty = new byte[] { 0, 0, 0, 0 };


	public static void main(String[] args) {
		String[] s = StringUtil.split("127.0.0.1", '.');
		System.out.println(s[0]);
		System.out.println(s[1]);
		System.out.println(s[2]);
		System.out.println(s[3]);
	}
}