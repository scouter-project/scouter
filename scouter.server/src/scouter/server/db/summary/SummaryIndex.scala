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

package scouter.server.db.summary;

import java.util.Hashtable
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.server.db.io.IndexTimeFile

object SummaryIndex {
    val table = new Hashtable[String, SummaryIndex]();
    def open(file: String): SummaryIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new SummaryIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}
class SummaryIndex(_file: String) extends IClose {

    var refrence = 0;
    private var timeIndex: IndexTimeFile = null;
    private var keyfile: SummaryKeyFile = null;
    private val file = _file;

    def add(time: Long, objHash: Int, mtype: Byte, pos: Long) {
        checkOpen
        val key = new Key();
        key.objHash = objHash;
        key.mtype = mtype;
        key.pos = pos;
        val fpos = this.keyfile.write(key);
        this.timeIndex.put(time, DataOutputX.toBytes5(fpos));
    }

    override def close() {
        SummaryIndex.table.synchronized {
            if (this.refrence == 0) {
                SummaryIndex.table.remove(this.file);
                FileUtil.close(this.timeIndex);
                FileUtil.close(this.keyfile);
            } else {
                this.refrence -= 1;
            }
        }
    }

    def read(fromTime: Long, toTime: Long, mtype: Byte, handler: (Long, Int, Byte, Long, SummaryDataReader) => Any, reader: SummaryDataReader) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file);
            this.keyfile = new SummaryKeyFile(file);
        }
        val handle = (time: Long, data: Array[Byte]) => {
            val pos = DataInputX.toLong5(data, 0);
            try {
                val key = keyfile.getRecord(pos);
                if (key.mtype == mtype) {
                    handler(time, key.objHash, key.mtype, key.pos, reader)
                }
            } catch {
                case e: Exception =>
                    e.printStackTrace();
            }
        };
         this.timeIndex.read(fromTime, toTime, handle);
    }

    def readFromEnd(fromTime: Long, toTime: Long, mtype: Byte, handler: (Long, Int, Byte, Long, SummaryDataReader) => Any, reader: SummaryDataReader) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file);
            this.keyfile = new SummaryKeyFile(file);
        }
        val handle = (time: Long, data: Array[Byte]) => {
            val pos = DataInputX.toLong5(data, 0);
            try {
                val key = keyfile.getRecord(pos);
                if (key.mtype == mtype) {
                    handler(time, key.objHash, key.mtype, key.pos, reader);
                }
            } catch {
                case e: Exception =>
                    e.printStackTrace();
            }
        };
        this.timeIndex.readFromEnd(fromTime, toTime, handle);
    }

    private def checkOpen {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file);
            this.keyfile = new SummaryKeyFile(file);
        }
    }

}