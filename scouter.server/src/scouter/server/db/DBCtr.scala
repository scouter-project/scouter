/*
 *  Copyright 2015 LG CNS.
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
 */

package scouter.server.db;

import scouter.server.Configure;
import scouter.server.ShutdownManager;
import scouter.util.IShutdown;

object DBCtr {
    val MAX_QUE_SIZE = 10000;
    val MAX_DIV = 20;
    val LARGE_MAX_QUE_SIZE = 100000;

    def getRootPath() = Configure.getInstance().scouter_db

    var running = true;
    ShutdownManager.add(new IShutdown() {
        override def shutdown() {
            running = false;
        }
    })
}