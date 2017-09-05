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
package scouter.server.plugin;

import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.server.Logger;
import scouter.server.core.AlertCore;
import scouter.server.db.TextRD;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for script plugin
 * Created by gunlee on 2017. 8. 18.
 * @since v1.7.3
 */
public class PluginHelper {
	private static final String NO_DATE = "00000000";
	private static Map<String, AccessibleObject> reflCache = Collections.synchronizedMap(new LinkedHashMap<String, AccessibleObject>(100));

	private static PluginHelper instance =new PluginHelper();
	private PluginHelper() {}

	public static PluginHelper getInstance() {
		return instance;
	}

	public void log(Object c) {
		Logger.println(c);
	}

	public void println(Object c) {
		System.out.println(c);
	}

	public NumberFormat getNumberFormatter() {
		return getNumberFormatter(1);
	}

	public NumberFormat getNumberFormatter(int fractionDigits) {
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(fractionDigits);
		return f;
	}

	public String formatNumber(float f) {
		return formatNumber(f, 1);
	}

	public String formatNumber(float f, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(f);
	}

	public String formatNumber(double v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(double v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public String formatNumber(int v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(int v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public String formatNumber(long v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(long v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public void alertInfo(int objHash, String objType, String title, String message) {
		alert(AlertLevel.INFO, objHash, objType, title, message);
	}
	public void alertWarn(int objHash, String objType, String title, String message) {
		alert(AlertLevel.WARN, objHash, objType, title, message);
	}
	public void alertError(int objHash, String objType, String title, String message) {
		alert(AlertLevel.ERROR, objHash, objType, title, message);
	}
	public void alertFatal(int objHash, String objType, String title, String message) {
		alert(AlertLevel.FATAL, objHash, objType, title, message);
	}

	public void alert(byte level, int objHash, String objType, String title, String message) {
		AlertPack p = new AlertPack();
		p.time = System.currentTimeMillis();
		p.level = level;
		p.objHash = objHash;
		p.objType = objType;
		p.title = title;
		p.message = message;
		AlertCore.add(p);
	}

	public String getErrorString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getErrorString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.ERROR, hash);
	}

	public String getApicallString(int hash) {
		return getApicallString(NO_DATE, hash);
	}
	public String getApicallString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.APICALL, hash);
	}

	public String getMethodString(int hash) {
		return getMethodString(NO_DATE, hash);
	}
	public String getMethodString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.METHOD, hash);
	}

