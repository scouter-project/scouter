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
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Properties;

import scouter.server.Configure;
import scouter.server.db.io.zip.GZipStore;
import scouter.util.FileUtil;
import scouter.util.IClose;

object XLogProfileDataReader {
    val table = new Hashtable[String, XLogProfileDataReader]();

    def open(date: String, file: String): XLogProfileDataReader = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
            } else {
                reader = new XLogProfileDataReader(date, file);
                table.put(file, reader);
            }
            return reader;
        }
    }
}

class XLogProfileDataReader(date: String, file: String) extends  IClose {

    var refrence = 0;
    val conf = Configure.getInstance();

    private var profileFile: RandomAccessFile = null
    private var gzip = conf.compress_profile_enabled

    val confFile = new File(file + ".profile.conf");
    if (confFile.exists()) {
        val properties = FileUtil.readProperties(confFile);
        this.gzip = "true".equalsIgnoreCase(properties.getProperty("compress_profile_enabled", ""+conf.compress_profile_enabled).trim());
    }

    val profile = new File(file + ".profile");
    if (profile.canRead() == true) {
        this.profileFile = new RandomAccessFile(profile, "r");
    }

    def read(pos: Long): Array[Byte] = {
        if (this.gzip) {
            return GZipStore.getInstance().read(date, pos);
        }
        if (profileFile == null)
            return null;
        try {
            this.synchronized {
                profileFile.seek(pos);
                val len = profileFile.readInt();
                val buffer = new Array[Byte](len);
                profileFile.read(buffer);

                return buffer;
            }
        } catch {
            case e: IOException =>
                throw new RuntimeException(e);
        }
    }

    override def close() {
        XLogProfileDataReader.table.synchronized {
            if (this.refrence == 0) {
                XLogProfileDataReader.table.remove(this.file);
                try {
                    if (profileFile != null)
                        profileFile.close();
                    profileFile = null;
                } catch {
                    case e: Throwable =>
                        e.printStackTrace();
                }
            } else {
                this.refrence -= 1
            }
        }

    }

}