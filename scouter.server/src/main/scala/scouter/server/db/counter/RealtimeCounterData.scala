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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import scouter.util.FileUtil;
import scouter.util.IClose;

object RealtimeCounterData {
    val table = new Hashtable[String, RealtimeCounterData]();

    def open(file: String): RealtimeCounterData = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
            } else {
                reader = new RealtimeCounterData(file);
                table.put(file, reader);
            }
            return reader;
        }
    }

}
class RealtimeCounterData(file: String) extends  IClose {

    var refrence = 0;
    private val dataFile = new RandomAccessFile(file + ".data", "rw");

    override def close() {
        RealtimeCounterData.table.synchronized {
            if (this.refrence == 0) {
                RealtimeCounterData.table.remove(this.file);
                FileUtil.close(dataFile);
            } else {
                this.refrence -= 1
            }
        }

    }

    def read(pos: Long): Array[Byte] = {
        this.synchronized {
            dataFile.seek(pos);
            val length = dataFile.readInt();
            val buffer = new Array[Byte](length);
            dataFile.read(buffer);
            return buffer;
        }
    }

    def readBulk(start: Long, end: Long): Array[Byte] = {
        this.synchronized {
            dataFile.seek(end)
            val length = dataFile.readInt()
            val buffer = new Array[Byte]((end-start).toInt + length + 4)
            dataFile.seek(start)
            dataFile.read(buffer)
            return buffer
        }
    }

    def write(data: Array[Byte]): Long = {
        if (data == null || data.length == 0)
            return 0;
        this.synchronized {
            val offset = dataFile.length()
            dataFile.seek(offset);
            dataFile.writeInt(data.length);
            dataFile.write(data);
            return offset;
        }
    }
}