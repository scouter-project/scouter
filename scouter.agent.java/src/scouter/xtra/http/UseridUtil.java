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
package scouter.xtra.http;
import scouter.agent.Logger;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.KeyGen;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
public class UseridUtil {
	private static final String SCOUTE_R = "SCOUTER";
	public static long getUserid(HttpServletRequest req, HttpServletResponse res) {
		try {
			String cookie = req.getHeader("Cookie");
			if (cookie != null) {
				int x1 = cookie.indexOf(SCOUTE_R);
				if (x1 >= 0) {
					String value = null;
					int x2 = cookie.indexOf(';', x1);
					if (x2 > 0) {
						value = cookie.substring(x1 + SCOUTE_R.length() + 1, x2);
					} else {
						value = cookie.substring(x1 + SCOUTE_R.length() + 1);
					}
					try {
						return Hexa32.toLong32(value);
					} catch (Throwable th) {
					}
				}
			}
			Cookie c = new Cookie(SCOUTE_R, Hexa32.toString32(KeyGen.next()));
			c.setMaxAge(Integer.MAX_VALUE);
			res.addCookie(c);
		} catch (Throwable t) {
			Logger.println("A153", t.toString());
		}
		return 0;
	}
	public static long getUseridCustom(HttpServletRequest req, HttpServletResponse res, String key) {
		if (key == null || key.length() == 0)
			return 0;
		try {
			String cookie = req.getHeader("Cookie");
			if (cookie != null) {
				int x1 = cookie.indexOf(key);
				if (x1 >= 0) {
					String value = null;
					int x2 = cookie.indexOf(';', x1);
					if (x2 > 0) {
						value = cookie.substring(x1 + key.length() + 1, x2);
					} else {
						value = cookie.substring(x1 + key.length() + 1);
					}
					if (value != null) {
						return HashUtil.hash(value);
					}
				}
			}
		} catch (Throwable t) {
			Logger.println("A154", t.toString());
		}
		return 0;
	}

	public static long getUseridFromHeader(HttpServletRequest req, HttpServletResponse res, String key) {
		if (key == null || key.length() == 0)
			return 0;
		try {
			String headerValue = req.getHeader(key);
			if (headerValue != null) {
				return HashUtil.hash(headerValue);
			}
		} catch (Throwable t) {
			Logger.println("A155", t.toString());
		}
		return 0;
	}
}
