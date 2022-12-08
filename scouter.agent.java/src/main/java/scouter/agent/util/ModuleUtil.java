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

package scouter.agent.util;

import scouter.agent.JavaAgent;
import scouter.agent.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * refer to glowroot (https://github.com/glowroot/glowroot)
 */
public class ModuleUtil {

    private static final ModuleUtil instance;

    static {
        if (JavaAgent.isJava9plus()) {
            instance = new ModuleUtil();
        } else {
            instance = null;
        }
    }

    private final Method getModuleMethod;
    private final Class moduleClass;
    private final Method redefineModuleMethod;

    private ModuleUtil() {
        try {
            getModuleMethod = Class.class.getMethod("getModule");
            moduleClass = Class.forName("java.lang.Module");
            redefineModuleMethod = Instrumentation.class.getMethod("redefineModule",
                    moduleClass, Set.class, Map.class, Map.class, Set.class, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object getModule(Class clazz) throws Exception {
        return instance.getModuleInternal(clazz);
    }

    public static void grantAccess(Instrumentation instrumentation, String fromClassName,
            String toClassName) throws Exception {
        instance.grantAccessInternal(instrumentation, fromClassName, null, toClassName, null);
    }

    public static void grantAccess(Instrumentation instrumentation, String fromClassName,
            ClassLoader fromClassLoader,
            String toClassName, ClassLoader toClassLoader) throws Exception {
        instance.grantAccessInternal(instrumentation, fromClassName, fromClassLoader, toClassName,
                toClassLoader);
    }

    private Object getModuleInternal(Class clazz) throws Exception {
        // getModule() always returns non-null
        return getModuleMethod.invoke(clazz);
    }

    private void grantAccessInternal(Instrumentation instrumentation, String fromClassName,
            ClassLoader fromClassLoader,
            String toClassName, ClassLoader toClassLoader) throws Exception {
        Class fromClass;
        try {
            if (fromClassLoader == null) {
                fromClass = Class.forName(fromClassName);
            } else {
                fromClass = Class.forName(fromClassName, true, fromClassLoader);
            }
        } catch (ClassNotFoundException e) {
            Logger.println("MODULEUTIL-1", e.getMessage() + " : " + fromClassName, e);
            return;
        }
        Class toClass;
        try {
            if (toClassLoader == null) {
                toClass = Class.forName(toClassName);
            } else {
                toClass = Class.forName(toClassName, true, toClassLoader);
            }
        } catch (ClassNotFoundException e) {
            Logger.println("MODULEUTIL-2", e.getMessage() + " : " + toClassName, e);
            return;
        }
        Map extraOpens = new HashMap();
        Package pkg = toClass.getPackage();
        if (pkg != null) {
            Set openSet = new HashSet();
            openSet.add(getModuleMethod.invoke(fromClass));
            extraOpens.put(pkg.getName(), openSet);
        }
        // getModule() always returns non-null
        redefineModuleMethod.invoke(instrumentation, getModuleMethod.invoke(toClass),
                new HashSet(), new HashMap(), extraOpens, new HashSet(), new HashMap());
        Logger.println("extra opens module. from = " + fromClassName + " to = " + toClassName);
    }

}
