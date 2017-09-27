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

import scouter.lang.TextTypes
import scouter.util.HashUtil
import scouter.util.IntKeyLinkedMap
import scouter.util.IntKeyMap;
import scouter.util.StringKeyLinkedMap

object TextCache {

    val cache = new StringKeyLinkedMap[IntKeyLinkedMap[String]]();

    def put(div: String, hash: Int, text: String) {
        val map = getMap(div);
        if (map.containsKey(hash))
            return ;
        map.put(hash, text);
    }

    
    def get(div: String, hash: Int): String = {
        val map = getMap(div);
        return map.get(hash);
    }

    private def getMap(div: String): IntKeyLinkedMap[String] = {
        var map = cache.get(div);
        if (map == null) {
            if (TextTypes.SERVICE.equals(div) || TextTypes.APICALL.equals(div)
                    || TextTypes.SQL.equals(div) || TextTypes.USER_AGENT.equals(div)) {
                map = new IntKeyLinkedMap().setMax(20000);
            } else {
                map = new IntKeyLinkedMap().setMax(2000);
            }
            cache.put(div, map);
        }
        return map;
    }
}
