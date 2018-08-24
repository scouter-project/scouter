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

package scouter.server.management;

import java.util.ArrayList
import java.util.Enumeration
import java.util.Hashtable
import scouter.server.Logger
import scouter.server.LoginManager
import scouter.server.LoginUser
import scouter.server.ShutdownManager
import scouter.util.IShutdown
import scouter.util.Queue
import scouter.util.ThreadUtil
import scouter.util.LongKeyLinkedMap
import scouter.server.util.ThreadScala
import scouter.server.core.CoreRun

object RemoteControlManager {

    val commandTable = new LongKeyLinkedMap[Queue[RemoteControl]]();

    def add(toSession: Long, remoteControl: RemoteControl): Boolean = {
        var queue = commandTable.get(toSession);
        if (queue == null) {
            queue = new Queue[RemoteControl](5);
            commandTable.put(toSession, queue);
        }
        val control = queue.enqueue(remoteControl);
        if (control == null) {
            Logger.println("S148", "[INFO] RemoteControlManager queue exceeded!command:" + remoteControl.commnad);
            return false;
        }
        return true;
    }

    def getCommand(session: Long): RemoteControl = {
        val queue = commandTable.get(session);
        if (queue == null) {
            return null;
        }
        val control = queue.dequeue();
        if (control != null) {
            return control;
        }
        return null;
    }

    def getCommandContents(): String = {
        return commandTable.toString();
    }

    ThreadScala.startDaemon("scouter.server.management.RemoteControlManager", { CoreRun.running }, 5000) {
        val enu = commandTable.keys();
        while (enu.hasMoreElements()) {
            val session = enu.nextLong();
            val user = LoginManager.getUser(session);
            if (user == null) {
                commandTable.remove(session)
            }
        }
    }

}
