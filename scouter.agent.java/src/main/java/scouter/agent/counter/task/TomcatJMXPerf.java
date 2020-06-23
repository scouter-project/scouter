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
package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.ObjTypeDetector;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.conf.ConfObserver;
import scouter.lang.counters.CounterConstants;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.ValueEnum;
import scouter.util.CastUtil;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.*;

public class TomcatJMXPerf {
    HashMap<MeterKey, MeterResource> meters = new HashMap<MeterKey, MeterResource>();
    HashMap<MeterKey, Long> lastValues = new HashMap<MeterKey, Long>();
    private static HashSet<String> deltas = new HashSet<String>();

    static {
        deltas.add(CounterConstants.REQUESTPROCESS_BYTES_RECEIVED);
        deltas.add(CounterConstants.REQUESTPROCESS_BYTES_SENT);
        deltas.add(CounterConstants.REQUESTPROCESS_ERROR_COUNT);
        deltas.add(CounterConstants.REQUESTPROCESS_PROCESSING_TIME);
        deltas.add(CounterConstants.REQUESTPROCESS_REQUEST_COUNT);
        ConfObserver.add("TomcatJMXPerf", new Runnable() {
            public void run() {
                ObjTypeDetector.dirtyConfig = true;
            }
        });
    }

    private long getDelta(MeterKey key, Long newValue) {
        Long oldValue = lastValues.put(key, newValue);
        return oldValue == null ? 0 : newValue.longValue() - oldValue.longValue();
    }

    private MeterResource getMeter(MeterKey key) {
        MeterResource meter = meters.get(key);
        if (meter == null) {
            meter = new MeterResource();
            meters.put(key, meter);
        }
        return meter;
    }

    static class MeterKey {
        int mbeanHash;
        String counter;

        public MeterKey(int mbeanHash, String counter) {
            this.mbeanHash = mbeanHash;
            this.counter = counter;
        }

