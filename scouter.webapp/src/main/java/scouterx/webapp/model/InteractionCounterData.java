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

package scouterx.webapp.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.lang.pack.InteractionPerfCounterPack;
import scouterx.webapp.framework.client.model.TextLoader;
import scouterx.webapp.framework.client.model.TextProxy;
import scouterx.webapp.framework.client.model.TextTypeEnum;
import scouterx.webapp.model.scouter.CounterValue;
import scouterx.webapp.model.scouter.SCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Getter
@Setter
@Builder
public class InteractionCounterData {

    public String objName;
    public String interactionType;

    public int fromObjHash;
    public String fromObjName;
    public List<CounterValue> fromObjCounters = new ArrayList<>();
    public int toObjHash;
    public String toObjName;
    public List<CounterValue> toObjCounters = new ArrayList<>();

    public int period;
    public int count;
    public int errorCount;
    public long totalElapsed;

    public static InteractionCounterData of(InteractionPerfCounterPack p, int serverId) {
        preLoadDictionary(p, serverId);

        return InteractionCounterData.builder()
                .interactionType(p.interactionType)
                .objName(p.objName)
                .fromObjHash(p.fromHash)
                .fromObjName(TextProxy.object.getCachedTextIfNullDefault(p.fromHash))
                .toObjHash(p.toHash)
                .toObjName(TextProxy.object.getCachedTextIfNullDefault(p.toHash))
                .period(p.period)
                .count(p.count)
                .errorCount(p.errorCount)
                .totalElapsed(p.totalElapsed)
                .fromObjCounters(new ArrayList<>())
                .toObjCounters(new ArrayList<>())
                .build();
    }

    private static void preLoadDictionary(InteractionPerfCounterPack pack, int serverId) {
        TextLoader loader = new TextLoader(serverId);
        loader.addTextHash(TextTypeEnum.OBJECT, pack.fromHash);
        loader.addTextHash(TextTypeEnum.OBJECT, pack.toHash);

        loader.loadAll();
    }

    public void addFromObjCounter(SCounter counter) {
        if (counter != null) {
            fromObjCounters.add(CounterValue.of(counter));
        }
    }

    public void addToObjCounter(SCounter counter) {
        if (counter != null) {
            toObjCounters.add(CounterValue.of(counter));
        }
    }
}
