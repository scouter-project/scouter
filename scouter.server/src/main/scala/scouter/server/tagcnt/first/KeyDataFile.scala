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
package scouter.server.tagcnt.first;

import java.io.File
import java.io.RandomAccessFile

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.tagcnt.core.TagCountUtil
import scouter.util.IClose

class KeyDataFile(path: String) extends IClose {
    class ITEM {
        var deleted = false
        var value: Array[Int]=null
        var key: Array[Byte]=null
        var link = 0L
        var next = 0L
    }

    val file = new File(path + ".kfile");
    var raf = new RandomAccessFile(file, "rw");
    if (this.raf.length() == 0) {
        this.raf.write(Array(0xCA.toByte, 0xFE.toByte));
    }

    def getRecord(pos: Long): ITEM = {
        this.synchronized {
            this.raf.seek(pos);

            val r = new ITEM();
            val in = new DataInputX(this.raf);
            val buf = in.read(1 + 5 + TagCountUtil.BUCKET_SIZE * 4);

            val in2 = new DataInputX(buf);
            r.deleted = in2.readBoolean();
            r.link = in2.readLong5();
            r.value = new Array[Int](TagCountUtil.BUCKET_SIZE);
            var inx = 0
            while (inx < TagCountUtil.BUCKET_SIZE) {
                r.value(inx) = in2.readInt();
                inx += 1
            }

            r.key = in.readShortBytes();
            r.next = this.raf.getFilePointer();
            return r;
        }
    }

    def isDeleted(pos: Long): Boolean = {
        this.synchronized {
            this.raf.seek(pos);
            return new DataInputX(this.raf).readBoolean();
        }
    }

    def getHashLink(pos: Long): Long = {
        this.synchronized {
            this.raf.seek(pos + 1);
            return new DataInputX(this.raf).readLong5();
        }
    }

    def getKey(pos: Long): Array[Byte] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5 + TagCountUtil.BUCKET_SIZE * 4);
            val in = new DataInputX(this.raf);
            return in.readShortBytes();
        }
    }

    def getValue(pos: Long): Array[Float] = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            val bytes = new DataInputX(this.raf).read(TagCountUtil.BUCKET_SIZE * 4);
            //
            val in = new DataInputX(bytes);
            val value = new Array[Float](TagCountUtil.BUCKET_SIZE)
            var inx = 0
            while (inx < TagCountUtil.BUCKET_SIZE) {
                value(inx) = in.readFloat();
                inx += 1
            }
            return value;
        }
    }

    def setDelete(pos: Long, deleted: Boolean) {
        this.synchronized {
            this.raf.seek(pos);
            new DataOutputX(this.raf).writeBoolean(deleted);
        }
    }

    def setHashLink(pos: Long, link: Long) {
        this.synchronized {
            this.raf.seek(pos + 1);
            new DataOutputX(this.raf).writeLong5(link);
        }
    }

    def write(pos: Long, next: Long, key: Array[Byte], value: Array[Float]) {
        this.synchronized {
            val out = new DataOutputX();
            out.writeBoolean(false);
            out.writeLong5(next);
            var inx = 0
            while (inx < TagCountUtil.BUCKET_SIZE) {
                out.writeFloat(value(inx));
                inx += 1
            }
            out.writeShortBytes(key);

            this.raf.seek(pos);
            this.raf.write(out.toByteArray());
        }
    }

    def update(pos: Long, hhmm: Int, value: Int) {
        this.synchronized {
            this.raf.seek(pos);
            val bucketPos = TagCountUtil.getBucketPos(hhmm);
            this.raf.seek(pos + 1 + 5 + bucketPos * 4);
            this.raf.write(DataOutputX.toBytes(value));
        }
    }

    def updateAdd(pos: Long, hhmm: Int, value: Float): Float = {
        this.synchronized {
            val bucketPos = TagCountUtil.getBucketPos(hhmm);
            this.raf.seek(pos + 1 + 5 + bucketPos * 4);
            val old = new DataInputX(this.raf).readFloat();
            this.raf.seek(pos + 1 + 5 + bucketPos * 4);
            this.raf.write(DataOutputX.toBytes(old + value));
            return old + value;
        }
    }

    def update(pos: Long, value: Array[Float]) {
        this.raf.seek(pos + 1 + 5);
        val out = new DataOutputX(this.raf);
        var inx = 0
        while (inx < TagCountUtil.BUCKET_SIZE) {
            out.writeFloat(value(inx));
            inx += 1
        }
    }

    def updateAdd(pos: Long, value: Array[Int]): Int = {
        this.synchronized {
            this.raf.seek(pos + 1 + 5);
            val oldbytes = new DataInputX(this.raf).read(value.length * 4);
            val out = new DataOutputX();
            var idx = 0
            while (idx < value.length) {
                val old = DataInputX.toInt(oldbytes, idx * 4);
                out.writeInt(old + value(idx));
                idx += 1
            }
            this.raf.seek(pos + 1 + 5);
            this.raf.write(out.toByteArray());

            return value.length;
        }
    }

    def append(next: Long, key: Array[Byte], value: Array[Float]): Long = {
        this.synchronized {
            val pos = this.raf.length();
            write(pos, next, key, value);
            return pos;
        }
    }

    def close() {
        this.synchronized {
            if (this.raf == null)
                return ;
            try {
                this.raf.close();
            } catch {
                case _:Throwable =>
            }
            this.raf = null;
        }
    }

    def getFirstPos() = 2L
    def getLength() = if (raf == null) 0 else raf.length();
}
