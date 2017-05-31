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
package scouter.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import scouter.lang.pack.XLogPack;

public class ClassUtil {
	public static <V> Map<String, V> getPublicFinalNameMap(Class<?> cls, Class v) {

		Map<String, V> map = new HashMap<String, V>();
		Field[] fields = cls.getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(v) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put(name, (V) value);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}

	public static <V> Map<V, String> getPublicFinalValueMap(Class<?> cls, Class type) {

		Map<V, String> map = new HashMap<V, String>();
		Field[] fields = cls.getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(type) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put((V) value, name);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}

	public static <V> Map<V, String> getPublicFinalDeclaredValueMap(Class<?> cls, Class type) {

		Map<V, String> map = new HashMap<V, String>();
		Field[] fields = cls.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(type) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put((V) value, name);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}

	public static String getClassDescription(Class c1) {
		int x = c1.getName().lastIndexOf(".");

		StringBuffer sb = new StringBuffer();
		if (x > 0) {
			sb.append("package ").append(c1.getName().substring(0, x)).append(";\n\n");
		}
		int acc = c1.getModifiers();
		mod(sb, acc, c1.isInterface());
		if (c1.isInterface()) {
			sb.append("interface ");
		} else {
			sb.append("class ");
		}

		if (x > 0) {
			sb.append(c1.getName().substring(x + 1));
		} else {
			sb.append(c1.getName());
		}
		if (c1.getSuperclass() != null && c1.getSuperclass() != Object.class) {
			sb.append(" extends ").append(c1.getSuperclass().getName());
		}
		Class[] inf = c1.getInterfaces();
		for (int i = 0; i < inf.length; i++) {
			if (i == 0) {
				sb.append(" implements ");
			}
			if (i > 0)
				sb.append(",");
			sb.append(inf[i].getName());
		}
		sb.append("{\n");
		Field[] f = c1.getDeclaredFields();
		for (int i = 0; i < f.length; i++) {
			sb.append("\t");
			mod(sb, f[i].getModifiers(), c1.isInterface());
			sb.append(toClassString(f[i].getType().getName())).append(" ");
			sb.append(f[i].getName()).append(";\n");
		}
		Method[] m = c1.getDeclaredMethods();
		if (f.length > 0 && m.length > 0) {
			sb.append("\n");
		}
		for (int i = 0; i < m.length; i++) {
			sb.append("\t");
			mod(sb, m[i].getModifiers(), c1.isInterface());
			sb.append(toClassString(m[i].getReturnType().getName())).append(" ");
			sb.append(m[i].getName());
			sb.append("(");
			Class[] pc = m[i].getParameterTypes();
			for (int p = 0; p < pc.length; p++) {
				if (p > 0)
					sb.append(",");
				sb.append(toClassString(pc[p].getName())).append(" a" + p);
			}
			sb.append(")");
			if (Modifier.isAbstract(m[i].getModifiers()) == false) {
				sb.append("{...}\n");
			} else {
				sb.append(";\n");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	private static String toClassString(String name) {
		if (name.startsWith("java.lang")) {
			return name.substring("java.lang".length() + 1);
		}
		return name;
	}

	private static void mod(StringBuffer sb, int acc, boolean isInterface) {
		if (Modifier.isAbstract(acc) && isInterface == false) {
			sb.append("abstract ");
		}
		if (Modifier.isProtected(acc)) {
			sb.append("protected ");
		}
		if (Modifier.isPrivate(acc)) {
			sb.append("private ");
		}
		if (Modifier.isPublic(acc)) {
			sb.append("public ");
		}
		if (Modifier.isFinal(acc)) {
			sb.append("final ");
		}
		if (Modifier.isSynchronized(acc)) {
			sb.append("synchronized ");
		}
	}

	public static byte[] getByteCode(Class c) {
		if (c == null)
			return null;
		String clsAsResource = "/" + c.getName().replace('.', '/').concat(".class");
		InputStream in = null;
		try {
			in = c.getResourceAsStream(clsAsResource);
			if (in == null) {
				return null;
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] buff = new byte[1024];
			int n = 0;
			while ((n = in.read(buff, 0, 1024)) >= 0) {
				out.write(buff, 0, n);
			}
			return out.toByteArray();
		} catch (Exception e) {
		} finally {
			FileUtil.close(in);
		}
		return null;
	}

	public static void main(String[] args) {
		System.out.println(getClassDescription(XLogPack.class));
	}
}
