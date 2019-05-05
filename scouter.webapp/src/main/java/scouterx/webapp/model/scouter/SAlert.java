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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.lang.pack.AlertLevelEnum;
import scouter.lang.pack.AlertPack;
import scouterx.webapp.framework.client.model.AgentModelThread;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Getter
@Setter
@Builder
public class SAlert {
    /**
     * Alert time
     */
    public long time;
    /**
     * Object type
     */
    public String objType;
    /**
     * Object ID
     */
    public int objHash;
    public String objName;

    /**
     * Alert level. 0:Info, 1:Warn, 2:Error, 3:Fatal
     */
    public AlertLevelEnum level;
    /**
     * Alert title
     */
    public String title;
    /**
     * Alert message
     */
    public String message;
    /**
     * More info
     */
    public Map<String, Object> tagMap = new HashMap<>();

    public static SAlert of(AlertPack p) {
        Map<String, Object> tagMap = p.tags.toMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toJavaObject()));

        String objName = AgentModelThread.getInstance().getAgentObject(p.objHash) == null ? "UNKNOWN"
                : AgentModelThread.getInstance().getAgentObject(p.objHash).getObjName();

        return SAlert.builder()
                .time(p.time)
                .objType(p.objType)
                .objHash(p.objHash)
                .objName(objName)
                .level(AlertLevelEnum.of(p.level))
                .title(p.title)
                .message(p.message)
                .tagMap(tagMap)
                .build();
    }
}
