/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.db.io

import java.io.{File, RandomAccessFile}

import scouter.io.{DataInputX, DataOutputX}
import scouter.util.IClose;

class KeyFileItem2 {
    var deleted = false;
    var expire = 0L; //unixtimestamp
    var prevPos = 0L;
    var key: Array[Byte] = null;
    var dataPos: Array[Byte] = null;
    var offset = 0L
}

/**
  * RealKeyFile2 is index file which can control TTL.
  */
class RealKeyFile2(_path: String) extends IClose {
    val path = _path;
    val file = new File(path + ".k2file");
    protected var raf = new RandomAccessFile(file, "rw");
    if (this.raf.length() == 0) {
        this.raf.write(Array[Byte](0xCA.toByte, 0xFE.toByte));
    }

    def getRecord(pos: Long): KeyFileItem2 = {
        this.synchronized {
            this.raf.seek(pos);
            val in = new DataInputX(this.raf);

            val item = new KeyFileItem2();
            item.deleted = in.readBoolean();
            item.expire = in.readLong5();
            item.prevPos = in.readLong5();
            item.key = in.readShortBytes();
            item.dataPos = in.readBlob();
            item.offset = this.raf.getFilePointer();
            return item;
        }
    }

    def isDeleted(pos: Long): Boolean = {
        this.synchronized {
            this.raf.seek(pos);
            return new DataInputX(this.raf).readBoolean();
        }
    }

    def isExpired(pos: Long): Boolean = {
        return getExpire(pos) < System.currentTimeMillis() / 1000
    }

    def isDeletedOrExpired(pos: Long): Boolean = {
        this.synchronized {
            this.raf.seek(pos)
            val in = new DataInputX(this.raf)

            val isDeleted = in.readBoolean()
            val expire = in.readLong5()

            return isDeleted || expire < System.currentTimeMillis() / 1000
        }
    }

    def getExpire(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos + 1)
            return new DataInputX(this.raf).readLong5()
        }
    }

    def getTTL(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos + 1);
            return new DataInputX(this.raf).readLong5() - (System.currentTimeMillis() / 1000);
        }
    }

    def getPrevPos(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            return new DataInputX(this.raf).readLong5();
        }
    }

    def getKey(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5 + 5);
            return new DataInputX(this.raf).readShortBytes();
        }
    }

    def getDataPos(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5 + 5);
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

    def setTTL(pos: Long, ttlSec: Long) = {
        this.synchronized {
            this.raf.seek(pos + 1);
            val expire = if(ttlSec < 0) DataInputX.LONG5_MAX_VALUE * 1L
                else System.currentTimeMillis()/1000L + ttlSec

            new DataOutputX(this.raf).writeLong5(expire)
        }
    }

    def setExpire(pos: Long, expireUnixTimestamp: Long) = {
        this.synchronized {
            this.raf.seek(pos + 1);
            new DataOutputX(this.raf).writeLong5(expireUnixTimestamp);
        }
    }

    def setHashLink(pos: Long, value: Long) {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            new DataOutputX(this.raf).writeLong5(value);
        }
    }

    def write(pos: Long, prevPos: Long, indexKey: Array[Byte], dataPos: Array[Byte]) {
        write(pos, -1L, prevPos, indexKey, dataPos)
    }

    def write(pos: Long, ttl: Long, prevPos: Long, indexKey: Array[Byte], dataPos: Array[Byte]) {
        this.synchronized {
            this.raf.seek(pos);

            val out = new DataOutputX();
            out.writeBoolean(false);

            val expire = if(ttl < 0) DataInputX.LONG5_MAX_VALUE * 1L
                else System.currentTimeMillis()/1000L + ttl;

            out.writeLong5(expire);
            out.writeLong5(prevPos);
            out.writeShortBytes(indexKey);
            out.writeBlob(dataPos);
         
            this.raf.write(out.toByteArray())
        }
    }

    def update(pos: Long, ttl: Long, key: Array[Byte], dataPos: Array[Byte]): Boolean = {
        if(pos < 0) return false;

        this.synchronized {
            this.raf.seek(pos + 1 + 5);

            val in = new DataInputX(raf);

            val prevPos = in.readLong5();
            val keyLen = this.raf.readShort();
            in.skipBytes(keyLen);
            val org = in.readBlob();
            if (org.length < dataPos.length)
                return false;

            this.raf.seek(pos);
            val out = new DataOutputX(this.raf);
            write(pos, ttl, prevPos, key, dataPos);
            return true;
        }
    }

    def append(prevPos: Long, indexKey: Array[Byte], datePos: Array[Byte]): Long = {
        return append(prevPos, -1L, indexKey, datePos)
    }

    def append(prevPos: Long, ttl: Long, indexKey: Array[Byte], datePos: Array[Byte]): Long = {
        this.synchronized {
            val pos = this.raf.length();
            write(pos, ttl, prevPos, indexKey, datePos);
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