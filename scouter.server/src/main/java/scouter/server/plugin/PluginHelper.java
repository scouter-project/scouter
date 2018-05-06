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

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.conf.ConfigDesc;
import scouter.lang.conf.Internal;
import scouter.lang.conf.ParamDesc;
import scouter.lang.pack.AlertPack;
import scouter.server.Logger;
import scouter.server.core.AlertCore;
import scouter.server.db.KeyValueStoreRW;
import scouter.server.db.TextRD;
import scouter.util.HashUtil;
import scouter.util.Hexa32;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static scouter.lang.constants.ScouterConstants.SHORTENER_KEY_SPACE;

/**
 * Utility class for script plugin
 * Created by gunlee on 2017. 8. 18.
 *
 * @since v1.7.3
 */
public class PluginHelper {
    private static final String NO_DATE = "00000000";
    private static Map<String, AccessibleObject> reflCache = Collections.synchronizedMap(new LinkedHashMap<String, AccessibleObject>(100));

    private static PluginHelper instance = new PluginHelper();

    private PluginHelper() {
    }

    public static PluginHelper getInstance() {
        return instance;
    }

    @ConfigDesc("logging")
    public void log(Object c) {
        Logger.println(c);
    }

    @ConfigDesc("System.out.println")
    public void println(Object c) {
        System.out.println(c);
    }

