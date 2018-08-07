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

import org.apache.commons.lang.builder.EqualsBuilder;
import scouter.agent.Configure;
import scouter.util.Pair;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static scouter.lang.counters.CounterConstants.INTR_API_INCOMING;
import static scouter.lang.counters.CounterConstants.INTR_API_OUTGOING;
import static scouter.lang.counters.CounterConstants.INTR_DB_CALL;
import static scouter.lang.counters.CounterConstants.INTR_REDIS_CALL;

public class MeterInteractionManager extends Thread {

    private static MeterInteractionManager instance;
    private static Configure conf = Configure.getInstance();

    private RequestQueue<Pair<String, Key>> queue = new RequestQueue<Pair<String, Key>>(1024);

    private static Map<Key, MeterInteraction> apiOutgoingMeterMap = new ConcurrentHashMap<Key, MeterInteraction>();
    private static Map<Key, MeterInteraction> apiIncomingMeterMap = new ConcurrentHashMap<Key, MeterInteraction>();
    private static Map<Key, MeterInteraction> dbCallMeterMap = new ConcurrentHashMap<Key, MeterInteraction>();
    private static Map<Key, MeterInteraction> redisCallMeterMap = new ConcurrentHashMap<Key, MeterInteraction>();

    //	private static MeterInteraction redisCallMeter = new MeterInteraction(INTR_REDIS_CALL);
    //	private static MeterInteraction dbCallMeter = new MeterInteraction(INTR_DB_CALL);
    //	private static MeterInteraction apiIncomingMeter = new MeterInteraction(INTR_API_INCOMING);
    //	private static MeterInteraction apiOutgoingMeter = new MeterInteraction(INTR_API_OUTGOING);

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
            //TODO create meter from key - At!! type??
        }
    }

    /**
     * @return nullable
     */
    public MeterInteraction getApiOutgoingMeter(String fromName, String toName) {
        Key key = new Key(fromName, toName);
        MeterInteraction meter = apiOutgoingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_API_OUTGOING, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getApiIncomingMeter(String fromName, String toName) {
        Key key = new Key(fromName, toName);
        MeterInteraction meter = apiIncomingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_API_INCOMING, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getDbCallMeter(String fromName, String toName) {
        Key key = new Key(fromName, toName);
        MeterInteraction meter = dbCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_DB_CALL, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getRedisCallMeter(String fromName, String toName) {
        Key key = new Key(fromName, toName);
        MeterInteraction meter = redisCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_REDIS_CALL, key));
        }
        return meter;
    }

    public Map<Key, MeterInteraction> getApiOutgoingMeterMap() {
        return apiOutgoingMeterMap;
    }

    public Map<Key, MeterInteraction> getApiIncomingMeterMap() {
        return apiIncomingMeterMap;
    }

    public Map<Key, MeterInteraction> getDbCallMeterMap() {
        return dbCallMeterMap;
    }

    public Map<Key, MeterInteraction> getRedisCallMeterMap() {
        return redisCallMeterMap;
    }

    public static class Key {
        private String fromName;
        private String toName;

        public Key(String fromName, String toName) {
            this.fromName = fromName;
            this.toName = toName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return new EqualsBuilder()
                    .append(fromName, key.fromName)
                    .append(toName, key.toName)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return fromName.hashCode() ^ toName.hashCode();
        }
    }
}