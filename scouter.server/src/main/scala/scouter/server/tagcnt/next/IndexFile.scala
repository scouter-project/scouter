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

import java.io.IOException
import java.util.Hashtable
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.value.Value
import scouter.server.Logger
import scouter.server.db.io.MemHashBlock
import scouter.server.tagcnt.core.TagCountUtil
import scouter.util.CompareUtil
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.IClose
import scouter.util.DateUtil
import scala.util.control.Breaks._

object IndexFile {

    val table = new Hashtable[String, IndexFile]();

    def open(path: String): IndexFile = {
        table.synchronized {
            var inx = table.get(path);
            if (inx != null) {
                inx.refrence += 1;
                return inx;
            } else {
                inx = new IndexFile(path);
                table.put(path, inx);
                return inx;
            }
        }
    }
}
class IndexFile(path: String, hashSize: Int = 1) extends IClose {

    var refrence = 0;

    val MB = 1024 * 1024;
    val hashFile = new MemHashBlock(path, hashSize * MB)
    val keyFile = new KeyFile(path);
    val dataFile = new DataFile(path)

    def getKeyFile() = keyFile

    def getDataPosition(key: Array[Byte]): Array[Long] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                val r = this.keyFile.getRecord(pos);
                return r.pos24h;
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return null;
    }

    def updateAdd(key: Array[Byte], hour: Int, value: Array[Float]): Int = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);

        val firstPos = pos;
        while (pos > 0) {
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                val r = this.keyFile.getRecord(pos);
                if (r.pos24h(hour) <= 0) {
                    val vpos = this.dataFile.append(value);
                    this.keyFile.update(pos, hour, vpos);
                } else {
                    this.dataFile.updateAdd(r.pos24h(hour), value);
                }
                this.keyFile.addTotalCount(pos, TagCountUtil.sum(value));
                return 1;
            }
            pos = this.keyFile.getHashLink(pos);
        }
        // no saved values
        val vpos = this.dataFile.append(value);
        val vposArr = new Array[Long](24);
        vposArr(hour) = vpos;
        pos = this.keyFile.append(firstPos, key, TagCountUtil.sum(value), vposArr);
        this.hashFile.put(keyHash, pos);

        return 0;
    }

    def hasKey(key: Array[Byte]): Boolean = {
        if (key == null) {
            throw new IOException("invalid key");
        }

        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                return true;
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return false;
    }

    def getTotalCount(key: Array[Byte]): Float = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                return this.keyFile.getTotalCount(pos);
            }
            pos = this.keyFile.getHashLink(pos);
        }
        return 0;
    }

    def read(handler: (Long, Value, Float, Array[Long], IndexFile, Long) => Any): Boolean = {
        if (this.keyFile == null)
            return false;

        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        var done = 0;
        while (pos < length && pos > 0) {
            var r = this.keyFile.getRecord(pos);
            if (r.key.length > 0) {
                val in = new DataInputX(r.key);
                val tag = in.readLong();
                val value = in.readValue();

                handler(tag, value, r.count, r.pos24h, this, pos)
            }
            pos = r.next;
            done += 1
        }
        return true;
    }

    def getValue(vpos: Long): Array[Float] = {
        if (vpos <= 0)
            return null;
        return this.dataFile.getValue(vpos);
    }

    private def _close() {
        this.hashFile.close();
        FileUtil.close(this.keyFile);
        FileUtil.close(this.dataFile);
    }

    override def close() {
        IndexFile.table.synchronized {
            if (this.refrence == 0) {
                this._close();
                IndexFile.table.remove(this.path);
            } else {
                this.refrence -= 1
            }
        }
    }

    def getValueAll(vpos: Array[Long]): Array[Float] = {
        val out = new Array[Float](1440);
        for (i <- 0 to 23) {
            val value = getValue(vpos(i));
            if (value != null) {
                System.arraycopy(value, 0, out, i * 60, 60);
            }
        }
        return out;
    }

    def cleanValue(pos: Long) {
        this.dataFile.write(pos, new Array[Float](60));
    }

    def add(tagKey: Long, tagvalue: Value, hh: Int, value: Array[Float]) {
        try {
            val out = new DataOutputX();
            out.writeLong(tagKey);
            out.writeValue(tagvalue);
            val key = out.toByteArray();
            this.updateAdd(key, hh, value);
        } catch {
            case e: Throwable => Logger.println("S204", 10, e.toString())
        }
    }

    def get(tagKey: Long, tagvalue: Value): Array[Float] = {

        val key = new DataOutputX();
        key.writeLong(tagKey);
        key.writeValue(tagvalue);

        val vpos = this.getDataPosition(key.toByteArray());
        if (vpos == null)
            return null;

        return this.getValueAll(vpos);
    }

    def hasKey(tagKey: Long, tagvalue: Value): Boolean = {

        val key = new DataOutputX();
        key.writeLong(tagKey);
        key.writeValue(tagvalue);
        return this.hasKey(key.toByteArray());
    }

}