    @ConfigDesc("url encoding")
    public String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }

    @ConfigDesc("url decoding")
    public String urlDecode(String s) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, "UTF-8");
    }

    @ConfigDesc("make shorten url.")
    public String toShortenUrl(String producerUrl, String urlToShortening) throws UnsupportedEncodingException {
        String shorten = Hexa32.toString32(HashUtil.hash(urlToShortening));
        KeyValueStoreRW.set(SHORTENER_KEY_SPACE, shorten, urlToShortening);
        return producerUrl + "/" + shorten;
    }

    @ConfigDesc("get NumberFormatter set fraction 1")
    public NumberFormat getNumberFormatter() {
        return getNumberFormatter(1);
    }

    @ConfigDesc("get NumberFormatter set the fraction")
    @ParamDesc("int fractionDigits")
    public NumberFormat getNumberFormatter(int fractionDigits) {
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(fractionDigits);
        return f;
    }

    @ConfigDesc("format the number as fraction 1")
    public String formatNumber(float f) {
        return formatNumber(f, 1);
    }

    @ConfigDesc("format the number as given fraction")
    @ParamDesc("float f, int fractionDigits")
    public String formatNumber(float f, int fractionDigits) {
        return getNumberFormatter(fractionDigits).format(f);
    }

    @ConfigDesc("format the number as fraction 1")
    public String formatNumber(double v) {
        return formatNumber(v, 1);
    }

    @ConfigDesc("format the number as given fraction")
    @ParamDesc("double v, int fractionDigits")
    public String formatNumber(double v, int fractionDigits) {
        return getNumberFormatter(fractionDigits).format(v);
    }

    @ConfigDesc("format the number as fraction 1")
    public String formatNumber(int v) {
        return formatNumber(v, 1);
    }

    @ConfigDesc("format the number as given fraction")
    @ParamDesc("int v, int fractionDigits")
    public String formatNumber(int v, int fractionDigits) {
        return getNumberFormatter(fractionDigits).format(v);
    }

    @ConfigDesc("format the number as fraction 1")
    public String formatNumber(long v) {
        return formatNumber(v, 1);
    }

    @ConfigDesc("format the number as given fraction")
    @ParamDesc("long v, int fractionDigits")
    public String formatNumber(long v, int fractionDigits) {
        return getNumberFormatter(fractionDigits).format(v);
    }

    @ConfigDesc("alert as info level")
    @ParamDesc("int objHash, String objType, String title, String message")
    public void alertInfo(int objHash, String objType, String title, String message) {
        alert(AlertLevel.INFO, objHash, objType, title, message);
    }

    @ConfigDesc("alert as warn level")
    @ParamDesc("int objHash, String objType, String title, String message")
    public void alertWarn(int objHash, String objType, String title, String message) {
        alert(AlertLevel.WARN, objHash, objType, title, message);
    }

    @ConfigDesc("alert as error level")
    @ParamDesc("int objHash, String objType, String title, String message")
    public void alertError(int objHash, String objType, String title, String message) {
        alert(AlertLevel.ERROR, objHash, objType, title, message);
    }

    @ConfigDesc("alert as fatal level")
    @ParamDesc("int objHash, String objType, String title, String message")
    public void alertFatal(int objHash, String objType, String title, String message) {
        alert(AlertLevel.FATAL, objHash, objType, title, message);
    }

    @ConfigDesc("make hashMap easily")
    @ParamDesc("String[] keyValueArray")
    public HashMap<String, String> toMap(String[] keyValueArray) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < keyValueArray.length; i+=2) {
            map.put(keyValueArray[i], keyValueArray[i+1]);
        }
        return map;
    }

    @ConfigDesc("request http get")
    @ParamDesc("String url, Map<String, String> paramMap")
    public void httpGet(String _url, Map<String, String> paramMap) {
        httpGet(_url, paramMap, 1000);
    }

    @ConfigDesc("request http get")
    @ParamDesc("String url, Map<String, String> paramMap, int timeoutMillis")
    public void httpGet(String _url, Map<String, String> paramMap, int timeoutMillis) {
        CloseableHttpResponse response = null;

        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(1000)
                    .setConnectionRequestTimeout(1000)
                    .setSocketTimeout(timeoutMillis).build();

            CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            HttpGet httpGet = new HttpGet(_url + "?" + getParamsString(paramMap));

            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);

        } catch(Exception e) {
            e.printStackTrace();

        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }

    }

    @Internal
    public String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
        if (params == null || params.size() == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }

    @Internal
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

    @Internal
    public String getErrorString(int hash) {
        return getErrorString(NO_DATE, hash);
    }

    @Internal
    public String getErrorString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.ERROR, hash);
    }

    @Internal
    public String getApicallString(int hash) {
        return getApicallString(NO_DATE, hash);
    }

    @Internal
    public String getApicallString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.APICALL, hash);
    }

    @Internal
    public String getMethodString(int hash) {
        return getMethodString(NO_DATE, hash);
    }

    @Internal
    public String getMethodString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.METHOD, hash);
    }

    @Internal
    public String getServiceString(int hash) {
        return getServiceString(NO_DATE, hash);
    }

    @Internal
    public String getServiceString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.SERVICE, hash);
    }

    @Internal
    public String getSqlString(int hash) {
        return getSqlString(NO_DATE, hash);
    }

    @Internal
    public String getSqlString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.SQL, hash);
    }

    @Internal
    public String getObjectString(int hash) {
        return getObjectString(NO_DATE, hash);
    }

    @Internal
    public String getObjectString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.OBJECT, hash);
    }

    @Internal
    public String getRefererString(int hash) {
        return getRefererString(NO_DATE, hash);
    }

    @Internal
    public String getRefererString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.REFERER, hash);
    }

    @Internal
    public String getUserAgentString(int hash) {
        return getUserAgentString(NO_DATE, hash);
    }

    @Internal
    public String getUserAgentString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.USER_AGENT, hash);
    }

    @Internal
    public String getUserGroupString(int hash) {
        return getUserGroupString(NO_DATE, hash);
    }

    @Internal
    public String getUserGroupString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.GROUP, hash);
    }

    @Internal
    public String getCityString(int hash) {
        return getCityString(NO_DATE, hash);
    }

    @Internal
    public String getCityString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.CITY, hash);
    }

    @Internal
    public String getLoginString(int hash) {
        return getLoginString(NO_DATE, hash);
    }

    @Internal
    public String getLoginString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.LOGIN, hash);
    }

    @Internal
    public String getDescString(int hash) {
        return getDescString(NO_DATE, hash);
    }

    @Internal
    public String getDescString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.DESC, hash);
    }

    @Internal
    public String getWebString(int hash) {
        return getWebString(NO_DATE, hash);
    }

    @Internal
    public String getWebString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.WEB, hash);
    }

    @Internal
    public String getHashMsgString(int hash) {
        return getHashMsgString(NO_DATE, hash);
    }

    @Internal
    public String getHashMsgString(String yyyymmdd, int hash) {
        return TextRD.getString(yyyymmdd, TextTypes.HASH_MSG, hash);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object invokeMethod(Object o, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object[] objs = {};
        return invokeMethod(o, methodName, objs);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object invokeMethod(Object o, String methodName, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int argsSize = args.length;
        StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(methodName).append("():");

        Class[] argClazzes = new Class[argsSize];

        for (int i = 0; i < argsSize; i++) {
            argClazzes[i] = args[i].getClass();
        }

        return invokeMethod(o, methodName, argClazzes, args);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object invokeMethod(Object o, String methodName, Class[] argTypes, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int argsSize = args.length;
        StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(methodName).append("():");

        for (int i = 0; i < argsSize; i++) {
            signature.append(argTypes[i].getName()).append("+");
        }
        Method m = (Method) reflCache.get(signature.toString());
        if (m == null) {
            m = o.getClass().getMethod(methodName, argTypes);
            reflCache.put(signature.toString(), m);
        }
        return m.invoke(o, args);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object newInstance(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return newInstance(className, Thread.currentThread().getContextClassLoader());
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object newInstance(String className, ClassLoader loader) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] objs = {};
        return newInstance(className, loader, objs);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object newInstance(String className, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return newInstance(className, Thread.currentThread().getContextClassLoader(), args);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object newInstance(String className, ClassLoader loader, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        int argsSize = args.length;
        Class[] argClazzes = new Class[argsSize];

        for (int i = 0; i < argsSize; i++) {
            argClazzes[i] = args[i].getClass();
        }

        return newInstance(className, loader, argClazzes, args);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object newInstance(String className, ClassLoader loader, Class[] argTypes, Object[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        int argsSize = args.length;

        StringBuilder signature = new StringBuilder(className).append(":<init>:");

        for (int i = 0; i < argsSize; i++) {
            signature.append(argTypes[i].getName()).append("+");
        }

        Class clazz = Class.forName(className, true, loader);
        Constructor constructor = (Constructor) reflCache.get(signature.toString());

        if (constructor == null) {
            constructor = clazz.getConstructor(argTypes);
            reflCache.put(signature.toString(), constructor);
        }

        return constructor.newInstance(args);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object getFieldValue(Object o, String fieldName) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        StringBuilder signature = new StringBuilder(o.getClass().getName()).append(":").append(fieldName).append(":");
        Field f = (Field) reflCache.get(signature.toString());
        if (f == null) {
            f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            reflCache.put(signature.toString(), f);
        }
        return f.get(o);
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0) {
        Class[] classes = new Class[1];
        classes[0] = class0;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1) {
        Class[] classes = new Class[2];
        classes[0] = class0;
        classes[1] = class1;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2) {
        Class[] classes = new Class[3];
        classes[0] = class0;
        classes[1] = class1;
        classes[2] = class2;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3) {
        Class[] classes = new Class[4];
        classes[0] = class0;
        classes[1] = class1;
        classes[2] = class2;
        classes[3] = class3;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4) {
        Class[] classes = new Class[5];
        classes[0] = class0;
        classes[1] = class1;
        classes[2] = class2;
        classes[3] = class3;
        classes[4] = class4;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5) {
        Class[] classes = new Class[6];
        classes[0] = class0;
        classes[1] = class1;
        classes[2] = class2;
        classes[3] = class3;
        classes[4] = class4;
        classes[5] = class5;
        return classes;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5, Class class6) {
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

    @Internal
    @ConfigDesc("reflection util")
    public Class[] makeArgTypes(Class class0, Class class1, Class class2, Class class3, Class class4, Class class5, Class class6, Class class7) {
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

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0) {
        Object[] objects = new Object[1];
        objects[0] = object0;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1) {
        Object[] objects = new Object[2];
        objects[0] = object0;
        objects[1] = object1;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2) {
        Object[] objects = new Object[3];
        objects[0] = object0;
        objects[1] = object1;
        objects[2] = object2;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2, Object object3) {
        Object[] objects = new Object[4];
        objects[0] = object0;
        objects[1] = object1;
        objects[2] = object2;
        objects[3] = object3;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4) {
        Object[] objects = new Object[5];
        objects[0] = object0;
        objects[1] = object1;
        objects[2] = object2;
        objects[3] = object3;
        objects[4] = object4;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5) {
        Object[] objects = new Object[6];
        objects[0] = object0;
        objects[1] = object1;
        objects[2] = object2;
        objects[3] = object3;
        objects[4] = object4;
        objects[5] = object5;
        return objects;
    }

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5, Object object6) {
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

    @Internal
    @ConfigDesc("reflection util")
    public Object[] makeArgs(Object object0, Object object1, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
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


    public static class Desc {
        public String desc;
        public String methodName;
        public List<String> parameterTypeNames;
        public String returnTypeName;
    }

    private static List<PluginHelper.Desc> pluginHelperDesc;

    public static synchronized List<PluginHelper.Desc> getPluginHelperDesc() {
        if (pluginHelperDesc != null) {
            return pluginHelperDesc;
        }
        List<PluginHelper.Desc> descList = new ArrayList<PluginHelper.Desc>();
        Method[] methods = PluginHelper.class.getDeclaredMethods();
        for (Method method : methods) {
            int mod = method.getModifiers();
            if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
                Deprecated deprecated = method.getAnnotation(Deprecated.class);
                Internal internal = method.getAnnotation(Internal.class);
                if (deprecated != null || internal != null) {
                    continue;
                }

                List<String> typeClassNameList = new ArrayList<String>();

                Class<?>[] clazzes = method.getParameterTypes();
                ParamDesc paramDesc = method.getAnnotation(ParamDesc.class);
                if (paramDesc != null) {
                    typeClassNameList.add(paramDesc.value());
                } else {
                    for (Class<?> clazz : clazzes) {
                        typeClassNameList.add(clazz.getName());
                    }
                }
                ConfigDesc configDesc = method.getAnnotation(ConfigDesc.class);

                PluginHelper.Desc desc = new PluginHelper.Desc();
                desc.methodName = method.getName();
                desc.returnTypeName = method.getReturnType().getName();
                if (configDesc != null) {
                    desc.desc = configDesc.value();
                }
                desc.parameterTypeNames = typeClassNameList;
                descList.add(desc);
            }
        }
        pluginHelperDesc = descList;
        return pluginHelperDesc;
    }
}

