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

package scouter.server.db.counter
import java.util.Hashtable
import scouter.server.db.io.IndexKeyFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.FileUtil
import scouter.util.IClose;
object DailyCounterIndex {
    val table = new Hashtable[String, DailyCounterIndex]();

    def open(file: String): DailyCounterIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new DailyCounterIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}
class DailyCounterIndex(_file: String) extends IClose {

    var refrence = 0
    val file = _file
    var index: IndexKeyFile = null
   
    def set(key: Array[Byte], pos: Long) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file)
        }
        this.index.put(key, DataOutputX.toBytes5(pos));
    }

    def get(key: Array[Byte]): Long = {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        val buf = this.index.get(key);
        if (buf == null) -1 else DataInputX.toLong5(buf, 0)
    }

    def read(handler: (Array[Byte], Array[Byte]) => Unit, reader: (Long)=>Array[Byte]) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        this.index.read(handler, reader);
    }

    def read(handler: (Array[Byte], Array[Byte]) => Any) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        this.index.read(handler);
    }

    def close() {
        DailyCounterIndex.table.synchronized {
            if (this.refrence == 0) {
                DailyCounterIndex.table.remove(this.file);
                FileUtil.close(this.index);
            } else {
                this.refrence -= 1;
            }
        }
    }

}