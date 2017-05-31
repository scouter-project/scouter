/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */

package scouter.server.core.cache;

import scouter.util.IntSet;

object XLogCache {

    val cache = new XLogLoopCache[Array[Byte]](20480);

    def put(objHash: Int, time: Int, error: Boolean, record: Array[Byte]) {
        cache.put(objHash, time, error, record);
    }

    def get(last_loop: Long, last_index: Int, time: Int): CacheOut[Array[Byte]] = {
        return cache.get(last_loop, last_index, time);
    }

    def get(objHashSet: IntSet, last_loop: Long, last_index: Int, time: Int): CacheOut[Array[Byte]] = {
        return cache.get(objHashSet, last_loop, last_index, time);
    }

    def getWithinCount(last_loop: Long, last_index: Int, count: Int): CacheOut[Array[Byte]] = {
        return cache.getWithinCount(last_loop, last_index, count);
    }

    def getWithinCount(objHashSet: IntSet, last_loop: Long, last_index: Int, count: Int): CacheOut[Array[Byte]] = {
        return cache.getWithinCount(objHashSet, last_loop, last_index, count);
    }


}