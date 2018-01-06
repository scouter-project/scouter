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

import lombok.Getter;
import lombok.ToString;
import scouter.lang.counters.CounterEngine;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.model.AgentObject;
import scouterx.webapp.framework.client.server.ServerManager;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@ToString
public class SCounter {
    private int objHash;
    private String objName;
    private String name;
    private String displayName;
    private String unit;
    private Object value;

    public SCounter(int objHash, String name, Object value) {
        this.objHash = objHash;
        this.name = name;
        this.value = value;

        AgentObject agentObject = AgentModelThread.getInstance().getAgentObject(objHash);
        CounterEngine counterEngine = ServerManager.getInstance().getServerIfNullDefault(agentObject.getServerId()).getCounterEngine();

        this.displayName = counterEngine.getCounterDisplayName(agentObject.getObjType(), name);
        this.unit = counterEngine.getCounterUnit(agentObject.getObjType(), name);
        this.objName = agentObject.getObjName();
    }
}
