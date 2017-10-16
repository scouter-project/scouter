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

package scouter.server.db.obj;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import scouter.util.IClose;

object ObjectData {
    val table = new Hashtable[String, ObjectData]();
    def open(file: String): ObjectData = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
            } else {
                reader = new ObjectData(file);
                table.put(file, reader);
            }
            return reader;
        }
    }
}

class ObjectData(file: String) extends  IClose {

    var refrence = 0;
    val dataFile = new RandomAccessFile(file + ".data", "rw");

    def close() {
        ObjectData.table.synchronized {
            if (this.refrence == 0) {
                ObjectData.table.remove(this.file);
                try {
                    dataFile.close();
                } catch {
                    case e: Throwable =>
                        e.printStackTrace()
                }
            } else {
                this.refrence -= 1;
            }
        }

    }

    def read(fpos: Long): Array[Byte] = {
        this.synchronized {
            try {
                dataFile.seek(fpos);
                val length = dataFile.readInt();
                val buffer = new Array[Byte](length);
                dataFile.read(buffer);
                return buffer;
            } catch {
                case e: IOException =>
                    throw new RuntimeException(e);
            }
        }
    }

    def write(data: Array[Byte]): Long = {
        this.synchronized {
            val location = dataFile.length();
            dataFile.seek(location);
            dataFile.writeInt(data.length);
            dataFile.write(data);
            return location;
        }
    }

    def update(pos: Long, data: Array[Byte]) {
        dataFile.seek(pos);
        dataFile.writeInt(data.length);
        dataFile.write(data);
    }
}