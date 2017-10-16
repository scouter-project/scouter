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
 */

package scouter.server.tagcnt.core;

import scouter.server.Configure;
import scouter.server.ShutdownManager;
import scouter.util.Hexa32;
import scouter.util.IShutdown;

object CountEnv {

    val MAX_QUE_SIZE = 2000;
    val MAX_ACTIVEDB = 30;

    var running = true;

    ShutdownManager.add(new IShutdown() {
        override def shutdown() {
            running = false;
        }
    });

    def getDBPath(logDate: String): String = {
        val sb = new StringBuffer();
        sb.append(Configure.getInstance().db_dir);
        sb.append("/").append(logDate);
        sb.append("/").append("tagcnt");
        return sb.toString();
    }

    def getDBPath(logDate: String, objType: String): String = {
        val sb = new StringBuffer();
        sb.append(Configure.getInstance().db_dir);
        sb.append("/").append(logDate);
        sb.append("/").append("tagcnt");
        sb.append("/").append(objType);
        return sb.toString();
    }
}
