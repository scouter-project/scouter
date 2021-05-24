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
package scouter.agent;

import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.util.AsyncRunner;
import scouter.util.StringSet;
import scouter.util.logo.Logo;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import static scouter.agent.Logger.conf;

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
		Configure conf = Configure.getInstance();
		if (conf._hook_coroutine_debugger_hook_enabled && System.getProperty("kotlinx.coroutines.debug") == null) {
			System.setProperty("kotlinx.coroutines.debug", "");
		}
		preStart(options, instrum, new AgentTransformer());
	}

	public static void preStart(String options, Instrumentation instrum, ClassFileTransformer transformer) {
		if (JavaAgent.instrumentation != null) {
			return;
		}
		intro();

		Configure conf = Configure.getInstance();

		BackJobs.getInstance().put(Logger.class.getName(), 3000, Logger.initializer);
		JavaAgent.instrumentation = instrum;
		JavaAgent.instrumentation.addTransformer(transformer);

		addAsyncRedefineClasses();

		TcpRequestMgr.getInstance();
		AsyncRunner.getInstance().add(new AgentBoot());
	}

	private static void addAsyncRedefineClasses() {
		//preloaded map impl classes before arriving trnasform method.
		StringSet redefineClasses = new StringSet();
		if(conf._hook_map_impl_enabled) {
			redefineClasses.put("java.util.HashMap");
			redefineClasses.put("java.util.LinkedHashMap");
			redefineClasses.put("java.util.concurrent.ConcurrentHashMap");
			redefineClasses.put("java.util.HashTable");
		}

		redefineClasses.put("java.util.concurrent.AbstractExecutorService");
		redefineClasses.put("java.util.concurrent.ThreadPoolExecutor");
		redefineClasses.put("java.lang.Thread");

		//java.lang.invoke.LambdaMetafactory.*,java.lang.invoke.CallSite.*,
		//java.lang.invoke.ConstantCallSite.*,
		//java.lang.invoke.MutableCallSite.*,
		//java.lang.invoke.VolatileCallSite.*

//		redefineClasses.put("java.lang.invoke.CallSite");
//		redefineClasses.put("java.lang.invoke.ConstantCallSite");
//		redefineClasses.put("java.lang.invoke.MutableCallSite");
//		redefineClasses.put("java.lang.invoke.VolatileCallSite");
//		redefineClasses.put("java.lang.invoke.LambdaForm");
//		redefineClasses.put("java.lang.invoke.DirectMethodHandle");

		AsyncRunner.getInstance().add(redefineClasses);
	}

	private static void intro() {
		try {
			System.setProperty("scouter.enabled", "true");
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