        public int hashCode() {
            return mbeanHash ^ counter.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof MeterKey) {
                MeterKey key = (MeterKey) obj;
                return (this.mbeanHash == key.mbeanHash) && (this.counter.equals(key.counter));
            }
            return false;
        }
    }

    private MBeanServer server;
    List<MBeanObj> beanList = new ArrayList<MBeanObj>();
    public long collectCnt = 0;

    @Counter
    public void process(CounterBasket pw) {
        if (conf.jmx_counter_enabled == false ||
                (CounterConstants.TOMCAT.equals(ObjTypeDetector.objType) == false
                        && CounterConstants.TOMCAT.equals(ObjTypeDetector.drivedType) == false)) {
            return;
        }

        getMBeanServer();
        if ((collectCnt <= 40 && collectCnt % 5 == 0) || ObjTypeDetector.dirtyConfig) {
            if (ObjTypeDetector.dirtyConfig) {
                AgentHeartBeat.clearSubObjects();
                ObjTypeDetector.dirtyConfig = false;
            }
            getMBeanList();
        }
        collectCnt++;
        for (MBeanObj beanObj : beanList) {
            if (errors.contains(beanObj.attrName)) continue;
            if (beanObj.valueType == ValueEnum.DECIMAL) {
                try {
                    MeterKey key = new MeterKey(beanObj.mbeanHash, beanObj.counter);
                    long v = CastUtil.clong(server.getAttribute(beanObj.mbean, beanObj.attrName));
                    if (deltas.contains(beanObj.counter)) {
                        v = getDelta(key, v);
                        MeterResource meter = getMeter(key);
                        meter.add(v);
                        v = (long) meter.getSum(60);
                        long sum = (long) meter.getSum(300) / 5;
                        pw.getPack(beanObj.objName, TimeTypeEnum.REALTIME).add(beanObj.counter, new DecimalValue(v));
                        pw.getPack(beanObj.objName, TimeTypeEnum.FIVE_MIN).add(beanObj.counter, new DecimalValue(sum));
                    } else {
                        MeterResource meter = getMeter(key);
                        meter.add(v);
                        double avg = meter.getAvg(300);
                        FloatValue avgValue = new FloatValue((float) avg);
                        pw.getPack(beanObj.objName, TimeTypeEnum.REALTIME).add(beanObj.counter, new DecimalValue(v));
                        pw.getPack(beanObj.objName, TimeTypeEnum.FIVE_MIN).add(beanObj.counter, avgValue);
                    }
                } catch (Exception e) {
                    errors.add(beanObj.attrName);
                    collectCnt = 0;
                    Logger.println("A902", e);
                }
            }
        }
        // long cpu2 = SysJMX.getCurrentThreadCPU();
    }

    private HashSet<String> errors = new HashSet<String>();

    private void getMBeanServer() {
        if (server == null) {
            server = ManagementFactory.getPlatformMBeanServer();
        }
    }

    Configure conf = Configure.getInstance();

    private void getMBeanList() {
        beanList.clear();
        Set<ObjectName> mbeans = server.queryNames(null, null);
        for (final ObjectName mbean : mbeans) {
            String type = mbean.getKeyProperty("type");
            String connectionpool = mbean.getKeyProperty("connectionpool");
            if (type == null) {
                continue;
            }
            if ("GlobalRequestProcessor".equals(type)) {
                String port = mbean.getKeyProperty("name");
                try {
                    String objName = conf.getObjName() + "/" + checkObjName(port);
                    String objType = getReqProcType();
                    AgentHeartBeat.addObject(objType, HashUtil.hash(objName), objName);
                    add(objName, mbean, objType, ValueEnum.DECIMAL, "bytesReceived",
                            CounterConstants.REQUESTPROCESS_BYTES_RECEIVED);
                    add(objName, mbean, objType, ValueEnum.DECIMAL, "bytesSent",
                            CounterConstants.REQUESTPROCESS_BYTES_SENT);
                    add(objName, mbean, objType, ValueEnum.DECIMAL, "errorCount",
                            CounterConstants.REQUESTPROCESS_ERROR_COUNT);
                    add(objName, mbean, objType, ValueEnum.DECIMAL, "processingTime",
                            CounterConstants.REQUESTPROCESS_PROCESSING_TIME);
                    add(objName, mbean, objType, ValueEnum.DECIMAL, "requestCount",
                            CounterConstants.REQUESTPROCESS_REQUEST_COUNT);
                } catch (Exception e) {
                }
            } else if ("DataSource".equals(type) && connectionpool == null) { // datasource
                try {
                    String modelerType = CastUtil.cString(server.getAttribute(mbean, "modelerType"));
                    if ("com.zaxxer.hikari.HikariDataSource".equals(modelerType)) {
                        continue;
                    }
                    String name = mbean.getKeyProperty("name");
                    if (StringUtil.isNotEmpty(name)) {
                        String context = mbean.getKeyProperty("context");
                        if (context != null && context.length() > 0) {
                            context = context.substring(1);
                        }
                        if (StringUtil.isNotEmpty(context)) {
                            name = context + "_" + name;
                        }
                        String objName = conf.getObjName() + "/" + checkObjName(name);
                        String objType = getDataSourceType();
                        AgentHeartBeat.addObject(objType, HashUtil.hash(objName), objName);
                        add(objName, mbean, objType, ValueEnum.DECIMAL, "numActive",
                                CounterConstants.DATASOURCE_CONN_ACTIVE);
                        add(objName, mbean, objType, ValueEnum.DECIMAL, "numIdle",
                                CounterConstants.DATASOURCE_CONN_IDLE);
                        add(objName, mbean, objType, ValueEnum.DECIMAL, "maxActive",
                                CounterConstants.DATASOURCE_CONN_MAX);
                        // for tomcat 5.5 +
                        // attribute name is changed from maxActive to maxTotal. (reported from zeroty : https://github.com/zeroty)
                        add(objName, mbean, objType, ValueEnum.DECIMAL, "maxTotal",
                                CounterConstants.DATASOURCE_CONN_MAX);
                    }
                } catch (Exception e) {
                }
            } else if ("com.zaxxer.hikari".equals(mbean.getDomain()) && type.startsWith("PoolConfig (")) {
                try {
                    final String poolName = CastUtil.cString(server.getAttribute(mbean, "PoolName"));
                    final String objName = conf.getObjName() + "/" + checkObjName(poolName);
                    final String objType = getDataSourceType();
                    AgentHeartBeat.addObject(objType, HashUtil.hash(objName), objName);
                    ObjectName hikariMBean = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
                    add(objName, hikariMBean, objType, ValueEnum.DECIMAL, "ActiveConnections",
                            CounterConstants.DATASOURCE_CONN_ACTIVE);
                    add(objName, hikariMBean, objType, ValueEnum.DECIMAL, "IdleConnections",
                            CounterConstants.DATASOURCE_CONN_IDLE);
                    add(objName, hikariMBean, objType, ValueEnum.DECIMAL, "TotalConnections",
                            CounterConstants.DATASOURCE_CONN_MAX);
                } catch (Exception e) {
                }
            }
        }
    }

    private String getReqProcType() {
        if (Configure.getInstance().obj_type_inherit_to_child_enabled) {
            return Configure.getInstance().obj_type + "_req";
        }
        return CounterConstants.REQUESTPROCESS;
    }

    private String getDataSourceType() {
        if (Configure.getInstance().obj_type_inherit_to_child_enabled) {
            return Configure.getInstance().obj_type + "_ds";
        }
        return CounterConstants.DATASOURCE;
    }

    private void add(String objName, ObjectName mbean, String type, byte decimal, String attrName, String counterName) {
        if (errors.contains(attrName))
            return;
        MBeanObj cObj = new MBeanObj(objName, mbean, type, ValueEnum.DECIMAL, attrName, counterName);
        beanList.add(cObj);
    }

    private static String checkObjName(String name) {
        StringBuffer sb = new StringBuffer();
        char[] charArray = name.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            switch (charArray[i]) {
                case '-':
                case '_':
                    sb.append(charArray[i]);
                    break;
                case '/':
                    sb.append('_');
                    break;
                default:
                    if (Character.isLetterOrDigit(charArray[i])) {
                        sb.append(charArray[i]);
                    }
            }
        }
        return sb.toString();
    }

    class MBeanObj {
        public int mbeanHash;
        public String objName;
        public ObjectName mbean;
        public String objType;
        public byte valueType;
        public String attrName;
        public String counter;

        public MBeanObj(String objName, ObjectName mbean, String objType, byte valueType, String attrName,
                        String counter) {
            this.objName = objName;
            this.mbean = mbean;
            this.mbeanHash = HashUtil.hash(mbean.toString());
            this.objType = objType;
            this.valueType = valueType;
            this.attrName = attrName;
            this.counter = counter;
        }

        @Override
        public String toString() {
            return "MBeanObj [objName=" + objName + ", mbean=" + mbean + ", objType=" + objType + ", valueType="
                    + valueType + ", attrName=" + attrName + ", counter=" + counter + "]";
        }
    }
}
