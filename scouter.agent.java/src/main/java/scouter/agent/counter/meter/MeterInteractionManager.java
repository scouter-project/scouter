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

package scouter.agent.counter.meter;

import scouter.agent.Configure;
import scouter.util.LinkedMap;
import scouter.util.Pair;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

import static scouter.lang.counters.CounterConstants.INTR_API_INCOMING;
import static scouter.lang.counters.CounterConstants.INTR_API_OUTGOING;
import static scouter.lang.counters.CounterConstants.INTR_DB_CALL;
import static scouter.lang.counters.CounterConstants.INTR_REDIS_CALL;

public class MeterInteractionManager extends Thread {

    private static MeterInteractionManager instance;
    private static Configure conf = Configure.getInstance();

    private RequestQueue<Pair<String, Key>> queue = new RequestQueue<Pair<String, Key>>(1024);

    private static LinkedMap<Key, MeterInteraction> apiOutgoingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> apiIncomingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> dbCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> redisCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);

    private MeterInteractionManager() {
    }

    public final static synchronized MeterInteractionManager getInstance() {
        if (instance == null) {
            instance = new MeterInteractionManager();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            instance.start();
        }
        return instance;
    }

    public void run() {
        while (true) {
            Pair<String, Key> pair = queue.get();
            String type = pair.getLeft();
            Key key = pair.getRight();
            MeterInteraction meterInteraction = new MeterInteraction(type, type + "_" + key.fromHash + "_" + key.toHash);

            if (INTR_API_OUTGOING.equals(type)) {
                apiOutgoingMeterMap.put(key, meterInteraction);
            } else if (INTR_API_INCOMING.equals(type)) {
                apiIncomingMeterMap.put(key, meterInteraction);
            } else if (INTR_DB_CALL.equals(type)) {
                dbCallMeterMap.put(key, meterInteraction);
            } else if (INTR_REDIS_CALL.equals(type)) {
                redisCallMeterMap.put(key, meterInteraction);
            }
        }
    }

    /**
     * @return nullable
     */
    public MeterInteraction getApiOutgoingMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = apiOutgoingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_API_OUTGOING, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getApiIncomingMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = apiIncomingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_API_INCOMING, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getDbCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = dbCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_DB_CALL, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getRedisCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = redisCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_REDIS_CALL, key));
        }
        return meter;
    }

    public LinkedMap<Key, MeterInteraction> getApiOutgoingMeterMap() {
        return apiOutgoingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getApiIncomingMeterMap() {
        return apiIncomingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getDbCallMeterMap() {
        return dbCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getRedisCallMeterMap() {
        return redisCallMeterMap;
    }

    public static class Key {
        public int fromHash;
        public int toHash;

        public Key(int fromHash, int toHash) {
            this.fromHash = fromHash;
            this.toHash = toHash;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return this.fromHash == key.fromHash && this.toHash == key.toHash;
        }

        @Override
        public int hashCode() {
            return fromHash ^ toHash;
        }
    }
}