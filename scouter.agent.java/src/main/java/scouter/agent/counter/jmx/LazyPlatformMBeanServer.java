package scouter.agent.counter.jmx;

import static scouter.lang.counters.CounterConstants.JBOSS;
import static scouter.lang.counters.CounterConstants.WEBLOGIC;
import static scouter.lang.counters.CounterConstants.WEBSPHERE;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import scouter.agent.JavaAgent;
import scouter.agent.Logger;
import scouter.agent.ObjTypeDetector;
import scouter.agent.util.ModuleUtil;
import scouter.util.StringIntMap;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

/**
 *  refer to glowroot (https://github.com/glowroot/glowroot)
 **/
public class LazyPlatformMBeanServer {

    private static volatile LazyPlatformMBeanServer instance;
    private MBeanServer platformMBeanServer;

    private final boolean waitForContainerToCreatePlatformMBeanServer;

    Map<String, ObjectName> objectNameMap = new HashMap<String, ObjectName>();
    Set<String> ignoreSet = new HashSet<String>();
    StringIntMap tryCountMap = new StringIntMap();

    public synchronized static LazyPlatformMBeanServer create() {
        if (instance == null) {
            instance = new LazyPlatformMBeanServer();
        }
        return instance;
    }

    private LazyPlatformMBeanServer() {
        waitForContainerToCreatePlatformMBeanServer = JBOSS.equals(ObjTypeDetector.objType)
                || WEBLOGIC.equals(ObjTypeDetector.objType) || WEBSPHERE.equals(ObjTypeDetector.objType);
    }

    public boolean checkInit() throws Exception {
        if (platformMBeanServer != null) return true;
        if (JavaAgent.isJava9plus()) {
            try {
                ModuleUtil.grantAccess(JavaAgent.getInstrumentation(),
                    LazyPlatformMBeanServer.class.getName(),
                    "sun.management.ManagementFactoryHelper");
            } catch (Throwable th) {
                Logger.println("MBEAN-01", th.getMessage(), th);
            }
        }
        if (waitForContainerToCreatePlatformMBeanServer) {
            String platformMBeanServerFieldName = SystemUtil.IS_JAVA_IBM ? "platformServer" : "platformMBeanServer";
            Field platformMBeanServerField =
                    ManagementFactory.class.getDeclaredField(platformMBeanServerFieldName);
            platformMBeanServerField.setAccessible(true);
            if (platformMBeanServerField.get(null) != null) {
                platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
                registerHotspotMbean(platformMBeanServer);
                return true;
            }
        } else {
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            registerHotspotMbean(platformMBeanServer);
            return true;
        }
        return false;
    }

    private void registerHotspotMbean(MBeanServer mbeanServer) throws Exception {
        Class<?> sunManagementFactoryHelperClass =
                Class.forName("sun.management.ManagementFactoryHelper");
        Method registerInternalMBeansMethod = sunManagementFactoryHelperClass
                .getDeclaredMethod("registerInternalMBeans", MBeanServer.class);
        registerInternalMBeansMethod.setAccessible(true);
        registerInternalMBeansMethod.invoke(null, mbeanServer);
    }

    public float getValue(String mbean, String attribute) throws Exception {
        if (!checkInit()) return -1;
        if (ignoreSet.contains(mbean)) return -1;
        String key = mbean + attribute;
        if (ignoreSet.contains(key)) return -1;
        ObjectName objectName = objectNameMap.get(mbean);
        if (objectName == null) {
            objectName = ObjectName.getInstance(mbean);
            if (objectName.isPattern()) {
                Logger.trace(mbean + "is pattern object name");
                ignoreSet.add(mbean);
                return -1;
            }
            objectNameMap.put(mbean, objectName);
        }
        try {
            Object value = platformMBeanServer.getAttribute(objectName, attribute);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else {
                Logger.trace(mbean + " " + attribute + " is not a number : " + value);
                ignoreSet.add(key);
            }
        } catch (InstanceNotFoundException e) {
            // try again next time until 30 times
            int count = tryCountMap.get(key);
            count++;
            if (count > 30) {
                ignoreSet.add(key);
                tryCountMap.remove(key);
            } else {
                tryCountMap.put(key, count);
            }
        } catch (Exception e) {
            Logger.trace(e.getClass().getName() + " : " + mbean + " " + attribute);
            Logger.trace(ThreadUtil.getStackTrace(e));
            ignoreSet.add(key);
        }
        return -1;
    }
}
