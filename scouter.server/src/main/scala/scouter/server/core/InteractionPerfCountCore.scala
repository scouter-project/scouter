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
package scouter.server.core

import scouter.lang.pack.InteractionPerfCounterPack
import scouter.server.Logger
import scouter.server.core.cache.{InteractionCounterCache, InteractionCounterCacheKey}
import scouter.server.util.ThreadScala
import scouter.util.{HashUtil, RequestQueue}

/**
  * request queue of interaction perf counter data and also dispatcher of the queue
  */
object InteractionPerfCountCore {

    var queue = new RequestQueue[InteractionPerfCounterPack](CoreRun.MAX_QUE_SIZE)

    ThreadScala.startDaemon("scouter.server.core.InteractionPerfCountCore", {CoreRun.running}) {
        val pack = queue.get()
        val objHash = HashUtil.hash(pack.objName)
        InteractionCounterCache.put(objHash, new InteractionCounterCacheKey(pack.interactionType, pack.fromHash, pack.toHash), pack)
    }

    def add(p: InteractionPerfCounterPack) {
        val ok = queue.put(p)
        if (!ok) {
            Logger.println("S1109", 10, "queue exceeded!!");
        }
    }
}
