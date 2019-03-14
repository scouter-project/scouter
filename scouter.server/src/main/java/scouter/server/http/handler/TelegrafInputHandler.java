/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.http.handler;

import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.lang.value.NumberValue;
import scouter.server.Configure;
import scouter.server.CounterManager;
import scouter.server.Logger;
import scouter.server.http.HttpServer;
import scouter.server.http.model.CounterProtocol;
import scouter.server.http.model.InfluxSingleLine;
import scouter.server.netio.data.NetDataProcessor;
import scouter.util.CacheTable;
import scouter.util.IPUtil;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 22.
 */
public class TelegrafInputHandler extends Thread {
    private static TelegrafInputHandler instance = new TelegrafInputHandler();
    private static Configure configure = Configure.getInstance();
    private static CounterManager counterManager = CounterManager.getInstance();

    private RequestQueue<InfluxSingleLine> registerObjTypeQueue = new RequestQueue<InfluxSingleLine>(1024);
    private RequestQueue<AddCounterParam> addCounterQueue = new RequestQueue<AddCounterParam>(1024);
    private RequestQueue<Integer> changeNotifyQueue = new RequestQueue<Integer>(10);

    private CacheTable<String, Counter> prevAddedCounter = new CacheTable<String, Counter>().setMaxRow(10000).setDefaultKeepTime(5000);

    private static class AddCounterParam {
        ObjectType objectType;
        Counter counter;
        public AddCounterParam(ObjectType objectType, Counter counter) {
            this.objectType = objectType;
            this.counter = counter;
        }
    }

    private TelegrafInputHandler() {
        this.setDaemon(true);
        this.setName(ThreadUtil.getName(this));
        this.start();
    }

    public static TelegrafInputHandler getInstance() {
        return instance;
    }

    @Override
    public void run() {
        while (true) {
            try {
                int notifyCount = 0;
                while (true) {
                    InfluxSingleLine line = registerObjTypeQueue.get(100);
                    if (line == null) {
                        break;
                    }
                    registerNewObjType0(line);
                    ThreadUtil.sleep(100);
                }
                while (true) {
                    AddCounterParam addCounterParam = addCounterQueue.get(100);
                    if (addCounterParam == null) {
                        break;
                    }
                    addCounter0(addCounterParam);
                    ThreadUtil.sleep(100);
                }
                while (true) {
                    Integer anyNum = changeNotifyQueue.get(100);
                    if (anyNum == null) {
                        if (notifyCount > 0) {
                            notifyCount = 0;
                            RegisterHandler.notifyAllClients();
                        }
                        break;
                    }
                    notifyCount++;
                }
            } catch (Exception e) {
                Logger.println("TGI-003", 60, "TelegrafInputHandler Error:" + e.getMessage(), e);
            }
        }
    }

