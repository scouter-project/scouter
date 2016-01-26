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

public class LeakableObject2 implements ILeakableObject {

    public LeakableContainer2 container;
    public int pidx;
    public LeakInfo2 info;

    public LeakableObject2(Error error, Object inner, ICloseManager closeManager, int serviceHash, long txid, boolean fullstack, int fullstackSkip) {
        LeakableContainer2.add(this);
        this.info = new LeakInfo2(error, inner, closeManager, serviceHash, txid, fullstack, fullstackSkip);
    }

    @Override
    public void close() {
        container.bucket[pidx] = null;
    }
}