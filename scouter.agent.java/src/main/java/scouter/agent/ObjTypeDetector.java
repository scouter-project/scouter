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

package scouter.agent;

import scouter.lang.counters.CounterConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjTypeDetector {

    public static Map<String, String> bootClass = new HashMap<String, String>();
    public static Set<String> extClass = new HashSet<String>();

    static {
        bootClass.put("org/eclipse/jetty/server/Server", CounterConstants.JETTY);
        bootClass.put("org/jboss/Main", CounterConstants.JBOSS); // jboss as 6.1.0
        bootClass.put("org/jboss/as/server/Main", CounterConstants.JBOSS); // jboss as 7.2.0 final
        bootClass.put("org/apache/catalina/startup/Bootstrap", CounterConstants.TOMCAT);
        bootClass.put("org/apache/catalina/startup/Tomcat", CounterConstants.TOMCAT);
        bootClass.put("com/caucho/server/resin/Resin", CounterConstants.RESIN); // resin 4.x
        bootClass.put("com/sun/enterprise/glassfish/bootstrap/ASMain", CounterConstants.GLASSFISH);
        bootClass.put("weblogic/Server", CounterConstants.WEBLOGIC);
        bootClass.put("com/ibm/wsspi/bootstrap/WSPreLauncher", CounterConstants.WEBSPHERE);
    }

    public static String objType = null;
    public static String drivedType = null;
    public static String objExtType = null;

    private static boolean initLog = false;

    public static void check(String className) {
        String type = bootClass.get(className);
        if (type == null)
            return;
        if (extClass.contains(type)) {
            drivedType = type;
        } else {
            objType = type;
        }

        Configure.getInstance().resetObjInfo();
        if (initLog == false) {
            Logger.initializer.run();
            initLog = true;
        }

        dirtyConfig = true;
    }

    public static boolean dirtyConfig = false;

}