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
import scouter.agent.netio.data.DataProxy;
import scouter.agent.summary.ErrorData;
import scouter.agent.summary.ServiceSummary;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.value.MapValue;
import scouter.util.*;

import java.lang.instrument.ClassDefinition;
import java.util.ArrayList;

import static scouter.agent.util.DumpUtil.conf;

public class AsyncRunner extends Thread {

    private static AsyncRunner instance = null;
    private RequestQueue<Object> queue = new RequestQueue<Object>(1024);

    public final static synchronized AsyncRunner getInstance() {
        if (instance == null) {
            instance = new AsyncRunner();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            instance.start();
        }
        return instance;
    }

    private static class Hook {
        public Hook(ClassLoader loader, String classname, byte[] body) {
            super();
            this.loader = loader;
            this.classname = classname.replace('/', '.');
            this.body = body;
        }
        ClassLoader loader;
        String classname;
        byte[] body;
    }

    private static class RedefineClasses {
        StringSet classnames;
        public RedefineClasses(StringSet classnames) {
            this.classnames = classnames;
        }
    }

    public void add(ClassLoader loader, String classname, byte[] body) {
        queue.put(new Hook(loader, classname, body));
    }

    public void add(LeakInfo data) {
        queue.put(data);
    }

    public void add(LeakInfo2 data) {
        queue.put(data);
    }

    public void add(Runnable r) {
        queue.put(r);
    }

    public void add(StringSet classnames) {
        queue.put(new RedefineClasses(classnames));
    }

    public void run() {
        while (true) {
            Object m = queue.get(1000);
            try {
                if (m instanceof Hook) {
                    hooking((Hook) m);
                } else if (m instanceof LeakInfo) {
                    alert((LeakInfo) m);
                } else if (m instanceof LeakInfo2) {
                    alert((LeakInfo2) m);
                } else if (m instanceof Runnable) {
                    process((Runnable) m);
                } else if (m instanceof RedefineClasses) {
                    redefine(((RedefineClasses) m).classnames);
                }
            } catch (Throwable t) {
            	t.printStackTrace();
            }
        }
    }

    private void redefine(StringSet clazzes) {
        Class[] loadedClasses = JavaAgent.getInstrumentation().getAllLoadedClasses();

        ArrayList<ClassDefinition> definitionList = new ArrayList<ClassDefinition>();
        boolean allSuccess = true;
        for (int i = 0; i < loadedClasses.length; i++) {
            if (clazzes.hasKey(loadedClasses[i].getName())) {
                try {
                    byte[] buff = ClassUtil.getByteCode(loadedClasses[i]);
                    if (buff == null) {
                        continue;
                    }
                    definitionList.add(new ClassDefinition(loadedClasses[i], buff));
                } catch (Exception e) {
                    allSuccess = false;
                    break;
                }
            }
        }
        if (definitionList.size() > 0 && allSuccess) {
            try {
                JavaAgent.getInstrumentation().redefineClasses(definitionList.toArray(new ClassDefinition[definitionList.size()]));
            } catch (Throwable th) {
                Logger.println("A183", "redefine error : " + loadedClasses);
            }
        }
    }

    private void process(Runnable r) {
        r.run();
    }

    private void alert(LeakInfo leakInfo) {
        ServiceSummary summary = ServiceSummary.getInstance();

        MapValue mv = new MapValue();
        mv.put(AlertPack.HASH_FLAG + TextTypes.SERVICE + "_service-name", leakInfo.serviceHash);

        if (leakInfo.fullstack) {
            ErrorData d = summary.process(leakInfo.error, 0, leakInfo.serviceHash, leakInfo.txid, 0, 0);
            Logger.println("A156", leakInfo.error + " " + leakInfo.inner);
            if (d != null && d.fullstack == 0) {
                String fullstack = ThreadUtil.getStackTrace(leakInfo.error.getStackTrace(), leakInfo.fullstackSkip);
                d.fullstack = DataProxy.sendError(fullstack);
                Logger.println("A157", fullstack);
            }

            mv.put(AlertPack.HASH_FLAG + TextTypes.ERROR + "_full-stack", d.fullstack);

        } else {
            summary.process(leakInfo.error, 0, leakInfo.serviceHash, leakInfo.txid, 0, 0);
            Logger.println("A179", leakInfo.error + " " + leakInfo.inner);
        }

        DataProxy.sendAlert(AlertLevel.WARN, "CONNECTION_NOT_CLOSE", "Connection may not closed", mv);
    }

    private void alert(LeakInfo2 leakInfo2) {
        ServiceSummary summary = ServiceSummary.getInstance();

        MapValue mv = new MapValue();
        mv.put(AlertPack.HASH_FLAG + TextTypes.SERVICE + "_service-name", leakInfo2.serviceHash);

        if (leakInfo2.fullstack) {
            ErrorData d = summary.process(leakInfo2.error, 0, leakInfo2.serviceHash, leakInfo2.txid, 0, 0);
            Logger.println("A156", leakInfo2.error + " " + leakInfo2.innerObject);
            if (d != null && d.fullstack == 0) {
                String fullstack = ThreadUtil.getStackTrace(leakInfo2.error.getStackTrace(), leakInfo2.fullstackSkip);
                d.fullstack = DataProxy.sendError(fullstack);
                Logger.println("A157", fullstack);
            }
            mv.put(AlertPack.HASH_FLAG + TextTypes.ERROR + "_full-stack", d.fullstack);

        } else {
            summary.process(leakInfo2.error, 0, leakInfo2.serviceHash, leakInfo2.txid, 0, 0);
            Logger.println("A179", leakInfo2.error + " " + leakInfo2.innerObject);
        }
        DataProxy.sendAlert(AlertLevel.WARN, "CONNECTION_NOT_CLOSE", "Connection may not closed", mv);

        if(conf._trace) Logger.trace("[Force-Close-InnerObject]" + System.identityHashCode(leakInfo2.innerObject));

        boolean closeResult = leakInfo2.closeManager.close(leakInfo2.innerObject);
        //Logger.println("G003", "connection auto closed:" + closeResult);
    }

    private void hooking(Hook m) {
        // Never use dynamic hooking on AIX with JDK1.5
        if (SystemUtil.IS_AIX && SystemUtil.IS_JAVA_1_5) {
            return;
        }
        try {
            Class cls = Class.forName(m.classname, false, m.loader);
            ClassDefinition[] cd = new ClassDefinition[1];
            cd[0] = new ClassDefinition(cls, m.body);
            JavaAgent.getInstrumentation().redefineClasses(cd);
        } catch (Throwable t) {
            Logger.println("A149", "async hook fail:" + m.classname + " " + t);
        }
    }
}
