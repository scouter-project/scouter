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

package scouter.server.test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scouter.util.FileUtil;

public class LoadTest {

	public static void main(String[] args) throws Throwable {

		ExecutorService ex = Executors.newCachedThreadPool();

		final String url = args.length == 0 ? "http://127.0.0.1:8080" : args[0];
		Runnable r = new Runnable() {
			public void run() {
				try {
					LoadTest.process(url);
				} catch (Exception x) {
				}
			}
		};
		while (true) {
			ex.execute(r);
			r.run();
		}
	}

	private static void process(String arg) throws Exception {
		long stime = System.currentTimeMillis();
		try {
			URL u = new URL(arg);
			URLConnection uc = u.openConnection();
			String uu = setUser(uc);
			uc.connect();
			InputStream o = uc.getInputStream();
			FileUtil.readAll(o);
			Map<String, List<String>> heads = uc.getHeaderFields();
			List<String> cookie = heads.get("Set-Cookie");
			keepCookie("" + cookie);
			o.close();
			long dur = System.currentTimeMillis() - stime;
			System.out.println(arg + " " + dur + " ms " + uu);
		} catch (Exception e) {
			long dur = System.currentTimeMillis() - stime;
			System.out.println(arg + " " + dur + " ms - ERROR");
		}
	}

	private static Random rand = new Random();
	private static List<String> cookies = new ArrayList();

	private static String setUser(URLConnection uc) {
		if (cookies.size() == 0)
			return "none";
		if (Math.abs(rand.nextInt()) % 2 == 0)
			return "none";
		int x = Math.abs(rand.nextInt()) % cookies.size();
		uc.addRequestProperty("Cookie", "SCOUTER=" + cookies.get(x));

		return "SCOUTER=" + cookies.get(x);
	}

	private static void keepCookie(String cookie) {
		if (cookie == null)
			return;

		int x1 = cookie.indexOf("SCOUTER");
		if (x1 >= 0) {
			String value = null;
			int x2 = cookie.indexOf(';', x1);
			if (x2 > 0) {
				value = cookie.substring(x1 + "SCOUTER".length() + 1, x2);
			} else {
				value = cookie.substring(x1 + "SCOUTER".length() + 1);
			}
			cookies.add(value);
			System.out.println("SCOUTER=" + value);
		}
		if (cookies.size() > 1000000)
			cookies.clear();
	}
}