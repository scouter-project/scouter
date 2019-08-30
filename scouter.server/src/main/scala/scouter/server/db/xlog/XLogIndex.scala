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

package scouter.server.db.xlog;

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Hashtable
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set

import scouter.server.db.io.IndexKeyFile
import scouter.server.db.io.IndexTimeFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.Configure
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.server.util.EnumerScala

object XLogIndex {
    val POSTFIX_TIME = "_tim";
    val POSTFIX_GID = "_gid";
    val POSTFIX_TID = "_tid";

    val table = new Hashtable[String, XLogIndex]();

    def open(file: String): XLogIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new XLogIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}

class XLogIndex(_file: String) extends IClose {
    val ID_INDEX_MB = Configure.getInstance()._mgr_xlog_id_index_mb

    val file = _file
    var refrence = 0
    var txidIndex: IndexKeyFile = null
    var gxidIndex: IndexKeyFile = null
    var timeIndex: IndexTimeFile = null

    def setByTxid(txid: Long, pos: Long) {
        if (this.txidIndex == null) {
            this.txidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_TID, ID_INDEX_MB);
        }
        this.txidIndex.put(DataOutputX.toBytes(txid), DataOutputX.toBytes5(pos));
    }

    def setByGxid(gxid: Long, pos: Long) {
        if (gxid == 0)
            return ;
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_GID, ID_INDEX_MB);
        }
        this.gxidIndex.put(DataOutputX.toBytes(gxid), DataOutputX.toBytes5(pos));
    }

    def setByTime(time: Long, pos: Long) {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(file + XLogIndex.POSTFIX_TIME);
        }
        this.timeIndex.put(time, DataOutputX.toBytes5(pos));
    }

    def getByTxid(txid: Long): Long = {
        if (this.txidIndex == null) {
            this.txidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_TID);
        }
        val b = this.txidIndex.get(DataOutputX.toBytes(txid));
        if (b == null) -1 else DataInputX.toLong5(b, 0);
    }

    def getByTxid(txSet: Set[Long]): Map[Long, Long] = {
        if (this.txidIndex == null) {
            this.txidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_TID);
        }
        val map = new HashMap[Long, Long]();
        EnumerScala.foreach(txSet.iterator(), (key: Long) => {
            val value = this.txidIndex.get(DataOutputX.toBytes(key));
            if (value != null) {
                map.put(key, DataInputX.toLong5(value, 0));
            }
        })
        return map;
    }

    def getByGxid(gxid: Long): List[Long] = {
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_GID);
        }
        val blist = this.gxidIndex.getAll(DataOutputX.toBytes(gxid));
        val olist = new ArrayList[Long]();
        EnumerScala.foreach(blist.iterator(), (bb: Array[Byte]) => {
            olist.add(DataInputX.toLong5(bb, 0))
        })

        return olist;
    }

    def readByTxid(handler: (Array[Byte], Array[Byte]) => Any, dr: (Long)=>Array[Byte]) {
        if (this.txidIndex == null) {
            this.txidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_TID);
        }
        this.txidIndex.read(handler, dr);
    }

    def readByGxid(handler: (Array[Byte], Array[Byte]) => Any, dr: (Long)=>Array[Byte]) {
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(file + XLogIndex.POSTFIX_GID);
        }
        this.gxidIndex.read(handler, dr);
    }

    override def close() {
        XLogIndex.table.synchronized {
            if (this.refrence == 0) {
                XLogIndex.table.remove(this.file);
                FileUtil.close(this.txidIndex);
                FileUtil.close(this.gxidIndex);
                FileUtil.close(this.timeIndex);
            } else {
                this.refrence -= 1;
            }
        }
    }

}