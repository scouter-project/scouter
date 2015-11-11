package scouter.agent.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import scouter.agent.Logger;

public class IPlugIn {
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
}
