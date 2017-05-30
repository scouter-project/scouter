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
package scouter.server.tagcnt.first;

import java.io.IOException
import scouter.util.IClose

class WorkDB(file: String) extends IClose {

    var lastActive = 0L

    var entry: TagValueEntry = null
    var table: IndexFile = null

    var logDate = ""
    var objType = ""

    def open() {
        this.table = IndexFile.open(file + "/first");
        this.entry = TagValueEntry.open(this.table);
    }

    def close() {
        this.entry.close();
        this.table.close();
    }
}
