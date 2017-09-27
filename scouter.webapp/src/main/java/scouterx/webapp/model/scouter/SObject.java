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

package scouterx.webapp.model.scouter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@ToString
@AllArgsConstructor
public class SObject {
    private String objType;
    private String objFamily;
    private int objHash;
    private String objName;
    private String address;
    private String version;
    private boolean alive = true;
    public long lastWakeUpTime;
    public HashMap<String, Object> tags = new HashMap<>();

    private SObject(ObjectPack p, Server server) {
        this.objType = p.objType;
        this.objFamily = server.getCounterEngine().getFamilyNameFromObjType(p.objType);
        this.objHash = p.objHash;
        this.objName = p.objName;
        this.address = p.address;
        this.version = p.version;
        this.alive = p.alive;
        this.lastWakeUpTime = p.wakeup;
        for (Map.Entry<String, Value> e : p.tags.toMap().entrySet()) {
            tags.put(e.getKey(), e.getValue().toJavaObject());
        }
    }

    public static SObject of(ObjectPack p, Server server) {
        return new SObject(p, ServerManager.getInstance().getServerIfNullDefault(server));
    }
}
