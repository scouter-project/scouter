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

import scouter.lang.TextTypes;
import scouter.util.HashUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.IntKeyMap;

object TextCache {

    val cache = new IntKeyMap[IntKeyLinkedMap[String]]();

    def put(div: Int, hash: Int, text: String) {
        val m = getMap(div);
        if (m.containsKey(hash))
            return ;
        m.put(hash, text);
    }

    def get(div: String, hash: Int): String = {
        return get(HashUtil.hash(div), hash);
    }
    
    def get(div: Int, hash: Int): String = {
        val m = getMap(div);
        return m.get(hash);
    }

    private def getMap(div: Int): IntKeyLinkedMap[String] = {
        var m = cache.get(div);
        if (m == null) {
            if (TextTypes.SERVICE.equals(div)) {
                m = new IntKeyLinkedMap().setMax(10000);
            } else {
                m = new IntKeyLinkedMap().setMax(1000);
            }
            cache.put(div, m);
        }
        return m;
    }
}