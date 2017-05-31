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

import scouter.util.CacheTable
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala

object CommonCache {

    val cache = new CacheTable[Any, Any]().setMaxRow(1000);

    ThreadScala.startDaemon("scouter.server.core.cache.CommonCache") {
        while (true) {
            ThreadUtil.sleep(5000);
            cache.clearExpiredItems();
        }
    }

    def put(key: Any, value: Any, time: Long) {
        cache.put(key, value, time);
    }

    def get(key: Any): Any = {
        return cache.get(key);
    }

}
