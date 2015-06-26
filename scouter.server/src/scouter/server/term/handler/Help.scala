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
 *
 */
package scouter.server.term.handler;

import scouter.server.term.ScouterHandler

object Help {
    def help(cmd: String): Unit = {
        System.out.println("\thelp = Help");
        System.out.println("\tquit = Quit");
        System.out.println("\tobjtypes = ObjType List");
        System.out.println("\tobjects = Object List");
        System.out.println("\tcounters [objType] = counter list for the objType");
        System.out.println("\trealtime [objType] | TPS [SUM/AVG] | LOOP 2 | FORMAT #,##0");
        System.out.println("\txlog [objType]");
        System.out.println("\ttagcnt group");
        System.out.println("\ttagcnt tag [group]");
        System.out.println("\ttagcnt top100 [objType] [group] [tag] ");
        System.out.println("\ttagcnt data [objType] [group] [tag] [inx]");

    }

    def quit(): Unit = {
        System.out.println("bye bye!!");
        System.exit(1);
    }

}
