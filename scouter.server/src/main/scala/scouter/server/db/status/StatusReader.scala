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

package scouter.server.db.status;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import scouter.util.FileUtil;
import scouter.util.IClose;

object StatusReader {
    val table = new Hashtable[String, StatusReader]();

    def open(file: String): StatusReader = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
            } else {
                reader = new StatusReader(file);
                table.put(file, reader);
            }
            return reader;
        }
    }
}

class StatusReader(file: String) extends IClose {

    var refrence = 0;
    val pointFile = new RandomAccessFile(file + ".pstat", "rw");

    def read(point: Long): Array[Byte] = {
        try {
            this.synchronized {
                pointFile.seek(point);
                val len = pointFile.readInt();
                val buffer = new Array[Byte](len);
                pointFile.read(buffer);
                return buffer;
            }
        } catch {
            case e: IOException =>
                throw new RuntimeException(e);
        }
    }

    override def close() {
        StatusReader.table.synchronized {
            if (this.refrence == 0) {
                StatusReader.table.remove(this.file);
                FileUtil.close(pointFile);
            } else {
                this.refrence -= 1;
            }
        }
    }
}