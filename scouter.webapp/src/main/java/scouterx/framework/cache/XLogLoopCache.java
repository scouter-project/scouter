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

package scouterx.framework.cache;

import scouter.lang.pack.XLogPack;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 9.
 */
public class XLogLoopCache {
    private int serverId = 0;
    private long loop = 0;
    private int index = 0;
    private XLogPack[] queue;

    public XLogLoopCache(int serverId, int capacity) {
        this.serverId = serverId;
        queue = new XLogPack[capacity];
    }

    public void add(XLogPack pack) {
        synchronized (queue) {
            queue[index] = pack;
            index += 1;
            if (index >= queue.length) {
                loop += 1;
                index = 0;
            }
        }
    }


}
