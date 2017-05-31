package scouter.agent.plugin;

import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.AlertProxy;
import scouter.agent.trace.HookArgs;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TransferMap;
import scouter.lang.AlertLevel;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ThreadSubmitStep;
import scouter.util.KeyGen;
import scouter.util.SysJMX;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AbstractPlugin {
	private static Map<String, AccessibleObject> reflCache = Collections.synchronizedMap(new LinkedHashMap<String, AccessibleObject>(100));

	long lastModified;

	public void log(Object c) {
		Logger.println("A158", c.toString());
	}
	public void println(Object c) {
		System.out.println(c);
	}

	public static Object invokeMethod(Object o, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object[] objs = {};
		return invokeMethod(o, methodName, objs);
	}

	public static Object invokeMethod(Object o, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		int argsSize = args.length;
		StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(methodName).append("():");

		Class[] argClazzes = new Class[argsSize];

		for(int i=0; i<argsSize; i++) {
			argClazzes[i] = args[i].getClass();
			signature.append(argClazzes[i].getName()).append("+");
		}
		Method m = (Method) reflCache.get(signature.toString());
		if(m == null) {
			m = o.getClass().getMethod(methodName, argClazzes);
			reflCache.put(signature.toString(), m);
		}
		return m.invoke(o, args);
	}

	public static Object newInstance(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		return newInstance(className, Thread.currentThread().getContextClassLoader());
	}

	public static Object newInstance(String className, ClassLoader loader) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Object[] objs = {};
		return newInstance(className, loader, objs);
	}

	public static Object newInstance(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		return newInstance(className, Thread.currentThread().getContextClassLoader(), args);
	}

	public static Object newInstance(String className, ClassLoader loader, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		int argsSize = args.length;
		Class[] argClazzes = new Class[argsSize];
		StringBuilder signature = new StringBuilder(className).append(":<init>:");

		for(int i=0; i<argsSize; i++) {
			argClazzes[i] = args[i].getClass();
			signature.append(argClazzes[i].getName()).append("+");
		}

		Class clazz = Class.forName(className, true, loader);
		Constructor constructor = (Constructor)reflCache.get(signature.toString());

		if(constructor == null) {
			constructor = clazz.getConstructor(argClazzes);
			reflCache.put(signature.toString(), constructor);
		}

		return constructor.newInstance(args);
	}

	public static Object getFieldValue(Object o, String fieldName) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
		StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(fieldName).append(":");
		Field f = (Field) reflCache.get(signature.toString());
		if(f == null) {
			f = o.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			reflCache.put(signature.toString(), f);
		}
		return f.get(o);
	}

	@Deprecated
	public Object field(Object o, String field) {
		if (o == null)
			return null;
		try {
			Field f = o.getClass().getDeclaredField(field);
			f.setAccessible(true);
			return f.get(o);
		} catch (Throwable e) {
		}
		return null;
	}
	@Deprecated
	public Object method(Object o, String method) {
		if (o == null)
			return null;
		try {
			Method m = o.getClass().getMethod(method, Wrapper.arg_c);
			m.setAccessible(true);
			return m.invoke(o, Wrapper.arg_o);
		} catch (Throwable e) {
		}
		return null;
	}
	@Deprecated
	public Object method1(Object o, String method) {
		if (o == null)
			return null;
		try {
			Method m = o.getClass().getMethod(method, Wrapper.arg_c);
			return m.invoke(o, Wrapper.arg_o);
		} catch (Throwable e) {
			return e.toString();
		}
	}
	@Deprecated
	public Object method(Object o, String method, String param) {
		if (o == null)
			return null;
		try {
			Method m = o.getClass().getMethod(method, Wrapper.arg_c_s);
			return m.invoke(o, new Object[] { param });
		} catch (Throwable e) {
		}
		return null;
	}
	public String toString(Object o) {
		return o == null ? null : o.toString();
	}
	public String toString(Object o, String def) {
		return o == null ? def : o.toString();
	}
	public void alert(char level, String title, String message) {
		switch (level) {
		case 'i':
		case 'I':
			AlertProxy.sendAlert(AlertLevel.INFO, title, message);
		case 'w':
		case 'W':
			AlertProxy.sendAlert(AlertLevel.WARN, title, message);
			break;
		case 'e':
		case 'E':
			AlertProxy.sendAlert(AlertLevel.ERROR, title, message);
			break;
		case 'f':
		case 'F':
			AlertProxy.sendAlert(AlertLevel.FATAL, title, message);
			break;
		}
	}
	///
	public int syshash(Object o) {
		if (o == null)
			return 0;
		return System.identityHashCode(o);
	}
	public int syshash(HookArgs hook, int x) {
		if (x >= hook.args.length)
			return 0;
		return syshash(hook.args[x]);
	}
	public int syshash(HookArgs hook) {
		if (hook == null || hook.this1 == null)
			return 0;
		return syshash(hook.this1);
	}
	public void forward(WrContext wctx, int uuid) {
		TraceContext ctx = wctx.inner();
		if (ctx.gxid == 0) {
			ctx.gxid = ctx.txid;
		}
		long callee = KeyGen.next();
		TransferMap.put(uuid, ctx.gxid, ctx.txid, callee, XLogTypes.APP_SERVICE);
		ApiCallStep step = new ApiCallStep();
		step.txid = callee;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.hash = DataProxy.sendApicall("local-forward");
		ctx.profile.add(step);
	}
	public void forwardThread(WrContext wctx, int uuid) {
		if (wctx == null)
			return;
		TraceContext ctx = wctx.inner();
		if (ctx.gxid == 0) {
			ctx.gxid = ctx.txid;
		}
		long callee = KeyGen.next();
		TransferMap.put(uuid, ctx.gxid, ctx.txid, callee, XLogTypes.BACK_THREAD);
		ThreadSubmitStep step = new ThreadSubmitStep();
		step.txid = callee;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		if (ctx.profile_thread_cputime) {
			step.start_cpu = (int) (SysJMX.getCurrentThreadCPU() - ctx.startCpu);
		}
		step.hash = DataProxy.sendApicall("local-forward");
		ctx.profile.add(step);
	}
	public void receive(WrContext ctx, int uuid) {
		TransferMap.ID id = TransferMap.get(uuid);
		if (id == null)
			return;

		TransferMap.remove(uuid);
		if (id.gxid != 0) {
			ctx.inner().gxid = id.gxid;
		}
		if (id.callee != 0) {
			ctx.inner().txid = id.callee;
		}
		if (id.caller != 0) {
			ctx.inner().caller = id.caller;
		}
		ctx.inner().xType = id.xType;
	}
}
