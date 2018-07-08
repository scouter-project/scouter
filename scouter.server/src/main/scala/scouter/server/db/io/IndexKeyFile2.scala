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

import java.io.IOException
import java.util.{ArrayList, HashMap, List, Map}

import scouter.io.DataInputX
import scouter.server.{Configure, Logger}
import scouter.util.{CompareUtil, HashUtil, IClose};

/**
  * IndexKeyFile2 is index file controller and this index file can control TTL.
  */
class IndexKeyFile2(_path: String, hashSize: Int = 1) extends IClose {
    val conf = Configure.getInstance();
    val MB = 1024 * 1024;

    val path = _path
    val hashBlock = new MemHashBlock(path, hashSize * MB)
    val keyFile = new RealKeyFile2(path)

    def put(indexKey: Array[Byte], dataOffset: Array[Byte], ttl: Long): Boolean = {
        if (indexKey == null || dataOffset == null) {
            throw new IOException("invalid key/value");
        }

        val keyHash = HashUtil.hash(indexKey);
        var prevKeyPos = hashBlock.get(keyHash);
        var newKeyPos = this.keyFile.append(prevKeyPos, ttl, indexKey, dataOffset);
        this.hashBlock.put(keyHash, newKeyPos);
        return true;
    }

    def put(indexKey: Array[Byte], dataOffset: Array[Byte]): Boolean = {
        return put(indexKey, dataOffset, -1L)
    }

    /**
      * update or put
      *  - preserve ttl when update, set ttl max when insert.
      */
    def updateOrPut(key: Array[Byte], value: Array[Byte]): Boolean = {
        return updateOrPut(key, value, -1L)
    }

    def updateOrPut(key: Array[Byte], value: Array[Byte], ttl: Long): Boolean = {
        if (key == null || value == null) {
            throw new IOException("invalid key/value");
        }

        val keyHash = HashUtil.hash(key);
        var realKeyPos = hashBlock.get(keyHash);

        try {
            var looping = 0;
            while (realKeyPos > 0) {
                val oKey = this.keyFile.getKey(realKeyPos);
                if (CompareUtil.equals(oKey, key)) {
                    val result = this.keyFile.update(realKeyPos, ttl, key, value);
                    if(!result) {
                        return put(key, value, ttl);
                    }
                }

                realKeyPos = this.keyFile.getPrevPos(realKeyPos);
                looping += 1;
            }
            if(looping > conf.log_index_traversal_warning_count) {
                Logger.println("S161", 10, "[warn] Too many index deep searching. " + new String(key, "UTF8"));
            }
        } catch {
            case e: IOException =>
                Logger.println("S162", "pos=" + realKeyPos + " keyFile.length=" + this.keyFile.getLength());
                throw e;
        }

        return put(key, value, ttl);
    }

    def setTTL(key: Array[Byte], ttl: Long): Boolean = {
        if (key == null) {
            throw new IOException("invalid key/value");
        }

        val keyHash = HashUtil.hash(key);
        var realKeyPos = hashBlock.get(keyHash);

        try {
            var looping = 0;
            while (realKeyPos > 0) {
                val oKey = this.keyFile.getKey(realKeyPos);
                if (CompareUtil.equals(oKey, key)) {
                    if(this.keyFile.isDeletedOrExpired(realKeyPos)) {
                        return false;
                    } else {
                        this.keyFile.setTTL(realKeyPos, ttl);
                        return true;
                    }
                }

                realKeyPos = this.keyFile.getPrevPos(realKeyPos);
                looping += 1;
            }
            if(looping > conf.log_index_traversal_warning_count) {
                Logger.println("S161", 10, "[warn] Too many index deep searching. " + new String(key, "UTF8"));
            }
        } catch {
            case e: IOException =>
                Logger.println("S162", "pos=" + realKeyPos + " keyFile.length=" + this.keyFile.getLength());
                throw e;
        }

        return false;
    }

    def get(key: Array[Byte]): Array[Byte] = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var realKeyPos = hashBlock.get(keyHash);

        try {
            var looping = 0;
            while (realKeyPos > 0) {
                val oKey = this.keyFile.getKey(realKeyPos);
                if (CompareUtil.equals(oKey, key)) {
                    if(this.keyFile.isDeletedOrExpired(realKeyPos)) {
                        return null
                    } else {
                        return this.keyFile.getDataPos(realKeyPos)
                    }
                }

                realKeyPos = this.keyFile.getPrevPos(realKeyPos);
                looping += 1;
            }
            if(looping > conf.log_index_traversal_warning_count) {
                Logger.println("S152", 10, "[warn] Too many index deep searching. " + new String(key, "UTF8"));
            }
        } catch {
            case e: IOException =>
                Logger.println("S124", "pos=" + realKeyPos + " keyFile.length=" + this.keyFile.getLength());
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
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                if (this.keyFile.isDeletedOrExpired(pos)) {
                    return false;
                } else {
                    return true;
                }
            }
            pos = this.keyFile.getPrevPos(pos);
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
                    out.add(this.keyFile.getDataPos(pos));
                }
            }
            pos = this.keyFile.getPrevPos(pos);
        }
        return out;
    }

    def delete(key: Array[Byte]): Int = {
        if (key == null) {
            throw new IOException("invalid key");
        }
        val keyHash = HashUtil.hash(key);
        var pos = hashBlock.get(keyHash);

        while (pos > 0) {
            val okey = this.keyFile.getKey(pos);
            if (CompareUtil.equals(okey, key)) {
                if(this.keyFile.isDeleted(pos)) {
                    return 0;
                } else {
                    this.keyFile.setDelete(pos, true);
                    return 1;
                }
            }
            pos = this.keyFile.getPrevPos(pos);
        }
        return 0;
    }

    //TODO not yet implemented
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
                    handler(r.key, r.dataPos)
                }
                done += 1;
                pos = r.offset;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S125", this.keyFile.path + " : read=" + done + " pos=" + pos + " file-len=" + length + " " + t);
        }
    }

    //TODO not yet implemented
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
                    handler(r.key, reader(DataInputX.toLong5(r.dataPos, 0)))
                }
                done += 1;
                pos = r.offset;
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
            pos = r.offset;
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
