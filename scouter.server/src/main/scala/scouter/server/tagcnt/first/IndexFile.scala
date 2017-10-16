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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import scouter.io.DataOutputX;
import scouter.lang.value.Value;
import scouter.server.Logger;
import scouter.server.db.io.MemHashBlock;
import scouter.server.tagcnt.core.TagCountUtil;
import scouter.util.CompareUtil;
import scouter.util.HashUtil;
import scouter.util.IClose;

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
class IndexFile(_path: String, hashSize: Int = 1) extends IClose {

    var refrence = 0;

    
    val MB = 1024 * 1024;
    val path = _path
    val hashFile = new MemHashBlock(path, hashSize * MB)
    val keyFile = new KeyDataFile(path);

    def put(key: Array[Byte], value: Array[Float]): Boolean = {
        this.synchronized {
            if (key == null || value == null) {
                throw new IOException("invalid key/value");
            }

            val keyHash = HashUtil.hash(key);
            var pos = hashFile.get(keyHash);
            pos = this.keyFile.append(pos, key, value);
            this.hashFile.put(keyHash, pos);
            return true;
        }
    }

    def get(key: Array[Byte]): Array[Float] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);

        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    return this.keyFile.getValue(pos);
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return null;
    }

    def updateAdd(key: Array[Byte], hhmm: Int, cnt: Float): Float = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);

        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    return this.keyFile.updateAdd(pos, hhmm, cnt);
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return 0;
    }

    def update(key: Array[Byte], value: Array[Float]) {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    this.keyFile.update(pos, value);
                    return ;
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }

    }

    def hasKey(key: Array[Byte]): Boolean = {
        if (key == null) {
            throw new IOException("invalid key");
        }

        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    return true;
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return false;
    }

    def getAll(key: Array[Byte]): List[Array[Float]] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val out = new ArrayList[Array[Float]]();
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    out.add(this.keyFile.getValue(pos));
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }
        return out;
    }

    def delete(key: Array[Byte]): Int = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashFile.get(keyHash);

        var deleted = 0;
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    this.keyFile.setDelete(pos, true);
                    deleted += 1;
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }
        return deleted;
    }

    def read(handler: (Array[Byte],Array[Int])=>Any): Boolean = {
        if (this.keyFile == null)
            return false
        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        var done = 0;
        try {
            while (pos < length && pos >0) {
                val r = this.keyFile.getRecord(pos);
                if (r.deleted == false && r.key.length>0) {
                    handler(r.key, r.value) 
                }
                done += 1
                pos = r.next;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S184", this.keyFile + " : read=" + done + " pos=" + pos + " file-len=" + length + " " + t);
        }
        return true;
    }

    def getStat(): Map[String, Number] = {
        var deleted = 0;
        var count = 0;
        var pos = this.keyFile.getFirstPos();
        var length = this.keyFile.getLength();
        while (pos < length && pos >0) {
            val r = this.keyFile.getRecord(pos);
            if (r.deleted) {
                deleted += 1;
            } else {
                count += 1;
            }
            pos = r.next;
        }
        val scatter = hashFile.getCount();

        val out = new HashMap[String, Number]();
        out.put("count", count);
        out.put("scatter", scatter);
        out.put("deleted", deleted);
        out.put("scan", ((count + deleted) / scatter));

        return out;
    }

    private def _close() {
        hashFile.close();
        keyFile.close();
    }

    // /////////////

    def close() {
        IndexFile.table.synchronized {
            if (this.refrence == 0) {
                this._close();
                IndexFile.table.remove(this.path);
            } else {
                this.refrence -= 1
            }
        }
    }

    def set(tagKey: Long, tagValue: Value, value: Array[Float]) {
        this.synchronized {
            val key = new DataOutputX();
            key.writeLong(tagKey);
            key.writeValue(tagValue);

            this.put(key.toByteArray(), value);
        }
    }

    def update(tagKey: Long, tagValue: Value, value: Array[Float]) {
        this.synchronized {

            try {

                val out = new DataOutputX();
                out.writeLong(tagKey);
                out.writeValue(tagValue);
                val key = out.toByteArray();
                val hask = this.hasKey(key);
                if (hask) {
                    this.update(key, value);
                } else {
                    this.put(key, value);
                }
            } catch {
                case _:Throwable =>
            }
        }
    }

    def add(tagKey: Long, tagValue: Value, hhmm: Int, cnt: Float) {
        this.synchronized {

            try {

                val out = new DataOutputX();
                out.writeLong(tagKey);
                out.writeValue(tagValue);
                val key = out.toByteArray();
                val hask = this.hasKey(key);
                if (hask) {
                    this.updateAdd(key, hhmm, cnt);
                } else {
                    val x = TagCountUtil.getBucketPos(hhmm);
                    val data = new Array[Float](TagCountUtil.BUCKET_SIZE);
                    data(x) = cnt;
                    this.put(key, data);
                }
            } catch {
                case _:Throwable =>
            }
        }
    }

    private def print(tagKey: Long, tagValue: Value, v: Array[Int]) {
        System.out.print(" " + tagKey + "  " + tagValue);
        var i = 0
        while (i < v.length) {
            System.out.print("," + v(i));
        }
        System.out.println("");
    }

    def get(tagKey: Long, tagValue: Value): Array[Float] = {
        this.synchronized {

            val key = new DataOutputX();
            key.writeLong(tagKey);
            key.writeValue(tagValue);

            return this.get(key.toByteArray());
        }
    }

    def hasKey(tagKey: Long, tagValue: Value): Boolean = {
        this.synchronized {

            val key = new DataOutputX();
            key.writeLong(tagKey);
            key.writeValue(tagValue);
            return this.hasKey(key.toByteArray());
        }
    }

}
