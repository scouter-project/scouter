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

package scouter.server.db.alert;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import scouter.util.FileUtil;
import scouter.util.IClose;

object AlertReader {
    val table = new Hashtable[String, AlertReader]();

    def open(file: String): AlertReader = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
                return reader;
            } else {
                reader = new AlertReader(file);
                table.put(file, reader);
                return reader;
            }
        }
    }
}

class AlertReader(file: String) extends  IClose {

    var refrence = 0;
    private val pointFile = new RandomAccessFile(file + ".alert", "rw");
    def read(point: Long): Array[Byte] = {
        try {
            this.synchronized {
                pointFile.seek(point);
                val len = pointFile.readShort();
                val buffer = new Array[Byte](len)
                pointFile.read(buffer);
                return buffer;
            }
        } catch {
            case e: IOException => throw new RuntimeException(e)
        }
    }

    override def close() {
        AlertReader.table.synchronized {
            if (this.refrence == 0) {
                AlertReader.table.remove(file);
                FileUtil.close(pointFile);
            } else {
                this.refrence -= 1
            }
        }

    }

}