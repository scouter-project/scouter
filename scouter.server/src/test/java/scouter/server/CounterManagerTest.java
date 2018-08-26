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

package scouter.server;

import org.junit.Assert;
import org.junit.Test;
import scouter.lang.Family;
import scouter.lang.ObjectType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 7. 21.
 */
public class CounterManagerTest {

    @Test
    public void getCounterManage_test() {
        CounterManager counterManager = CounterManager.getInstance();
        Assert.assertNotNull(counterManager);
    }

    @Test
    public void safelyAddObjectType_test() {
        CounterManager counterManager = CounterManager.getInstance();
        ObjectType objectType = new ObjectType();
        objectType.setName("test-counter-1");
        objectType.setDisplayName("test-counter-1");
        objectType.setFamily(counterManager.getCounterEngine().getFamily("host"));
        objectType.setIcon("");

        counterManager.safelyAddObjectType(objectType);
        ObjectType generated = counterManager.getCounterEngine().getObjectType(objectType.getName());

        Assert.assertEquals(objectType, generated);
    }

    @Test
    public void safelyAddFamily_test() {
        CounterManager counterManager = CounterManager.getInstance();
        Family hostFamily = counterManager.getCounterEngine().getFamily("host");
        String newFamilyName = "host-custom-generated";
        hostFamily.setName(newFamilyName);
        counterManager.safelyAddFamily(hostFamily);

        Family generated = counterManager.getCounterEngine().getFamily(newFamilyName);
        Assert.assertEquals(hostFamily, generated);
    }
}