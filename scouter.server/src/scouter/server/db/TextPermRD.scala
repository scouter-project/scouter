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

package scouter.server.db;

import scouter.server.core.cache.TextCache;
import scouter.server.db.text.TextTable;
import scouter.util.HashUtil;

object TextPermRD {

    def getString(division: String, hash: Int): String = {
        val divHash = HashUtil.hash(division);
        val out = TextCache.get(divHash, hash);
        if (out != null)
            return out;

        val idx = TextPermWR.open();
        try {
            val b = idx.get(divHash, hash);
            if (b == null)
                return null;
            val text = new String(b, "UTF-8");
            TextCache.put(divHash, hash, text);
            return text;
        } catch {
            case e: Exception => e.printStackTrace();
        }
        return null;
    }

    def read(handler: (Array[Byte], Array[Byte]) => Unit) {
        try {
            val idx = TextPermWR.open();
            idx.read(handler);
        } catch {
            case e: Exception => e.printStackTrace();
        }
    }
}