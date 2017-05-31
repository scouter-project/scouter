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
import java.util.Hashtable;

import scouter.server.db.io.RealDataFile;
import scouter.util.FileUtil;
import scouter.util.IClose;

object AlertWriter {
    val table = new Hashtable[String, AlertWriter]();

    def open(file: String): AlertWriter = {
        table.synchronized {
            var writer = table.get(file);
            if (writer != null) {
                writer.refrence += 1
            } else {
                writer = new AlertWriter(file)
                table.put(file, writer);
            }
            return writer;
        }
    }
}

class AlertWriter(file: String) extends IClose {

    var refrence = 0;
    private val out = new RealDataFile(file + ".alert");

    def write(bytes: Array[Byte]): Long = {
        this.synchronized {
            val point = out.getOffset();
            out.writeShort(bytes.length.toShort);
            out.write(bytes);
            out.flush();
            return point;
        }
    }

    override def close() {
        AlertWriter.table.synchronized {
            if (this.refrence == 0) {
                AlertWriter.table.remove(this.file);
                FileUtil.close(out);
            } else {
                this.refrence -= 1
            }
        }
    }
}