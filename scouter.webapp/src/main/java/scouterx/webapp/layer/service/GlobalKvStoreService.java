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

package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.GlobalKvStoreConsumer;
import scouterx.webapp.model.KeyValueData;

import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class GlobalKvStoreService {
    private final GlobalKvStoreConsumer kvStoreConsumer;

    public GlobalKvStoreService() {
        this.kvStoreConsumer = new GlobalKvStoreConsumer();
    }

    public String get(String key, Server server) {
        return kvStoreConsumer.get(key, server);
    }

    public boolean set(String key, String value, long ttl, Server server) {
        boolean result = kvStoreConsumer.set(key, value, ttl, server);
        if (!result) {
            throw new RuntimeException("Error on setting value to kvstore!");
        }
        return result;
    }

    public boolean setTTL(String key, long ttl, Server server) {
        boolean result = kvStoreConsumer.setTTL(key, ttl, server);
        if (!result) {
            throw new RuntimeException("Error on setting value to kvstore!");
        }
        return true;
    }

    public List<KeyValueData> getBulk(List<String> paramList, final Server server) {
        return kvStoreConsumer.getBulk(paramList, server);
    }

    public List<KeyValueData> setBulk(Map<String, String> paramMap, long ttl, final Server server) {
        return kvStoreConsumer.setBulk(paramMap, ttl, server);
    }
}
