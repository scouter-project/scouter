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

import scouter.lang.pack.StatusPack;
import scouter.util.CacheTable;

object StatusCache {

    val cache = new CacheTable[StatusKey, StatusPack]();

    def put(pack: StatusPack) {
        cache.put(new StatusKey(pack.objHash, pack.key), pack, 15000);
    }

    def get(objHash: Int, key: String): StatusPack = {
        return cache.get(new StatusKey(objHash, key));
    }

    class StatusKey(_objHash: Int, _key: String) {
        val objHash = _objHash;
        val key = _key;

        override def hashCode(): Int = {
            if (key == null) {
                return objHash;
            }
            return objHash ^ key.hashCode();
        }

        override def equals(obj: Any): Boolean = {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            val other = obj.asInstanceOf[StatusKey];
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (objHash != other.objHash)
                return false;
            return true;
        }
    }

    def clearDirty() {
        cache.clearExpiredItems();
    }

}