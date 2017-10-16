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

package scouter.server.core;

import scouter.lang.pack.StackPack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.db.StackAnalyzerDB
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue

object StackAnalyzerCore {

    val queue = new RequestQueue[StackPack](CoreRun.MAX_QUE_SIZE);

    ThreadScala.startDaemon("scouter.server.core.StackCore") {
        val conf = Configure.getInstance();
        while (CoreRun.running) {
            val m = queue.get();
            StackAnalyzerDB.add(m)
        }
    }

    def add(p: StackPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S200", 10, "Stack queue exceeded!!");
        }
    }
}
  
