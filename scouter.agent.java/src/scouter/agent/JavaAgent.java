/*
 *  Copyright 2015 the original author or authors.
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
package scouter.agent;

import java.lang.instrument.Instrumentation;

import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.util.AsyncRunner;
import scouter.util.StringUtil;
import scouter.util.logo.Logo;

public class JavaAgent {

	private static Instrumentation instrumentation;

	public static void premain(String options, Instrumentation i) {

		if (JavaAgent.instrumentation != null) {
			return;
		}
		intro();
		Configure.getInstance();

		JavaAgent.instrumentation = i;
		JavaAgent.instrumentation.addTransformer(new AgentTransformer());

		//RequestAgent.getInstance();
		TcpRequestMgr.getInstance();
		AsyncRunner.getInstance().add(new AgentBoot());
	}

	public static void agentmain(String options, Instrumentation i) throws Exception {

		if (JavaAgent.instrumentation != null) {
			return;
		}

		setOpt(options);
		intro();
		Configure.getInstance();

		JavaAgent.instrumentation = i;
		JavaAgent.instrumentation.addTransformer(new AgentTransformer());

		//RequestAgent.getInstance();
		TcpRequestMgr.getInstance();
		AsyncRunner.getInstance().add(new LazyAgentBoot());
	}

	private static void setOpt(String opts) {

		try {
			opts = StringUtil.trim(opts);
			if (StringUtil.isEmpty(opts))
				return;

			String[] options = StringUtil.split(opts, ',');
			for (int i = 0; i < options.length; i++) {
				String[] op = StringUtil.split(options[i], '=');
				if (op.length != 2)
					continue;
				String key = StringUtil.trimToEmpty(op[0]);
				String value = StringUtil.trimToEmpty(op[1]);
				if (key.length() > 0) {
					System.setProperty(key, value);
					Logger.println("A117","add property : " + key + "=" + value);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	

	private static void intro() {
		try {
			System.setProperty("scouter.enabled", "true");
			Logo.print(false);
			ClassLoader cl = JavaAgent.class.getClassLoader();
			if (cl == null) {
				Logger.info("loaded by system classloader ");
				Logger.info(cut(""
						+ ClassLoader.getSystemClassLoader().getResource(
								JavaAgent.class.getName().replace('.', '/') + ".class")));
			} else {
				Logger.info("loaded by app classloader ");
				Logger.info(cut("" + cl.getResource(JavaAgent.class.getName().replace('.', '/') + ".class")));
			}
		} catch (Throwable t) {
		}
	}

	private static String cut(String s) {
		int x = s.indexOf('!');
		return x > 0 ? s.substring(0, x) : s;
	}

	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}
}
