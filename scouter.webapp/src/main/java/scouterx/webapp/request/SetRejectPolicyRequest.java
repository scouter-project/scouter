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

package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.framework.client.server.ServerManager;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keys are the reject policy names without the 'control_reject_duplicated_ip_' prefix,
 * e.g. 'enabled', 'rate_window_ms', 'rate_max_count'.
 *
 * Keys omitted here fall back to each agent's local configuration, so only the
 * values you actually want to centralize need to be sent.
 *
 * @see scouterx.webapp.layer.controller.RejectControlController
 */
@Getter
@Setter
@ToString
public class SetRejectPolicyRequest {
    private int serverId;

    @NotNull
    private Map<String, String> policy = new LinkedHashMap<>();

    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }
}
