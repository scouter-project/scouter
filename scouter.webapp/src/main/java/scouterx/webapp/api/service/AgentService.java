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

package scouterx.webapp.api.service;

import scouterx.client.server.Server;
import scouterx.webapp.api.consumer.AgentConsumer;
import scouterx.webapp.api.model.SObject;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AgentService {
    private final AgentConsumer agentConsumer;

    public AgentService() {
        this.agentConsumer = new AgentConsumer();
    }

    /**
     * retrieve object(agent) list from collector server
     */
    public List<SObject> retrieveAgentList(final Server server) {
        return agentConsumer.retrieveAgentList(server);
    }
}
