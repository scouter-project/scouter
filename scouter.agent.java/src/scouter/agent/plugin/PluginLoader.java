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
 */
package scouter.agent.plugin;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.HookReturn;
import scouter.agent.trace.TraceSQL;
import scouter.javassist.*;
import scouter.util.FileUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
public class PluginLoader extends Thread {
	private static PluginLoader instance;
	public synchronized static PluginLoader getInstance() {
		if (instance == null) {
			instance = new PluginLoader();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(PluginLoader.class));
			instance.start();
		}
		return instance;
	}
	public void run() {
		while (true) {
			try {
				File root = Configure.getInstance().plugin_dir;
				reloadIfModified(root);
			} catch (Throwable t) {
				Logger.println("A160", t.toString());
			}
			ThreadUtil.sleep(5000);
		}
	}
	private void reloadIfModified(File root) {
		File script = new File(root, "service.plug");
		if (script.canRead() == false) {
			PluginAppServiceTrace.plugIn = null;
		} else {
			if (PluginAppServiceTrace.plugIn == null
					|| PluginAppServiceTrace.plugIn.lastModified != script.lastModified()) {
				PluginAppServiceTrace.plugIn = createAppService(script);
			}
		}
		script = new File(root, "httpservice.plug");
		if (script.canRead() == false) {
			PluginHttpServiceTrace.plugIn = null;
		} else {
			if (PluginHttpServiceTrace.plugIn == null
					|| PluginHttpServiceTrace.plugIn.lastModified != script.lastModified()) {
				PluginHttpServiceTrace.plugIn = createHttpService(script);
			}
		}
		script = new File(root, "capture.plug");
		if (script.canRead() == false) {
			PluginCaptureTrace.plugIn = null;
		} else {
			if (PluginCaptureTrace.plugIn == null || PluginCaptureTrace.plugIn.lastModified != script.lastModified()) {
				PluginCaptureTrace.plugIn = createICaptureTrace(script);
			}
		}
		script = new File(root, "springControllerCapture.plug");
		if (script.canRead() == false) {
			PluginSpringControllerCaptureTrace.plugIn = null;
		} else {
			if (PluginSpringControllerCaptureTrace.plugIn == null || PluginSpringControllerCaptureTrace.plugIn.lastModified != script.lastModified()) {
				PluginSpringControllerCaptureTrace.plugIn = createICaptureTrace(script);
			}
		}
		script = new File(root, "jdbcpool.plug");
		if (script.canRead() == false) {
			PluginJdbcPoolTrace.plugIn = null;
		} else {
			if (PluginJdbcPoolTrace.plugIn == null
					|| PluginJdbcPoolTrace.plugIn.lastModified != script.lastModified()) {
				PluginJdbcPoolTrace.plugIn = createIJdbcPool(script);
				if (PluginJdbcPoolTrace.plugIn != null) {
					TraceSQL.clearUrlMap();
				}
			}
		}
		script = new File(root, "httpcall.plug");
		if (script.canRead() == false) {
			PluginHttpCallTrace.plugIn = null;
		} else {
			if (PluginHttpCallTrace.plugIn == null
					|| PluginHttpCallTrace.plugIn.lastModified != script.lastModified()) {
				PluginHttpCallTrace.plugIn = createIHttpCall(script);
			}
		}
	}
	private long IHttpServiceCompile;
	private AbstractHttpService createHttpService(File script) {
		if (IHttpServiceCompile == script.lastModified())
			return null;
		IHttpServiceCompile = script.lastModified();
		try {
			HashMap<String, StringBuffer> bodyTable = loadFileText(script);
			String superName = AbstractHttpService.class.getName();
			String className = "scouter.agent.plugin.impl.HttpServiceImpl";
			String METHOD_START = "start";
			String METHOD_END = "end";
			String METHOD_REJECT = "reject";
			String SIGNATURE = nativeName(WrContext.class) + nativeName(WrRequest.class) + nativeName(WrResponse.class);
			String METHOD_P1 = WrContext.class.getName();
			String METHOD_P2 = WrRequest.class.getName();
			String METHOD_P3 = WrResponse.class.getName();
			if (bodyTable.containsKey(METHOD_START) == false)
				throw new CannotCompileException("no method body: " + METHOD_START);
			if (bodyTable.containsKey(METHOD_END) == false)
				throw new CannotCompileException("no method body: " + METHOD_END);
			if (bodyTable.containsKey(METHOD_REJECT) == false)
				throw new CannotCompileException("no method body: " + METHOD_REJECT);
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(PluginLoader.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			CtMethod method_start = null;
			CtMethod method_end = null;
			CtMethod method_reject = null;
			StringBuffer sb = null;
			try {
				impl = cp.get(className);
				impl.defrost();
				// START METHOD
				method_start = impl.getMethod(METHOD_START, "(" + SIGNATURE + ")V");
				// END METHOD
				method_end = impl.getMethod(METHOD_END, "(" + SIGNATURE + ")V");
				// REJECT METHOD
				method_reject = impl.getMethod(METHOD_REJECT, "(" + SIGNATURE + ")Z");
			} catch (scouter.javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				StringBuffer sb1 = new StringBuffer();
				sb1.append(METHOD_P1).append(" p1").append(",");
				sb1.append(METHOD_P2).append(" p2").append(",");
				sb1.append(METHOD_P3).append(" p3");
				// START METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(METHOD_START).append("(").append(sb1).append("){}");
				method_start = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_start);
				// END METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(METHOD_END).append("(").append(sb1).append("){}");
				method_end = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_end);
				// REJECT METHOD
				sb = new StringBuffer();
				sb.append("public boolean ").append(METHOD_REJECT).append("(").append(sb1).append("){return false;}");
				method_reject = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_reject);
			}
			StringBuffer bodyPrefix = new StringBuffer();
			bodyPrefix.append("{");
			bodyPrefix.append(METHOD_P1).append(" $ctx=$1;");
			bodyPrefix.append(METHOD_P2).append(" $req=$2;");
			bodyPrefix.append(METHOD_P3).append(" $res=$3;");
			method_start.setBody(
					new StringBuffer().append(bodyPrefix).append(bodyTable.get(METHOD_START)).append("\n}").toString());
			method_end.setBody(
					new StringBuffer().append(bodyPrefix).append(bodyTable.get(METHOD_END)).append("\n}").toString());
			method_reject.setBody(
					new StringBuffer().append(bodyPrefix).append(bodyTable.get(METHOD_REJECT)).append("\n}").toString());
			Class c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AbstractHttpService plugin = (AbstractHttpService) c.newInstance();
			plugin.lastModified = script.lastModified();
			Logger.println("PLUG-IN : " + AbstractHttpService.class.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (scouter.javassist.CannotCompileException ee) {
			Logger.println(ee.getMessage());
		} catch (Throwable e) {
			Logger.println("A161", e);
		}
		return null;
	}
	private HashMap<String, StringBuffer> loadFileText(File script) {
		StringBuffer sb = new StringBuffer();
		HashMap<String, StringBuffer> result = new HashMap<String, StringBuffer>();
		String txt = new String(FileUtil.readAll(script));
		try {
			BufferedReader r = new BufferedReader(new StringReader(txt));
			while (true) {
				String line = StringUtil.trim(r.readLine());
				if (line == null)
					break;
				if (line.startsWith("[") && line.endsWith("]")) {
					sb = new StringBuffer();
					result.put(line.substring(1, line.length() - 1), sb);
				} else {
					sb.append(line).append("\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	private long IServiceTraceCompile;
	private AbstractAppService createAppService(File script) {
		if (IServiceTraceCompile == script.lastModified())
			return null;
		IServiceTraceCompile = script.lastModified();
		try {
			HashMap<String, StringBuffer> bodyTable = loadFileText(script);
			String superName = AbstractAppService.class.getName();
			String className = "scouter.agent.plugin.impl.ServiceTraceImpl";
			String START = "start";
			String START_SIG = "(" + nativeName(WrContext.class) + nativeName(HookArgs.class) + ")V";
			String START_P1 = WrContext.class.getName();
			String START_P2 = HookArgs.class.getName();
			StringBuffer START_BODY = bodyTable.get(START);
			if (START_BODY == null)
				throw new CannotCompileException("no method body: " + START);
			String END = "end";
			String END_SIG = "(" + nativeName(WrContext.class) + ")V";
			String END_P1 = WrContext.class.getName();
			StringBuffer END_BODY = bodyTable.get(END);
			if (END_BODY == null)
				throw new CannotCompileException("no method body: " + END);
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(PluginLoader.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			Class c = null;
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			StringBuffer sb;
			CtMethod method_start = null;
			CtMethod method_end = null;
			try {
				impl = cp.get(className);
				impl.defrost();
				// START METHOD
				method_start = impl.getMethod(START, START_SIG);
				// END METHOD
				method_end = impl.getMethod(END, END_SIG);
			} catch (scouter.javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				// START METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(START).append("(");
				sb.append(START_P1).append(" p1 ").append(",");
				sb.append(START_P2).append(" p2");
				sb.append("){}");
				method_start = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_start);
				// END METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(END).append("(");
				sb.append(END_P1).append(" p1 ");
				sb.append("){}");
				method_end = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_end);
			}
			sb = new StringBuffer();
			sb.append("{");
			sb.append(START_P1).append(" $ctx=$1;");
			sb.append(START_P2).append(" $hook=$2;");
			sb.append(START_BODY);
			sb.append("\n}");
			method_start.setBody(sb.toString());
			sb = new StringBuffer();
			sb.append("{");
			sb.append(END_P1).append(" $ctx=$1;");
			sb.append(END_BODY);
			sb.append("\n}");
			method_end.setBody(sb.toString());
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AbstractAppService plugin = (AbstractAppService) c.newInstance();
			plugin.lastModified = script.lastModified();
			Logger.println("PLUG-IN : " + AbstractAppService.class.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (scouter.javassist.CannotCompileException ee) {
			Logger.println("PLUG-IN : " + ee.getMessage());
		} catch (Exception e) {
			Logger.println("A162", e);
		}
		return null;
	}
	private long ICaptureCompile;
	private AbstractCapture createICaptureTrace(File script) {
		if (ICaptureCompile == script.lastModified())
			return null;
		ICaptureCompile = script.lastModified();
		try {
			HashMap<String, StringBuffer> bodyTable = loadFileText(script);
			String superName = AbstractCapture.class.getName();
			String className = "scouter.agent.plugin.impl.CaptureImpl";
			String ARG = "capArgs";
			String ARG_SIG = "(" + nativeName(WrContext.class) + nativeName(HookArgs.class) + ")V";
			String ARG_P1 = WrContext.class.getName();
			String ARG_P2 = HookArgs.class.getName();
			StringBuffer ARG_BODY = bodyTable.get("args");
			String RTN = "capReturn";
			String RTN_SIG = "(" + nativeName(WrContext.class) + nativeName(HookReturn.class) + ")V";
			String RTN_P1 = WrContext.class.getName();
			String RTN_P2 = HookReturn.class.getName();
			StringBuffer RTN_BODY = bodyTable.get("return");
			String THIS = "capThis";
			String THIS_SIG = "(" + nativeName(WrContext.class) + nativeName(String.class) + nativeName(String.class)
					+ nativeName(Object.class) + ")V";
			String THIS_P1 = WrContext.class.getName();
			String THIS_P2 = String.class.getName();
			String THIS_P3 = String.class.getName();
			String THIS_P4 = "Object";
			StringBuffer THIS_BODY = bodyTable.get("this");
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(PluginLoader.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			Class c = null;
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			CtMethod method_args;
			CtMethod method_return;
			CtMethod method_this;
			StringBuffer sb;
			try {
				impl = cp.get(className);
				impl.defrost();
				// ARG METHOD
				method_args = impl.getMethod(ARG, ARG_SIG);
				// RETURN METHOD
				method_return = impl.getMethod(RTN, RTN_SIG);
				// THIS METHOD
				method_this = impl.getMethod(THIS, THIS_SIG);
			} catch (scouter.javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				// ARG METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(ARG).append("(");
				sb.append(ARG_P1).append(" p1 ").append(",");
				sb.append(ARG_P2).append(" p2 ");
				sb.append("){}");
				method_args = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_args);
				// RTN METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(RTN).append("(");
				sb.append(RTN_P1).append(" p1 ").append(",");
				sb.append(RTN_P2).append(" p2 ");
				sb.append("){}");
				method_return = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_return);
				// THIS METHOD
				sb = new StringBuffer();
				sb.append("public void ").append(THIS).append("(");
				sb.append(THIS_P1).append(" p1 ").append(",");
				sb.append(THIS_P2).append(" p2 ").append(",");
				sb.append(THIS_P3).append(" p3 ").append(",");
				sb.append(THIS_P4).append(" p4 ");
				sb.append("){}");
				method_this = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method_this);
			}
			sb = new StringBuffer();
			sb.append("{");
			sb.append(ARG_P1).append(" $ctx=$1;");
			sb.append(ARG_P2).append(" $hook=$2;");
			sb.append(ARG_BODY);
			sb.append("\n}");
			method_args.setBody(sb.toString());
			sb = new StringBuffer();
			sb.append("{");
			sb.append(RTN_P1).append(" $ctx=$1;");
			sb.append(RTN_P2).append(" $hook=$2;");
			sb.append(RTN_BODY);
			sb.append("\n}");
			method_return.setBody(sb.toString());
			sb = new StringBuffer();
			sb.append("{");
			sb.append(THIS_P1).append(" $ctx=$1;");
			sb.append(THIS_P2).append(" $class=$2;");
			sb.append(THIS_P3).append(" $desc=$3;");
			sb.append(THIS_P4).append(" $this=$4;");
			sb.append(THIS_BODY);
			sb.append("\n}");
			method_this.setBody(sb.toString());
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AbstractCapture plugin = (AbstractCapture) c.newInstance();
			plugin.lastModified = script.lastModified();
			Logger.println("PLUG-IN : " + AbstractCapture.class.getName() + " " + script.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (scouter.javassist.CannotCompileException ee) {
			Logger.println("PLUG-IN : " + ee.getMessage());
		} catch (Exception e) {
			Logger.println("A905", e);
		}
		return null;
	}
	private long IJdbcPoolCompile;
	private AbstractJdbcPool createIJdbcPool(File script) {
		if (IJdbcPoolCompile == script.lastModified())
			return null;
		IJdbcPoolCompile = script.lastModified();
		try {
			HashMap<String, StringBuffer> bodyTable = loadFileText(script);
			String superName = AbstractJdbcPool.class.getName();
			String className = "scouter.agent.plugin.impl.JdbcPoolImpl";
			String URL = "url";
			String URL_SIG = "(" + nativeName(WrContext.class) + nativeName(String.class) + nativeName(Object.class)
					+ ")" + nativeName(String.class);
			String URL_P1 = WrContext.class.getName();
			String URL_P2 = String.class.getName();
			String URL_P3 = "Object";
			StringBuffer URL_BODY = bodyTable.get("url");
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(PluginLoader.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			Class c = null;
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			CtMethod method = null;
			try {
				impl = cp.get(className);
				impl.defrost();
				method = impl.getMethod(URL, URL_SIG);
			} catch (scouter.javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				StringBuffer sb = new StringBuffer();
				sb.append("public String ").append(URL).append("(");
				sb.append(URL_P1).append(" p1 ").append(",");
				sb.append(URL_P2).append(" p2 ").append(",");
				sb.append(URL_P3).append(" p3 ");
				sb.append("){return null;}");
				method = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method);
			}
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			sb.append(URL_P1).append(" $ctx=$1;");
			sb.append(URL_P2).append(" $msg=$2;");
			sb.append(URL_P3).append(" $pool=$3;");
			sb.append(URL_BODY);
			sb.append("\n}");
			method.setBody(sb.toString());
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AbstractJdbcPool plugin = (AbstractJdbcPool) c.newInstance();
			plugin.lastModified = script.lastModified();
			Logger.println("PLUG-IN : " + AbstractJdbcPool.class.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (scouter.javassist.CannotCompileException ee) {
			Logger.println("PLUG-IN : " + ee.getMessage());
		} catch (Exception e) {
			Logger.println("A906", e);
		}
		return null;
	}
	private long IHttpCallCompile;
	private AbstractHttpCall createIHttpCall(File script) {
		if (IHttpCallCompile == script.lastModified())
			return null;
		IHttpCallCompile = script.lastModified();
		try {
			HashMap<String, StringBuffer> bodyTable = loadFileText(script);
			String superName = AbstractHttpCall.class.getName();
			String className = "scouter.agent.plugin.impl.IHttCallTraceImpl";
			String CALL = "call";
			String CALL_SIG = "(" + nativeName(WrContext.class) + nativeName(WrHttpCallRequest.class) + ")V";
			String CALL_P1 = WrContext.class.getName();
			String CALL_P2 = WrHttpCallRequest.class.getName();
			StringBuffer CALL_BODY = bodyTable.get(CALL);
			if (CALL_BODY == null)
				throw new CannotCompileException("no method body: " + CALL);
			ClassPool cp = ClassPool.getDefault();
			String jar = FileUtil.getJarFileName(PluginLoader.class);
			if (jar != null) {
				cp.appendClassPath(jar);
			}
			Class c = null;
			CtClass cc = cp.get(superName);
			CtClass impl = null;
			StringBuffer sb;
			CtMethod method = null;
			try {
				impl = cp.get(className);
				impl.defrost();
				method = impl.getMethod(CALL, CALL_SIG);
			} catch (scouter.javassist.NotFoundException e) {
				impl = cp.makeClass(className, cc);
				sb = new StringBuffer();
				sb.append("public void ").append(CALL).append("(");
				sb.append(CALL_P1).append(" p1 ").append(",");
				sb.append(CALL_P2).append(" p2");
				sb.append("){}");
				method = CtNewMethod.make(sb.toString(), impl);
				impl.addMethod(method);
			}
			sb = new StringBuffer();
			sb.append("{");
			sb.append(CALL_P1).append(" $ctx=$1;");
			sb.append(CALL_P2).append(" $req=$2;");
			sb.append(CALL_BODY);
			sb.append("\n}");
			method.setBody(sb.toString());
			c = impl.toClass(new URLClassLoader(new URL[0], this.getClass().getClassLoader()), null);
			AbstractHttpCall plugin = (AbstractHttpCall) c.newInstance();
			plugin.lastModified = script.lastModified();
			Logger.println("PLUG-IN : " + AbstractHttpCall.class.getName() + " loaded #"
					+ Hexa32.toString32(plugin.hashCode()));
			return plugin;
		} catch (scouter.javassist.CannotCompileException ee) {
			Logger.println("PLUG-IN : " + ee.getMessage());
		} catch (Exception e) {
			Logger.println("A907", e);
		}
		return null;
	}
	private String nativeName(Class class1) {
		return "L" + class1.getName().replace('.', '/') + ";";
	}
}
