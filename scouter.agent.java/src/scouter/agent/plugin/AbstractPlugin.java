package scouter.agent.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
import scouter.util.ThreadUtil;

public class AbstractPlugin {
	long lastModified;

	public void log(Object c) {
		Logger.println("A158", c.toString());
	}

	public void println(Object c) {
		System.out.println(c);
	}

	public Object field(Object o, String field) {
		if (o == null)
			return null;
		try {
			Field f = o.getClass().getField(field);
			f.setAccessible(true);
			return f.get(o);
		} catch (Throwable e) {
		}
		return null;
	}

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
