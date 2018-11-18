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

package scouter.server.db.xlog;

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Hashtable
import scouter.server.db.io.zip.GZipStore
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.server.Configure

object XLogDataReader {
    val table = new Hashtable[String, XLogDataReader]();

    def open(date: String, file: String): XLogDataReader = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.reference += 1;
            } else {
                reader = new XLogDataReader(date, file);
                table.put(file, reader);
            }
            return reader;
        }
    }

}
class XLogDataReader(date: String, file: String) extends IClose {

    var reference = 0;
    val conf = Configure.getInstance()
    var pointFile: RandomAccessFile = null
    var gzip = conf.compress_xlog_enabled

    val confFile = new File(file + ".service.conf");
    if (confFile.exists()) {
        var properties = FileUtil.readProperties(confFile);
        this.gzip = "true".equalsIgnoreCase(properties.getProperty("compress_xlog_enabled", "" + conf.compress_xlog_enabled).trim());
    }

    val xlogFile = new File(file + ".service");
    if (xlogFile.canRead()) {
        this.pointFile = new RandomAccessFile(xlogFile, "r");
    }

    def read(point: Long): Array[Byte] = {
        if (gzip) {
            return GZipStore.getInstance().read(date, point);
        }
        if (pointFile == null)
            return null;
        try {
            this.synchronized {
                pointFile.seek(point);
                val len = pointFile.readShort();
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
        XLogDataReader.table.synchronized {
            if (this.reference == 0) {
                XLogDataReader.table.remove(this.file);
                pointFile = FileUtil.close(pointFile);
            } else {
                this.reference -= 1;
            }
        }
    }

}