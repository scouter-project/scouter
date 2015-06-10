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
 */

package scouter.server.db.io.zip;

class Block(_date: String, _buf: Array[Byte], _start: Int, _end: Int, _max: Int) {
    val date = _date;
    var buf = _buf;
    val START = _start;
    var END = _end;
    val MAX = _max;

    var blockNum = 0
    var dirty = false
    var lastAccessTime = 0L

    def this(date: String, buf: Array[Byte], max: Int) {
        this(date, buf, 0, buf.length, max);
    }

    def this(date: String, max: Int) {
        this(date, new Array[Byte](128), 0, 0, max);
    }

    def this(date: String) {
        this(date, GZipCtr.BLOCK_MAX_SIZE)
    }

    def setLastAccessTime(time: Long) {
        this.lastAccessTime = time
    }

    private def ensureCapacity(minCapacity: Int) {
        if (minCapacity > buf.length) {
            val oldCapacity = buf.length;
            var newCapacity = oldCapacity << 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            newCapacity = Math.min(newCapacity, MAX);
            this.buf = copyOf(buf, newCapacity);
        }
    }

    private def copyOf(org: Array[Byte], length: Int): Array[Byte] = {
        val copy = new Array[Byte](length);
        System.arraycopy(org, 0, copy, 0, Math.min(org.length, length));
        return copy;
    }

    def write(b: Array[Byte]): Boolean = {
        return write(b, 0, b.length);
    }

    def write(b: Array[Byte], offset: Int, len: Int): Boolean = {
        this.synchronized {
            if (END + len > MAX)
                return false;
            ensureCapacity(END - START + len);
            System.arraycopy(b, offset, buf, END - START, len);
            END += len;
            this.dirty = true;
            return true;
        }
    }

    def read(pos: Long, len: Int): Array[Byte] = {
        this.synchronized {
            if (len <= 0)
                return null;
            val bpos = (pos - (blockNum * GZipCtr.BLOCK_MAX_SIZE)).toInt;

            if (len + bpos > END)
                return null;
            if (bpos < START)
                return null;

            val out = new Array[Byte](len)

            System.arraycopy(this.buf, bpos - START, out, 0, len);
            return out;
        }
    }

    def getOffset() = END + (blockNum.toLong * GZipCtr.BLOCK_MAX_SIZE);

    def getBlockBytes(): Array[Byte] = {
        def out = new Array[Byte](this.END - this.START);
        System.arraycopy(this.buf, 0, out, 0, this.END - this.START);
        return out;
    }

    def createNextBlock(): Block = {
        val bk = new Block(date, MAX);
        bk.blockNum = this.blockNum + 1;
        return bk;
    }

    def readable(pos: Long): Boolean = {
        val bpos = (pos - (blockNum * GZipCtr.BLOCK_MAX_SIZE)).toInt;

        if (8 + bpos > END)
            return false;
        if (bpos < START)
            return false;
        return true;
    }

    def merge(old: Block): Block = {
        val start = Math.min(this.START, old.START);
        val end = Math.max(this.END, old.END);
        val block = new Array[Byte](end - start)
        System.arraycopy(this.buf, 0, block, this.START - start, this.END - this.START);
        System.arraycopy(old.buf, 0, block, old.START - start, old.END - old.START);

        val b = new Block(this.date, block, start, end, this.MAX);
        b.blockNum = this.blockNum;
        return b;
    }

    override def toString() = "Block [date=" + date + ", blockNum=" + blockNum + ", START=" + START + ", END=" + END + ", MAX=" + MAX + ", dirty=" + dirty + "]"

}