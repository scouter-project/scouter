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
import scouter.server.http.model.InfluxSingleLine;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
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
    public void start() {
        //TODO register job
    }

    public void handlerRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        //TODO counter를 가지고 일정 수 이상이 누적되면 204 응답하자. 204일때 telegraf 에서 어떻게 처리하는지를 확인해 봐야한다. 재전송 안하는지...
        long receivedTime = System.currentTimeMillis();
        Map<String, String> uniqueLineString = new HashMap<String, String>();
        int lineCount = 0;
        while (true) {
            if (lineCount++ > 500) {
                //TODO 응답 주고 끝내자.
            }

            String lineString = request.getReader().readLine();
            if (lineString == null) {
                break;
            }
            uniqueLineString.put(InfluxSingleLine.toLineStringKey(lineString), lineString);
        }

        for (Map.Entry<String, String> lineStringEntry : uniqueLineString.entrySet()) {
            InfluxSingleLine line = InfluxSingleLine.of(lineStringEntry.getValue(), configure, receivedTime);
            if (line == null) {
                continue;
            }
        }
    }

    public void count(InfluxSingleLine line) {
        ObjectType objectType = counterManager.getCounterEngine().getObjectType(line.getObjType());
        if (objectType == null) {
            registerNewObjType(line);
            return;
        }

        Map<Counter, NumberValue> numberFields = line.getNumberFields();
        boolean dirty = false;
        for (Counter counter : numberFields.keySet()) {
            Counter counterInternal = objectType.getCounter(counter.getName());
            if (counterInternal == null) {
                dirty = true;
                addCounter(objectType, counter);
                continue;
            }
        }
        if (dirty) {
            return;
        }
        //TODO counter action
    }

    private void registerNewObjType(InfluxSingleLine line) {
        registerObjTypeQueue.put(line);
    }

    private void addCounter(ObjectType objectType, Counter counter) {
        addCounterQueue.put(new AddCounterParam(objectType, counter));
    }

    private synchronized void registerNewObjType0(InfluxSingleLine line) {
        ObjectType objectTypeDoubleCheck = counterManager.getCounterEngine().getObjectType(line.getObjType());
        if (objectTypeDoubleCheck != null) {
            return;
        }

        try {
            String objTypeName = line.getObjType();
            ObjectType objectType = new ObjectType();
            objectType.setName(objTypeName);
            objectType.setDisplayName(objTypeName);
            objectType.setIcon("");

            Family family = new Family();
            objectType.setFamily(family);
            family.setName(objTypeName);

            Map<Counter, NumberValue> numberFields = line.getNumberFields();
            boolean firstCounter = true;
            for (Counter counter : numberFields.keySet()) {
                family.addCounter(counter);
                if (firstCounter) {
                    family.setMaster(counter.getName());
                }
                firstCounter = false;
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

    private synchronized void addCounter0(ObjectType objectType, Counter counter) {
        Counter counterDoubleCheck = objectType.getCounter(counter.getName());
        if (counterDoubleCheck != null) {
            return;
        }
        objectType.addCounter(counter);
        boolean success = counterManager.safelyAddObjectType(objectType);
        if (success) {
            RegisterHandler.notifyAllClients();
        }
    }
}
