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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scouter.server.Logger;
import scouter.io.DataInputX;
import scouter.util.CompareUtil;
import scouter.util.HashUtil;
import scouter.util.IClose;

class IndexKeyFile(_path: String, hashSize: Int = 1) extends IClose {

    val MB = 1024 * 1024;

    val path = _path
    val hashBlock = new MemHashBlock(path, hashSize * MB)
    val keyFile = new RealKeyFile(path)

    def putAll(table: IndexKeyFile): Int = {
        var count = 0
        table.read((key: Array[Byte], data: Array[Byte]) => {
            try {
                IndexKeyFile.this.put(key, data);
                count += 1;
            } catch {
                case e: Exception =>
            }
        })
        return count;
    }

    def put(key: Array[Byte], dataOffset: Array[Byte]): Boolean = {
        if (key == null || dataOffset == null) {
            throw new IOException("invalid key/value");
        }

        val keyHash = HashUtil.hash(key);
        var prevKeyPos = hashBlock.get(keyHash);
        var newKeyPos = this.keyFile.append(prevKeyPos, key, dataOffset);
        this.hashBlock.put(keyHash, newKeyPos);
        return true;
    }

    def update(key: Array[Byte], value: Array[Byte]): Boolean = {
        if (key == null || value == null) {
            throw new IOException("invalid key/value");
        }

        val keyHash = HashUtil.hash(key);
        val pos = hashBlock.get(keyHash);
        return this.keyFile.update(pos, key, value);
    }

    def get(key: Array[Byte]): Array[Byte] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var realKeyPos = hashBlock.get(keyHash);

        try {
            while (realKeyPos > 0) {
                if (this.keyFile.isDeleted(realKeyPos) == false) {
                    val oKey = this.keyFile.getKey(realKeyPos);
                    if (CompareUtil.equals(oKey, key)) {
                        return this.keyFile.getValue(realKeyPos);
                    }
                }
                realKeyPos = this.keyFile.getHashLink(realKeyPos);
            }
        } catch {
            case e: IOException =>
                Logger.println("S124", "pos=" + realKeyPos + " keyFile.lengrh=" + this.keyFile.getLength());
                throw e;
        }
        return null;
    }

    def hasKey(key: Array[Byte]): Boolean = {
        if (key == null) {
            throw new IOException("invalid key");
        }

        val keyHash = HashUtil.hash(key);
        var pos = hashBlock.get(keyHash);
        while (pos > 0) {
            if (!this.keyFile.isDeleted(pos)) {
                val okey = this.keyFile.getKey(pos);
                if (CompareUtil.equals(okey, key)) {
                    return true;
                }
            }
            pos = this.keyFile.getHashLink(pos);
        }

        return false;
    }

    def getAll(key: Array[Byte]): List[Array[Byte]] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val out = new ArrayList[Array[Byte]]()
        val keyHash = HashUtil.hash(key);
        var pos = hashBlock.get(keyHash);
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
        var pos = hashBlock.get(keyHash);

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

    def read(handler: (Array[Byte], Array[Byte]) => Any) {
        if (this.keyFile == null)
            return
        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        var done = 0;
        try {
            while (pos < length && pos >0) {
                val r = this.keyFile.getRecord(pos);
                if (r.deleted == false) {
                    handler(r.key, r.value)
                }
                done += 1;
                pos = r.next;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S125", this.keyFile.path + " : read=" + done + " pos=" + pos + " file-len=" + length + " " + t);
        }
    }

    def read(handler: (Array[Byte], Array[Byte]) => Any, reader: (Long)=>Array[Byte]) {
        if (this.keyFile == null)
            return ;
        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        var done = 0;
        try {
            while (pos < length && pos >0) {
                val r = this.keyFile.getRecord(pos);
                if (r.deleted == false) {
                    handler(r.key, reader(DataInputX.toLong5(r.value, 0)))
                }
                done += 1;
                pos = r.next;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S126", this.keyFile.path + " : read=" + done + " pos=" + pos + " file-len=" + length + " " + t);
        }
    }

    def getStat(): Map[String, Number] = {
        var deleted = 0;
        var count = 0;
        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        while (pos < length && pos >0) {
            val r = this.keyFile.getRecord(pos);
            if (r.deleted) {
                deleted += 1;
            } else {
                count += 1;
            }
            pos = r.next;
        }
        val scatter = hashBlock.getCount();

        val out = new HashMap[String, Number]();
        out.put("count", count);
        out.put("scatter", scatter);
        out.put("deleted", deleted);
        out.put("scan", (count + deleted).toFloat / scatter);

        return out;
    }

    override def close() {
        hashBlock.close();
        keyFile.close();
    }

}