	public String getServiceString(int hash) {
		return getServiceString(NO_DATE, hash);
	}
	public String getServiceString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.SERVICE, hash);
	}

	public String getSqlString(int hash) {
		return getSqlString(NO_DATE, hash);
	}
	public String getSqlString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.SQL, hash);
	}

	public String getObjectString(int hash) {
		return getObjectString(NO_DATE, hash);
	}
	public String getObjectString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.OBJECT, hash);
	}

	public String getRefererString(int hash) {
		return getRefererString(NO_DATE, hash);
	}
	public String getRefererString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.REFERER, hash);
	}

	public String getUserAgentString(int hash) {
		return getUserAgentString(NO_DATE, hash);
	}
	public String getUserAgentString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.USER_AGENT, hash);
	}

	public String getUserGroupString(int hash) {
		return getUserGroupString(NO_DATE, hash);
	}
	public String getUserGroupString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.GROUP, hash);
	}

	public String getCityString(int hash) {
		return getCityString(NO_DATE, hash);
	}
	public String getCityString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.CITY, hash);
	}

	public String getLoginString(int hash) {
		return getLoginString(NO_DATE, hash);
	}
	public String getLoginString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.LOGIN, hash);
	}

	public String getDescString(int hash) {
		return getDescString(NO_DATE, hash);
	}
	public String getDescString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.DESC, hash);
	}

	public String getWebString(int hash) {
		return getWebString(NO_DATE, hash);
	}
	public String getWebString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.WEB, hash);
	}

	public String getHashMsgString(int hash) {
		return getHashMsgString(NO_DATE, hash);
	}
	public String getHashMsgString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.HASH_MSG, hash);
	}

	public static Object invokeMethod(Object o, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object[] objs = {};
		return invokeMethod(o, methodName, objs);
	}

	public static Object invokeMethod(Object o, String methodName, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		int argsSize = args.length;
		StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(methodName).append("():");

		Class[] argClazzes = new Class[argsSize];

		for(int i=0; i<argsSize; i++) {
			argClazzes[i] = args[i].getClass();
		}

		return invokeMethod(o, methodName, argClazzes, args);
	}

	public static Object invokeMethod(Object o, String methodName, Class[] argTypes, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		int argsSize = args.length;
		StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(methodName).append("():");

		for(int i=0; i<argsSize; i++) {
			signature.append(argTypes[i].getName()).append("+");
		}
		Method m = (Method) reflCache.get(signature.toString());
		if(m == null) {
			m = o.getClass().getMethod(methodName, argTypes);
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

	public static Object newInstance(String className, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		return newInstance(className, Thread.currentThread().getContextClassLoader(), args);
	}

	public static Object newInstance(String className, ClassLoader loader, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		int argsSize = args.length;
		Class[] argClazzes = new Class[argsSize];

		for(int i=0; i<argsSize; i++) {
			argClazzes[i] = args[i].getClass();
		}

		return newInstance(className, loader, argClazzes, args);
	}

	public static Object newInstance(String className, ClassLoader loader, Class[] argTypes, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		int argsSize = args.length;

		StringBuilder signature = new StringBuilder(className).append(":<init>:");

		for(int i=0; i<argsSize; i++) {
			signature.append(argTypes[i].getName()).append("+");
		}

		Class clazz = Class.forName(className, true, loader);
		Constructor constructor = (Constructor)reflCache.get(signature.toString());

		if(constructor == null) {
			constructor = clazz.getConstructor(argTypes);
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

	public static Class[] makeArgTypes(Class class0) {
		Class[] classes = new Class[1];
		classes[0] = class0;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1) {
		Class[] classes = new Class[2];
		classes[0] = class0;
		classes[1] = class1;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2) {
		Class[] classes = new Class[3];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3) {
		Class[] classes = new Class[4];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		classes[3] = class3;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4) {
		Class[] classes = new Class[5];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		classes[3] = class3;
		classes[4] = class4;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5) {
		Class[] classes = new Class[6];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		classes[3] = class3;
		classes[4] = class4;
		classes[5] = class5;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5, Class class6) {
		Class[] classes = new Class[7];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		classes[3] = class3;
		classes[4] = class4;
		classes[5] = class5;
		classes[6] = class6;
		return classes;
	}

	public static Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5, Class class6, Class class7) {
		Class[] classes = new Class[8];
		classes[0] = class0;
		classes[1] = class1;
		classes[2] = class2;
		classes[3] = class3;
		classes[4] = class4;
		classes[5] = class5;
		classes[6] = class6;
		classes[7] = class7;
		return classes;
	}

	public static Object[] makeArgs(Object object0) {
		Object[] objects = new Object[1];
		objects[0] = object0;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1) {
		Object[] objects = new Object[2];
		objects[0] = object0;
		objects[1] = object1;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2) {
		Object[] objects = new Object[3];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2, Object object3) {
		Object[] objects = new Object[4];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		objects[3] = object3;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4) {
		Object[] objects = new Object[5];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		objects[3] = object3;
		objects[4] = object4;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5) {
		Object[] objects = new Object[6];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		objects[3] = object3;
		objects[4] = object4;
		objects[5] = object5;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5, Object object6) {
		Object[] objects = new Object[7];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		objects[3] = object3;
		objects[4] = object4;
		objects[5] = object5;
		objects[6] = object6;
		return objects;
	}

	public static Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
		Object[] objects = new Object[8];
		objects[0] = object0;
		objects[1] = object1;
		objects[2] = object2;
		objects[3] = object3;
		objects[4] = object4;
		objects[5] = object5;
		objects[6] = object6;
		objects[7] = object7;
		return objects;
	}
}

