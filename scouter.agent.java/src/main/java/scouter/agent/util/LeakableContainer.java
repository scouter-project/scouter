/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.agent.util;


public class LeakableContainer {
    private final static int MAX_BUCKET = 20;
    private int pos = 0;

    private static LeakableContainer container = new LeakableContainer();
    public LeakableObject[] bucket = new LeakableObject[MAX_BUCKET];

    protected void finalize() throws Throwable {
        for (int i = 0; i < MAX_BUCKET; i++) {
            if (bucket[i] != null) {
                AsyncRunner.getInstance().add(bucket[i].info);
            }
        }
    }

    public synchronized static void add(LeakableObject obj) {
        container.bucket[container.pos] = obj;
        obj.container = container;
        obj.pidx = container.pos;
        container.pos++;
        if (container.pos >= MAX_BUCKET) {
            container = new LeakableContainer();
        }
    }
}
