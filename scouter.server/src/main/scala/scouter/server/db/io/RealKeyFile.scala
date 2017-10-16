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

package scouter.server.db.io;

import java.io.{File, RandomAccessFile}

import scouter.io.{DataInputX, DataOutputX}
import scouter.util.IClose;

class ITEM {
    var deleted = false;
    var prevPos = 0L;
    var timeKey: Array[Byte] = null;
    var dataPos: Array[Byte] = null;
    var offset = 0L
}

class RealKeyFile(_path: String) extends IClose {
    val path = _path;
    val file = new File(path + ".kfile");
    protected var raf = new RandomAccessFile(file, "rw");
    if (this.raf.length() == 0) {
        this.raf.write(Array[Byte](0xCA.toByte, 0xFE.toByte));
    }

    def getRecord(pos: Long): ITEM = {
        this.synchronized {
            this.raf.seek(pos);
            val in = new DataInputX(this.raf);

            val r = new ITEM();
            r.deleted = in.readBoolean();
            r.prevPos = in.readLong5();
            r.timeKey = in.readShortBytes();
            r.dataPos = in.readBlob();
            r.offset = this.raf.getFilePointer();
            return r;
        }
    }

    def isDeleted(pos: Long): Boolean = {
        this.synchronized {
            this.raf.seek(pos);
            return new DataInputX(this.raf).readBoolean();
        }
    }
    def getPrevPos(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos + 1);
            return new DataInputX(this.raf).readLong5();
        }
    }

    def getTimeKey(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            return new DataInputX(this.raf).readShortBytes();
        }
    }

    def getDataPos(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            val in = new DataInputX(raf);
            val keyLen = this.raf.readShort();
            in.skipBytes(keyLen);
            return in.readBlob()
        }
    }

    def setDelete(pos: Long, bool: Boolean) {
        this.synchronized {
            this.raf.seek(pos);
            new DataOutputX(this.raf).writeBoolean(bool);
        }
    }

    def setHashLink(pos: Long, value: Long) {
        this.synchronized {
            this.raf.seek(pos + 1);
            new DataOutputX(this.raf).writeLong5(value);
        }
    }

    def write(pos: Long, prevPos: Long, indexKey: Array[Byte], dataPos: Array[Byte]) {
        this.synchronized {
            this.raf.seek(pos);

            val out = new DataOutputX();
            out.writeBoolean(false);
            out.writeLong5(prevPos);
            out.writeShortBytes(indexKey);
            out.writeBlob(dataPos);
         
            this.raf.write(out.toByteArray())
        }
    }
    def update(pos: Long, key: Array[Byte], value: Array[Byte]): Boolean = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            
            val in = new DataInputX(raf);
            val keylen = this.raf.readShort();
            in.skipBytes(keylen);
            val org = in.readBlob();
            if (org.length < value.length)
                return false;
            this.raf.seek(pos + 1 + 5 + 2 + keylen);
            
            val out = new DataOutputX(this.raf);
            out.writeBlob(value);
            return true;
        }
    }
    def append(prevPos: Long, indexKey: Array[Byte], datePos: Array[Byte]): Long = {
        this.synchronized {
            val pos = this.raf.length();
            write(pos, prevPos, indexKey, datePos);
            return pos;
        }
    }

    def close() {
        if (this.raf == null)
            return ;
        try {
            this.raf.close();
        } catch {
            case t: Throwable => t.printStackTrace();
        }
        this.raf = null;
    }

    def terminate() {
        close();
        try {
            file.delete();
        } catch {
            case t: Throwable => t.printStackTrace();
        }
    }

    def getFirstPos(): Long = {
        return 2;
    }

    def getLength(): Long = {
        return if (raf == null) 0 else raf.length()
    }

}