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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.FileUtil;
import scouter.util.IClose;

class Key {
    var objHash = 0
    var mtype: Byte = 0
    var pos = 0L

    override def toString(): String = {
        "Key [objHash=" + objHash + ", type=" + mtype + ", pos=" + pos + "]";
    }
}

class SummaryKeyFile(path: String) extends IClose {

    protected val file = new File(path + ".key")
    protected val braf = new RandomAccessFile(file, "rw");
    if (this.file.length() == 0) {
        this.braf.write(Array(0xCA.toByte, 0xFE.toByte));
    }

    def getRecord(pos: Long): Key = {
        this.synchronized {
            this.braf.seek(pos);
            val len = this.braf.readByte();
            this.braf.seek(pos + 1);
            val b = new Array[Byte](len);
            this.braf.read(b);

            val in = new DataInputX(b);

            val data = new Key();
            data.objHash = in.readDecimal().toInt
            data.mtype = in.readByte();
            data.pos = in.readDecimal();
            return data;
        }
    }

    def write(pos: Long, r: Key) {
        this.synchronized {
            val out = new DataOutputX();
            out.writeByte(0); // 길이정보
            out.writeDecimal(r.objHash);
            out.writeByte(r.mtype);
            out.writeDecimal(r.pos);

            val data = out.toByteArray();
            data(0) = (data.length - 1).toByte
            this.braf.seek(pos);
            this.braf.write(data);
        }
    }

    def write(r: Key): Long = {
        val pos = this.braf.length();
        write(pos, r);
        return pos;
    }

    override def close() {
        this.synchronized {
            FileUtil.close(this.braf);
        }
    }

}