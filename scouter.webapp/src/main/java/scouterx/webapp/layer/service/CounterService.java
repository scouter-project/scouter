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
import scouterx.webapp.layer.consumer.CounterConsumer;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.request.CounterAvgRequestByObjHashes;
import scouterx.webapp.request.CounterAvgRequestByType;
import scouterx.webapp.request.CounterRequestByObjHashes;
import scouterx.webapp.request.CounterRequestByType;
import scouterx.webapp.view.AvgCounterView;
import scouterx.webapp.view.CounterView;

import java.util.List;
import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class CounterService {
    private final CounterConsumer counterConsumer;

    public CounterService() {
        this.counterConsumer = new CounterConsumer();
    }

    public List<SCounter> retrieveRealTimeCountersByObjType(String objType, Set<String> counterNames, final Server server) {
        return counterConsumer.retrieveRealTimeCountersByObjType(objType, counterNames, server);
    }

    public List<SCounter> retrieveRealTimeCountersByObjHashes(Set<Integer> objHashSet, Set<String> counterNames, final Server server) {
        return counterConsumer.retrieveRealTimeCountersByObjHashes(objHashSet, counterNames, server);
    }

    public List<CounterView> retrieveCounterByObjType(CounterRequestByType request) {
        return counterConsumer.retrieveCounterByObjType(request);
    }

    public List<CounterView> retrieveCounterByObjHashes(CounterRequestByObjHashes request) {
        return counterConsumer.retrieveCounterByObjHashes(request);
    }

    public List<AvgCounterView> retrieveAvgCounterByObjType(CounterAvgRequestByType request) {
        return counterConsumer.retrieveAvgCounterByObjType(request);
    }

    public List<AvgCounterView> retrieveAvgCounterByObjHashes(CounterAvgRequestByObjHashes request) {
        return counterConsumer.retrieveAvgCounterByObjHashes(request);
    }
}
