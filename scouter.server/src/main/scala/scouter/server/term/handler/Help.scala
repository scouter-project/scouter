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
package scouter.server.term.handler;

import scouter.server.term.ScouterHandler
import scouter.server.term.TermMain
import java.util.ArrayList

object Help {
    def help(cmd: String): Unit = {
        System.out.println("\thelp = Help");
        System.out.println("\tquit = Quit");
        System.out.println("\tobjtype = ObjType List");
        System.out.println("\tobject = Object List");
        System.out.println("\tcounter [objType] = counter list for the objType");
        System.out.println("\tvisitor [objType] = today visit users for the objType");
        System.out.println("\tdashboard [objType]  = javaee's dashboard");
        System.out.println("\tdashboard [objType]  [Counter] = host's dashdoard");
        System.out.println("\trealtime [objType]  TPS [SUM/AVG] = realtime counter view");
        System.out.println("\txlog [objType]") ;
        System.out.println("\txlist [objType] [Time=10:12:23.123] [Count=20000] [minElapsed=0]");
        System.out.println("\ttagcnt group");
        System.out.println("\ttagcnt tag [group]");
        System.out.println("\ttagcnt top100 [objType] [group] [tag] ");
        System.out.println("\ttagcnt data [objType] [group] [tag] [inx]");
        System.out.println("\tdebug queue");

    }

    def quit(): Unit = {
        System.out.println("bye bye!!");
        // System.exit(1);
        TermMain.exit();
    }
    def words(): ArrayList[String] = {
        val w = new ArrayList[String]();
        w.add("help")
        w.add("quit")
        w.add("objtype")
        w.add("object")
        w.add("counter")
        w.add("realtime")
        w.add("tagcnt")
        w.add("dashboard")

        return w;
    }
}
