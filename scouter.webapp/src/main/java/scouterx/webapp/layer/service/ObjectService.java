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
import scouterx.webapp.layer.consumer.ObjectConsumer;
import scouterx.webapp.model.HeapHistogramData;
import scouterx.webapp.model.SocketObjectData;
import scouterx.webapp.model.ThreadObjectData;
import scouterx.webapp.model.VariableData;
import scouterx.webapp.model.scouter.SObject;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 26.
 */
public class ObjectService {
    private final ObjectConsumer agentConsumer;

    public ObjectService() {
        this.agentConsumer = new ObjectConsumer();
    }

    /**
     * retrieve object(agent) list from collector server
     */
    public List<SObject> retrieveObjectList(final Server server) {
        return agentConsumer.retrieveObjectList(server);
    }

    /**
     * retrieve object(agent) thread list from collector server
     *
     */
    public List<ThreadObjectData> retrieveThreadList(int objHash, Server server) {
        return agentConsumer.retrieveThreadList(objHash, server);
    }

    /**
     * retrieve object(agent) thread dump from collector server
     *
     */
    public String retrieveThreadDump(int objHash, Server server) {
        return agentConsumer.retrieveThreadDump(objHash, server);
    }

    /**
     * retrieve object(agent) Heap histogram from collector server
     *
     */
    public List<HeapHistogramData> retrieveHeapHistogram(int objHash, Server server) {
        return agentConsumer.retrieveHeapHistogram(objHash, server);
    }

    /**
     * retrieve object(agent) environment info from collector server
     *
     */
    public List<VariableData> retrieveEnv(int objHash, Server server) {
        return agentConsumer.retrieveEnv(objHash, server);
    }

    /**
     * retrieve object(agent) environment info from collector server
     *
     */
    public List<SocketObjectData> retrieveSocket(int objHash, int serverId) {
        return agentConsumer.retrieveSocket(objHash, serverId);
    }
}
