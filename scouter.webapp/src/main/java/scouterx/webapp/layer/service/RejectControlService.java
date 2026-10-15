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

import scouter.util.StringUtil;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.RejectControlConsumer;
import scouterx.webapp.model.RejectControlStatusData;

import java.util.Map;

/**
 * Central reject(block) control.
 *
 * A block has no TTL and is released only through {@link #removeBlock}, so this
 * layer refuses obviously malformed input rather than letting it reach the store -
 * a bogus entry there would sit forever until somebody notices.
 */
public class RejectControlService {

    private final RejectControlConsumer consumer;

    public RejectControlService() {
        this.consumer = new RejectControlConsumer();
    }

    public RejectControlStatusData getStatus(Server server) {
        return consumer.getStatus(server);
    }

    public boolean addBlock(String ip, String url, String reason, String by, Server server) {
        if (StringUtil.isEmpty(ip)) {
            throw new IllegalArgumentException("'ip' is required to add a block.");
        }
        return consumer.addBlock(ip, url, reason, by, server);
    }

    public boolean removeBlock(String ip, String url, Server server) {
        if (StringUtil.isEmpty(ip)) {
            throw new IllegalArgumentException("'ip' is required to remove a block.");
        }
        return consumer.removeBlock(ip, url, server);
    }

    /**
     * @param target 'detection' clears only the candidate list, anything else clears the blocks.
     */
    public int clear(String target, Server server) {
        return consumer.clear(target, server);
    }

    public Map<String, String> getPolicy(Server server) {
        return consumer.getPolicy(server);
    }

    public boolean setPolicy(Map<String, String> policy, Server server) {
        if (policy == null || policy.isEmpty()) {
            throw new IllegalArgumentException("'policy' is empty. use an explicit empty map only if you intend to fall back to each agent's local config.");
        }
        return consumer.setPolicy(policy, server);
    }
}
