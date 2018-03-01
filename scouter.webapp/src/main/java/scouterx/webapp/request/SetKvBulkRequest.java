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
import scouter.lang.constants.ParamConstant;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.KeyValueData;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@Setter
@ToString
public class SetKvBulkRequest {
    private int serverId;

    @NotNull
    private List<KeyValueData> kvList;

    private long ttl = ParamConstant.TTL_PERMANENT;

    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        for (KeyValueData data : kvList) {
            map.put(data.getKey(), data.getValue().toString());
        }
        return map;
    }

    public Map<String, String> toMapPadKeyPrefix(String keyPrefix) {
        Map<String, String> map = new HashMap<>();
        for (KeyValueData data : kvList) {
            map.put(keyPrefix + data.getKey(), data.getValue().toString());
        }
        return map;
    }
}