    public void handlerRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!configure.input_telegraf_enabled) {
            return;
        }

        long receivedTime = System.currentTimeMillis();
        Map<String, String> uniqueLineString = new HashMap<String, String>();
        int lineCount = 0;
        boolean earlyResponse = false;
        while (true) {
            if (lineCount++ > 1000) {
                earlyResponse = true;
                break;
            }
            String lineString = request.getReader().readLine();
            if (lineString == null) {
                break;
            }
            if (configure.input_telegraf_debug_enabled) {
                Logger.println("TG002", "[line protocol received] " + lineString);
            }
            uniqueLineString.put(InfluxSingleLine.toLineStringKey(lineString), lineString);
        }

        if (earlyResponse) {
            Logger.println("TG010", "[WARN] Too many line protocol in payload. fast return working. some line could be dropped!");
            return;
        }

        for (Map.Entry<String, String> lineStringEntry : uniqueLineString.entrySet()) {
            InfluxSingleLine line = InfluxSingleLine.of(lineStringEntry.getValue(), configure, receivedTime);
            if (configure.input_telegraf_debug_enabled) {
                Logger.println("TG003", "[line protocol] " + lineStringEntry.getValue() + " [line parsed] " + line);

            } else if (line != null && line.isDebug()) {
                Logger.println("TG004", "[line protocol] " + lineStringEntry.getValue() + " [line parsed] " + line);
            }

            if (line == null) {
                continue;
            }
            count(line, HttpServer.getRemoteAddr(request));
        }
    }

    protected void count(InfluxSingleLine line, String remoteAddress) throws IOException {
        ObjectType objectType = counterManager.getCounterEngine().getObjectType(line.getObjType());
        if (objectType == null) {
            registerNewObjType(line);
            return;
        }

        if (hasNewCounterThenRegister(objectType, line)) {
            return;
        }

        InetAddress address = InetAddress.getByAddress(IPUtil.toBytes(remoteAddress));
        NetDataProcessor.add(line.toObjectPack(remoteAddress, configure.telegraf_object_deadtime_ms), address);
        NetDataProcessor.add(line.toPerfCounterPack(), address);
    }

    private boolean hasNewCounterThenRegister(ObjectType objectType, InfluxSingleLine line) {
        Map<CounterProtocol, NumberValue> numberFields = line.getNumberFields();
        boolean hasAnyNewCounter = false;

        for (CounterProtocol counterProtocol : numberFields.keySet()) {
            boolean isNewCounter = counterProtocol.isNewOrChangedCounter(objectType, line);
            if (isNewCounter) {
                hasAnyNewCounter = true;
                for (Counter counter : counterProtocol.toCounters(line.getTags())) {
                    addCounter(objectType, counter);
                }
                continue;
            }
        }
        return hasAnyNewCounter;
    }

    private void registerNewObjType(InfluxSingleLine line) {
        registerObjTypeQueue.put(line);
    }

    private void addCounter(ObjectType objectType, Counter counter) {
        addCounterQueue.put(new AddCounterParam(objectType, counter));
    }

    private void registerNewObjType0(InfluxSingleLine line) {
        ObjectType objectTypeDoubleCheck = counterManager.getCounterEngine().getObjectType(line.getObjType());
        if (objectTypeDoubleCheck != null) {
            return;
        }

        try {
            String objTypeName = line.getObjType();

            Family family = new Family();
            family.setName(line.getFamily());

            ObjectType objectType = new ObjectType();
            objectType.setName(objTypeName);
            objectType.setDisplayName(objTypeName);
            objectType.setIcon(line.getObjTypeIcon());
            objectType.setFamily(family);

            Map<CounterProtocol, NumberValue> numberFields = line.getNumberFields();
            boolean firstCounter = true;
            for (CounterProtocol counterProtocol : numberFields.keySet()) {
                List<Counter> counters = counterProtocol.toCounters(line.getTags());
                for (Counter counter : counters) {
                    family.addCounter(counter);
                    if (firstCounter) {
                        family.setMaster(counter.getName());
                        firstCounter = false;
                    }
                }
            }

            boolean success0 = counterManager.safelyAddFamily(family);
            boolean success1 = counterManager.safelyAddObjectType(objectType);

            if (success0 && success1) {
                RegisterHandler.notifyAllClients();
            }

        } catch (Throwable th) {
            Logger.println("HT-001", 30, "Error on register telegraf type", th);
        }
    }

    private void addCounter0(AddCounterParam param) {
        ObjectType objectType = param.objectType;
        Counter counter = param.counter;

        Logger.println("[counter+]Trying to add new counter : " + objectType.getFamily().getName()
                + " - " + counter.getName());

        //TODO login check
        if (counter.semanticEquals(prevAddedCounter.get(counter.getName()))) {
            Logger.println("[counter+] ignored by equals");
            return;
        }

        Family family = objectType.getFamily();
        family.addCounter(counter);
        boolean success = counterManager.safelyAddFamily(family);
        if (success) {
            prevAddedCounter.put(counter.getName(), counter);
            changeNotifyQueue.put(1);
        }
    }
}
