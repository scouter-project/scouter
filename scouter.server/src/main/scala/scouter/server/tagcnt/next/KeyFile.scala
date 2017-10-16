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
 */
package scouter.server.tagcnt.next;

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.IClose;
import scouter.util.FileUtil

class ITEM(_pos24h: Array[Long], _key: Array[Byte], _link: Long, _next: Long, _count: Float) {
    val pos24h = _pos24h
    val key = _key
    val link = _link
    val next = _next
    val count = _count
}

class KeyFile(path: String) extends IClose {

    val file = new File(path + ".kfile")
    var raf = new RandomAccessFile(file, "rw");
    if (this.raf.length() == 0) {
        this.raf.write(Array[Byte](0xCA.toByte, 0xFE.toByte))
    }

    def getRecord(pos: Long): ITEM = {
        this.synchronized {
            this.raf.seek(pos);

            val in = new DataInputX(this.raf);
            val buf = in.read(5 + 4 + 24 * 5);

            val in2 = new DataInputX(buf);
            val link = in2.readLong5();
            val count = in2.readFloat();
            val pos24h = new Array[Long](24)
            for (i <- 0 to 23) {
                pos24h(i) = in2.readLong5();
            }
            val key = in.readShortBytes();
            val next = this.raf.getFilePointer();
            return new ITEM(pos24h, key, link, next, count);
        }
    }

    def getHashLink(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos);
            return new DataInputX(this.raf).readLong5();
        }
    }

    def getKey(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 5 + 4 + 24 * 5);
            val in = new DataInputX(this.raf);
            return in.readShortBytes();
        }
    }

    def getVPos(pos: Long): Array[Long] = {
        this.synchronized {
            this.raf.seek(pos + 5 + 4);
            val bytes = new DataInputX(this.raf).read(24 * 5);
            //
            val in = new DataInputX(bytes);
            val value = new Array[Long](24)
            for (i <- 0 to 23) {
                value(i) = in.readLong5();
            }
            return value;
        }
    }

    def getTotalCount(pos: Long): Float = {
        this.synchronized {
            this.raf.seek(pos + 5);
            val in = new DataInputX(this.raf);
            return in.readFloat();
        }
    }

    def setHashLink(pos: Long, link: Long) {
        this.synchronized {
            this.raf.seek(pos);
            new DataOutputX(this.raf).writeLong5(link);
        }
    }

    def write(pos: Long, next: Long, key: Array[Byte], cntSum: Float, vpos: Array[Long]) {
        this.synchronized {
            val out = new DataOutputX();
            out.writeLong5(next);
            out.writeFloat(cntSum);
            for (i <- 0 to 23) {
                out.writeLong5(vpos(i));
            }
            out.writeShortBytes(key);

            this.raf.seek(pos);
            this.raf.write(out.toByteArray());
        }
    }

    def update(pos: Long, hour: Int, vpos: Long) {
        this.synchronized {
            this.raf.seek(pos + 5 + 4 + hour * 5);
            this.raf.write(DataOutputX.toBytes5(vpos));
        }
    }

    def addTotalCount(pos: Long, totalCount: Float): Float = {
        this.synchronized {
            this.raf.seek(pos + 5);
            val old = new DataInputX(this.raf).readFloat();
            this.raf.seek(pos + 5);
            this.raf.write(DataOutputX.toBytes(old + totalCount));
            return old + totalCount;
        }
    }
    def append(next: Long, key: Array[Byte], count: Float, vpos: Array[Long]): Long = {
        val pos = this.raf.length();
        write(pos, next, key, count, vpos);
        return pos;
    }

    def close() {
        this.synchronized {
            if (this.raf != null) {
                FileUtil.close(this.raf);
                this.raf = null
            }
        }
    }

    def getFirstPos() = 2L
    def getLength() = if (raf == null) 0 else raf.length()

}
