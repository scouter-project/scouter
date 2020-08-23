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
import static scouter.lang.counters.CounterConstants.INTR_ELASTICSEARCH_CALL;
import static scouter.lang.counters.CounterConstants.INTR_KAFKA_CALL;
import static scouter.lang.counters.CounterConstants.INTR_MONGODB_CALL;
import static scouter.lang.counters.CounterConstants.INTR_RABBITMQ_CALL;
import static scouter.lang.counters.CounterConstants.INTR_NORMAL_INCOMING;
import static scouter.lang.counters.CounterConstants.INTR_NORMAL_OUTGOING;
import static scouter.lang.counters.CounterConstants.INTR_REDIS_CALL;

public class MeterInteractionManager extends Thread {

    private static MeterInteractionManager instance;
    private static Configure conf = Configure.getInstance();

    private RequestQueue<Pair<String, Key>> queue = new RequestQueue<Pair<String, Key>>(1024);

    private static LinkedMap<Key, MeterInteraction> apiOutgoingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> normalOutgoingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> apiIncomingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> normalIncomingMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(100);
    private static LinkedMap<Key, MeterInteraction> dbCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> redisCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> kafkaCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> rabbitmqCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> elasticSearchCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);
    private static LinkedMap<Key, MeterInteraction> mongoDbCallMeterMap = new LinkedMap<Key, MeterInteraction>().setMax(1000);

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

            } else if (INTR_NORMAL_OUTGOING.equals(type)) {
                normalOutgoingMeterMap.put(key, meterInteraction);

            } else if (INTR_API_INCOMING.equals(type)) {
                apiIncomingMeterMap.put(key, meterInteraction);

            } else if (INTR_NORMAL_INCOMING.equals(type)) {
                normalIncomingMeterMap.put(key, meterInteraction);

            } else if (INTR_DB_CALL.equals(type)) {
                dbCallMeterMap.put(key, meterInteraction);

            } else if (INTR_REDIS_CALL.equals(type)) {
                redisCallMeterMap.put(key, meterInteraction);

            } else if (INTR_KAFKA_CALL.equals(type)) {
                kafkaCallMeterMap.put(key, meterInteraction);

            } else if (INTR_RABBITMQ_CALL.equals(type)) {
                rabbitmqCallMeterMap.put(key, meterInteraction);

            } else if (INTR_ELASTICSEARCH_CALL.equals(type)) {
                elasticSearchCallMeterMap.put(key, meterInteraction);

            } else if (INTR_MONGODB_CALL.equals(type)) {
                mongoDbCallMeterMap.put(key, meterInteraction);
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
    public MeterInteraction getNormalOutgoingMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = normalOutgoingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_NORMAL_OUTGOING, key));
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
    public MeterInteraction getNormalIncomingMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = normalIncomingMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_NORMAL_INCOMING, key));
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

    /**
     * @return nullable
     */
    public MeterInteraction getKafkaCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = kafkaCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_KAFKA_CALL, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getRabbitmqCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = rabbitmqCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_RABBITMQ_CALL, key));
        }
        return meter;
    }

    /**
     * @return nullable
     */
    public MeterInteraction getElasticSearchCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = elasticSearchCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_ELASTICSEARCH_CALL, key));
        }
        return meter;
    }

    public MeterInteraction getMongoDbCallMeter(int fromHash, int toHash) {
        Key key = new Key(fromHash, toHash);
        MeterInteraction meter = mongoDbCallMeterMap.get(key);
        if (meter == null) {
            queue.put(new Pair<String, Key>(INTR_MONGODB_CALL, key));
        }
        return meter;
    }

    public LinkedMap<Key, MeterInteraction> getApiOutgoingMeterMap() {
        return apiOutgoingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getNormalOutgoingMeterMap() {
        return normalOutgoingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getApiIncomingMeterMap() {
        return apiIncomingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getNormalIncomingMeterMap() {
        return normalIncomingMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getDbCallMeterMap() {
        return dbCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getRedisCallMeterMap() {
        return redisCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getKafkaCallMeterMap() {
        return kafkaCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getRabbitmqCallMeterMap() {
        return rabbitmqCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getElasticSearchCallMeterMap() {
        return elasticSearchCallMeterMap;
    }

    public LinkedMap<Key, MeterInteraction> getMongoDbCallMeterMap() {
        return mongoDbCallMeterMap;
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