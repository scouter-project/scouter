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

package scouterx.webapp.model.countermodel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterEngine;
import scouter.util.StringKeyLinkedMap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 1.
 */
@Getter
@NoArgsConstructor
public class CounterModelData {
    List<FamilyData> families;
    List<ObjectTypeData> objTypes;

    public static CounterModelData of(CounterEngine engine) {
        CounterModelData counterModelData = new CounterModelData();
        counterModelData.families = makeFamilies(engine);
        counterModelData.objTypes = makeObjTypes(engine);

        return counterModelData;
    }

    private static List<FamilyData> makeFamilies(CounterEngine engine) {
        List<FamilyData> families = new ArrayList<>();
        StringKeyLinkedMap<Family> familyMap = engine.getRawFamilyMap();
        Enumeration<Family> familyEnumeration = familyMap.values();

        while (familyEnumeration.hasMoreElements()) {
            Family family = familyEnumeration.nextElement();
            FamilyData familyData = new FamilyData(family.getName());

            for (Counter counter : family.listCounters()) {
                boolean isMasterCounter = false;
                if (counter.getName().equals(family.getMaster())) {
                    isMasterCounter = true;
                }

                CounterData counterData = CounterData.builder()
                        .name(counter.getName())
                        .displayName(counter.getDisplayName())
                        .unit(counter.getUnit())
                        .totalizable(counter.isTotal())
                        .icon(counter.getIcon())
                        .isMaster(isMasterCounter).build();

                familyData.addCounterData(counterData);
            }
            families.add(familyData);
        }

        return families;
    }

    private static List<ObjectTypeData> makeObjTypes(CounterEngine engine) {
        List<ObjectTypeData> objTypes = new ArrayList<>();
        StringKeyLinkedMap<ObjectType> objTypeMap = engine.getRawObjectTypeMap();
        Enumeration<ObjectType> objTypeEnumeration = objTypeMap.values();

        while (objTypeEnumeration.hasMoreElements()) {
            ObjectType objType = objTypeEnumeration.nextElement();
            ObjectTypeData objectTypeData = ObjectTypeData.builder()
                    .name(objType.getName())
                    .displayName(objType.getDisplayName())
                    .familyName(objType.getFamily().getName())
                    .isSubObject(objType.isSubObject())
                    .icon(objType.getIcon())
                    .build();

            objTypes.add(objectTypeData);
        }

        return objTypes;
    }
}
