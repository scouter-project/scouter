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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import scouter.util.logo.Logo;
import scouter.agent.batch.AgentTransformer;
import scouter.agent.batch.task.BatchMonitor;

public class JavaAgent {
	private static Instrumentation instrumentation;
	private static boolean java9plus;
	private static ClassLoader platformClassLoader;

	static {
		try
		{
			Method m = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader", new Class[0]);
			platformClassLoader = (ClassLoader) m.invoke(null, new Object[0]);
			java9plus = true;
		} catch (Exception ignored) {}
	}
	
	public static void premain(String options, Instrumentation instrum) {
		if (JavaAgent.instrumentation != null) {
			return;
		}
		intro();
		Configure config = Configure.getInstance();
		
		JavaAgent.instrumentation = instrum;
		if(config.scouter_enabled){
			JavaAgent.instrumentation.addTransformer(new AgentTransformer());			
			BatchMonitor.getInstance();
		}
	}

	private static void intro() {
		try {
			String confFile = System.getProperty("scouter.config"); 
			if( confFile == null){
				System.setProperty(Configure.VM_SCOUTER_ENABLED, "false");
			}
			
			Logo.print(false);
			String nativeName = JavaAgent.class.getName().replace('.', '/') + ".class";
			ClassLoader cl = JavaAgent.class.getClassLoader();
			if (cl == null) {
				Logger.println("loaded by system classloader ");
				Logger.println(cut("" + ClassLoader.getSystemClassLoader().getResource(nativeName)));
			} else {
				Logger.println("loaded by app classloader ");
				Logger.println(cut("" + cl.getResource(nativeName)));
			}
			Logger.println("scouter.config=" + confFile);
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
	
	public static boolean isJava9plus() {
		return java9plus;
	}	
	public static ClassLoader getPlatformClassLoader() {
		return platformClassLoader;
	}
}
