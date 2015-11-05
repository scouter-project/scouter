package scouter.xtra.httpclient;

import java.lang.reflect.Method;

import scouter.agent.Logger;
import scouter.agent.proxy.IHttpClient;

public class NettyHttpClient implements IHttpClient {
	static class RefMethod {
		Class clazz = null;
		Method method = null;

		public boolean isOk(Class class1) {
			return this.method != null && class1 == this.clazz;
		}
	}

	Class[] param = {};
	Object[] args = {};
	RefMethod getHost = null;

	public String getHost(Object o) {
		try {
			if (o.getClass().getName().lastIndexOf("Server") >= 0) {
				if (getHost == null) {
					getHost = new RefMethod();
					getHost.clazz = o.getClass();
					getHost.method = o.getClass().getMethod("getHostPort", new Class[0]);
					return (String) getHost.method.invoke(o, args);
				}
				if (getHost.isOk(o.getClass())) {
					return (String) getHost.method.invoke(o, args);
				}
			}
		} catch (Exception e) {
			getHost = new RefMethod();
		}
		return null;
	}

	RefMethod addHeader = null;

	public void addHeader(Object o, String key, String value) {
		if (o == null)
			return;
		try {
			if (addHeader == null) {
				if (o.getClass().getName().lastIndexOf("HttpClientRequest") >= 0) {
					addHeader = new RefMethod();
					addHeader.clazz = o.getClass();
					addHeader.method = o.getClass().getMethod("addHeader", new Class[] { String.class, String.class });
					addHeader.method.invoke(o, new Object[] { key, value });
					return;
				}
			} else if (addHeader.isOk(o.getClass())) {
				addHeader.method.invoke(o, new Object[] { key, value });
				return;
			}
		} catch (Exception e) {
			addHeader = new RefMethod();
			Logger.println("NETTY", e);
		}
	}

	RefMethod getHeader = null;

	public java.lang.String getHeader(Object o, java.lang.String key) {
		if (o == null)
			return null;

		try {
			if (getHeader == null) {
				if (o.getClass().getName().lastIndexOf("HttpClientRequest") >= 0) {
					getHeader = new RefMethod();
					getHeader.clazz = o.getClass();
					getHeader.method = o.getClass().getMethod("getHeaders", new Class[] {});
					return get(getHeader.method.invoke(o, new Object[] {}), key);
				}
			} else if (getHeader.isOk(o.getClass())) {
				return get(getHeader.method.invoke(o, new Object[] {}), key);
			}
		} catch (Exception e) {
			addHeader = new RefMethod();
			Logger.println("NETTY", e);
		}

		return null;
	}

	RefMethod getHeader2 = null;

	private String get(Object o, String key) {
		if (o == null)
			return null;
		try {
			if (getHeader2 == null) {
				getHeader2 = new RefMethod();
				getHeader2.clazz = o.getClass();
				getHeader2.method = o.getClass().getMethod("getHeaders", new Class[] { String.class });
				return (String) getHeader2.method.invoke(o, new Object[] { key });
			} else if (getHeader2.isOk(o.getClass())) {
				return (String) getHeader2.method.invoke(o, new Object[] { key });
			}
		} catch (Exception e) {
			getHeader2 = new RefMethod();
			getHeader = new RefMethod();
			Logger.println("NETTY", e);
		}
		return null;
	}

	RefMethod getURI = null;

	public String getURI(Object o) {
		if (o == null)
			return null;
		try {
			if (getURI == null) {
				if (o.getClass().getName().lastIndexOf("HttpClientRequest") >= 0) {
					getURI = new RefMethod();
					getURI.clazz = o.getClass();
					getURI.method = o.getClass().getMethod("getUri", new Class[] {});
					return (String) getHeader2.method.invoke(o, new Object[] {});
				}
			} else if (getURI.isOk(o.getClass())) {
				return (String) getURI.method.invoke(o, new Object[] {});
			}
		} catch (Exception e) {
			getURI = new RefMethod();
			Logger.println("NETTY", e);
		}
		return null;
	}
}
