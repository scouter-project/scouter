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

object XLogProfileDataWriter {
    val table = new Hashtable[String, XLogProfileDataWriter]();

    def open(date: String, file: String): XLogProfileDataWriter = {
        table.synchronized {
            var writer = table.get(file);
            if (writer != null) {
                writer.refrence += 1;
            } else {
                writer = new XLogProfileDataWriter(date, file);
                table.put(file, writer);
            }
            return writer;
        }
    }
}

class XLogProfileDataWriter(date: String, file: String) extends IClose {

    var refrence = 0;
    val conf = Configure.getInstance();

    var gzip = conf.gzip_profile

    val f = new File(file + ".profile.conf");
    if (f.exists()) {
        val properties = FileUtil.readProperties(f);
        gzip = "true".equalsIgnoreCase(properties.getProperty("gzip_profile", "" + conf.gzip_profile).trim());
    } else {
        gzip = conf.gzip_profile;
        val properties = new Properties();
        properties.put("gzip_profile", "" + conf.gzip_profile);
        FileUtil.writeProperties(f, properties);
    }
    var out: RealDataFile = null;
    if (gzip==false) {
        out = new RealDataFile(file + ".profile");
    }

    def write(bytes: Any): Long = {
        return write(bytes.asInstanceOf[Array[Byte]]);
    }
    def write(bytes: Array[Byte]): Long = {
        if (gzip) {
            return GZipStore.getInstance().write(date, bytes);
        }
        this.synchronized {
            val point = out.getOffset();
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
            return point;
        }
    }

    override def close() {
        XLogProfileDataWriter.table.synchronized {
            if (this.refrence == 0) {
                XLogProfileDataWriter.table.remove(this.file);
                FileUtil.close(out)
            } else {
                this.refrence -= 1;
            }
        }

    }

}