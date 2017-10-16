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
package scouter.server.core

import scouter.lang.TextTypes
import scouter.lang.pack.TextPack
import scouter.server.Logger
import scouter.server.core.cache.TextCache
import scouter.server.db.TextWR
import scouter.server.plugin.PlugInManager
import scouter.server.util.ThreadScala
import scouter.util.{DateUtil, RequestQueue}

/**
  * request queue of text data and dispatcher of the queue.
  */
object TextCore {
    val queue = new RequestQueue[TextPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("scouter.server.core.TextCore", {CoreRun.running}) {
        val pack = queue.get();
        ServerStat.put("text.core.queue", queue.size());
        val yyyymmdd = DateUtil.yyyymmdd();
        if (TextTypes.SQL.equals(pack.xtype)) {
            SqlTables.add(yyyymmdd, pack.hash, pack.text);
        }
        TextWR.add(yyyymmdd, pack.xtype, pack.hash, pack.text);
        PlugInManager.text(pack);
    }

    def add(p: TextPack) {
        TextCache.put(p.xtype, p.hash, p.text);
        val ok = queue.put(p);
        if (!ok) {
            Logger.println("S115", 10, "queue exceeded!!");
        }
    }
}
