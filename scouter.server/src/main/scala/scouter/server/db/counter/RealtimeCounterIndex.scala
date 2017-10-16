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

package scouter.server.db.counter;

import java.util.Hashtable

import scouter.lang.value.MapValue
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.IntKeyMap

object RealtimeCounterIndex {
    val table = new Hashtable[String, RealtimeCounterIndex]();

    def open(file: String): RealtimeCounterIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new RealtimeCounterIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}
class RealtimeCounterIndex(_file: String) extends IClose {

    var refrence = 0
    var index: RealtimeCounterKeyFile = null
    val file = _file

    def write(objHash: Int, time: Long, pos: Long) {
        if (this.index == null) {
            this.index = new RealtimeCounterKeyFile(file);
        }
        this.index.write(objHash, time, pos);
    }

    def read(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Any, dataMap: IntKeyMap[String],
        reader: (Long)=>Array[Byte]) {
        if (this.index == null) {
            this.index = new RealtimeCounterKeyFile(file);
        }
        this.index.read(objHash, stime, etime, handler, dataMap, reader);
    }

    def readFromEnd(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Any, dataMap: IntKeyMap[String],
        reader: (Long)=>Array[Byte]) {
        if (this.index == null) {
            this.index = new RealtimeCounterKeyFile(file);
        }
        this.index.readFromEnd(objHash, stime, etime, handler, dataMap, reader);
    }

    def getStartEndDataPos(stime: Long, etime: Long): (Array[Byte], Array[Byte]) = {
        if (this.index == null) {
            this.index = new RealtimeCounterKeyFile(file);
        }
        this.index.getStartEndDataPos(stime, etime);
    }

    override def close() {
        RealtimeCounterIndex.table.synchronized {
            if (this.refrence == 0) {
                RealtimeCounterIndex.table.remove(this.file);
                FileUtil.close(this.index);
                this.index = null;
            } else {
                this.refrence -= 1;
            }
        }
    }

}