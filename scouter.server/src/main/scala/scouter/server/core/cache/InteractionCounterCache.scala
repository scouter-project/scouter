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

package scouter.server.core.cache

import java.util

import scouter.lang.pack.InteractionPerfCounterPack
import scouter.server.util.{EnumerScala, ThreadScala}
import scouter.util.{CacheTable, ThreadUtil}

/**
  * Singleton object of the memory cache for counter data.
  */
object InteractionCounterCache {
    val dummyTable = new CacheTable[InteractionCounterCacheKey, InteractionPerfCounterPack]()
    val objCacheMap = new util.HashMap[Int, CacheTable[InteractionCounterCacheKey, InteractionPerfCounterPack]]()
    val keepTime = 10000

    ThreadScala.startDaemon("scouter.server.core.cache.CounterCache") {
        while (true) {
            ThreadUtil.sleep(5000)

            EnumerScala.foreach(objCacheMap.values().iterator(),
                (cache: CacheTable[InteractionCounterCacheKey, InteractionPerfCounterPack]) => {
                cache.clearExpiredItems()
            })
        }
    }

    def put(objHash: Int, key: InteractionCounterCacheKey, pack: InteractionPerfCounterPack) {
        var cache = objCacheMap.get(objHash)
        if (cache == null) {
            cache = new CacheTable[InteractionCounterCacheKey, InteractionPerfCounterPack]()
            objCacheMap.put(objHash, cache)
        }
        val packPrev = cache.get(key)
        cache.put(key, pack, keepTime)
    }

    def get(objHash: Int, key: InteractionCounterCacheKey): InteractionPerfCounterPack = {
        var cache = objCacheMap.get(objHash)
        if (cache == null) {
            return null
        }
        cache.get(key)
    }

    def getCacheTable(objHash: Int): CacheTable[InteractionCounterCacheKey, InteractionPerfCounterPack] = {
        val table = objCacheMap.get(objHash)
        if(table == null) {
            return dummyTable
        }
        return table
    }
}
