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
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouterx.webapp.framework.client.model.AgentModelThread;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@ToString
@AllArgsConstructor
public class SActiveServiceStepCount {
    private int objHash;
    private String objName;
    private int step1Count;
    private int step2Count;
    private int step3Count;

    public static SActiveServiceStepCount of(MapPack mapPack) {
        String objName = AgentModelThread.getInstance().getAgentObject(mapPack.getInt(ParamConstant.OBJ_HASH)) == null
                ? "UNKNOWN" : AgentModelThread.getInstance().getAgentObject(mapPack.getInt(ParamConstant.OBJ_HASH)).getObjName();

        return new SActiveServiceStepCount(
                mapPack.getInt(ParamConstant.OBJ_HASH),
                objName,
                mapPack.getInt(ParamConstant.ACTIVE_SERVICE_STEP1),
                mapPack.getInt(ParamConstant.ACTIVE_SERVICE_STEP2),
                mapPack.getInt(ParamConstant.ACTIVE_SERVICE_STEP3));
    }
}
