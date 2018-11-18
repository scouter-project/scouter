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

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import scouter.server.Configure;
import scouter.server.db.io.RealDataFile;
import scouter.server.db.io.zip.GZipStore;
import scouter.util.FileUtil;
import scouter.util.IClose;

object XLogDataWriter {
    val table = new Hashtable[String, XLogDataWriter]();

    def open(date: String, file: String): XLogDataWriter = {
        table.synchronized {
            var writer = table.get(file);
            if (writer != null) {
                writer.reference += 1;
            } else {
                writer = new XLogDataWriter(date, file);
                table.put(file, writer);
            }
            return writer;
        }
    }

}
class XLogDataWriter(date: String, file: String) extends IClose {
    var reference = 0;
    val conf = Configure.getInstance()

    var gzip = conf.compress_xlog_enabled

    var f = new File(file + ".service.conf");
    if (f.exists()) {
        val properties = FileUtil.readProperties(f);
        gzip = "true".equalsIgnoreCase(properties.getProperty("compress_xlog_enabled", ""+conf.compress_xlog_enabled).trim());
    } else {
        gzip = conf.compress_xlog_enabled;
        val properties = new Properties();
        properties.put("compress_xlog_enabled", "" + conf.compress_xlog_enabled);
        FileUtil.writeProperties(f, properties);
    }

     var out:RealDataFile = null
     if(gzip==false){
         out=new RealDataFile(file + ".service");
     }


    def write(bytes: Array[Byte]): Long = {
        if (gzip) {
            return GZipStore.getInstance().write(date, bytes);
        }
        this.synchronized {
            val point = out.getOffset();
            out.writeShort(bytes.length.toShort);
            out.write(bytes);
            out.flush();
            return point;
        }
    }

    override def close() {
        XLogDataWriter.table.synchronized {
            if (this.reference == 0) {
                XLogDataWriter.table.remove(this.file);
                FileUtil.close(out);
            } else {
                this.reference -= 1
            }
        }
    }
}