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
import java.util.Hashtable
import java.util.List
import scouter.server.db.io.IndexKeyFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.server.util.EnumerScala
import java.util.Arrays

object XLogProfileIndex {
    val table = new Hashtable[String, XLogProfileIndex]();

    def open(file: String): XLogProfileIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new XLogProfileIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}

class XLogProfileIndex(_file: String) extends IClose {

    val POSTFIX_PROFILE = "_profile";

    val file = _file
    var refrence = 0;
    var profileX: IndexKeyFile = null

    override def close() {
        XLogProfileIndex.table.synchronized {
            if (this.refrence == 0) {
                XLogProfileIndex.table.remove(this.file);
                if (this.profileX != null) {
                    FileUtil.close(this.profileX);
                    this.profileX = null;
                }
            } else {
                this.refrence -= 1;
            }
        }
    }

    def addByTxid(txid: Long, offset: Long) {
        checkOpen();
        this.profileX.put(DataOutputX.toBytes(txid), DataOutputX.toBytes5(offset));
    }

    private def checkOpen() {
        if (this.profileX == null) {
            this.profileX = new IndexKeyFile(file + POSTFIX_PROFILE);
        }
    }

    def getByTxid(txid: Long): List[Long] = {
        checkOpen();
        val blist = this.profileX.getAll(DataOutputX.toBytes(txid));
        val olist = new Array[Long](blist.size());
        var cnt = 0;
        EnumerScala.foreach(blist.iterator(), (bb: Array[Byte]) => {
        	olist(cnt)=DataInputX.toLong5(bb, 0);
        	cnt+=1;
        })
        Arrays.sort(olist);
        val list = new ArrayList[Long](olist.length);
        EnumerScala.foreach(olist, (ll: Long) => {
          list.add(ll);
        })
        return list;
    }
    
    def read(handler: (Array[Byte], Array[Byte]) => Unit, dr: (Long)=>Array[Byte]) {
        checkOpen();
        this.profileX.read(handler, dr);
    }

}