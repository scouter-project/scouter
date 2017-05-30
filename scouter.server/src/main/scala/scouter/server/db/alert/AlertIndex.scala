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

package scouter.server.db.alert

;

import java.util.Hashtable

import scouter.io.DataOutputX
import scouter.server.db.io.IndexTimeFile
import scouter.util.{FileUtil, IClose};

object AlertIndex {
    val table = new Hashtable[String, AlertIndex]();

    def open(file: String): AlertIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.reference += 1;
                return index;
            } else {
                index = new AlertIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }

}

class AlertIndex(file: String) extends IClose {

    val POSTFIX_TIME = "_tim";

    var reference = 0;
    var timeIndex: IndexTimeFile = null

    def add(time: Long, dataPos: Long) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file + POSTFIX_TIME);
        }
        this.timeIndex.put(time, DataOutputX.toBytes5(dataPos));
    }

    def close() {
        AlertIndex.table.synchronized {
            if (this.reference == 0) {
                AlertIndex.table.remove(this.file);
                FileUtil.close(this.timeIndex);
            } else {
                this.reference -= 1;
            }
        }
    }

    def read(fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file + POSTFIX_TIME);
        }
        this.timeIndex.read(fromTime, toTime, handler, reader);
    }

    def readFromEnd(fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file + POSTFIX_TIME);
        }
        this.timeIndex.readFromEnd(fromTime, toTime, handler, reader);
    }

}