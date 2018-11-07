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

/**
  * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
  */
package scouter.server.db.span

import java.util

import scouter.io.{DataInputX, DataOutputX}
import scouter.server.db.io.{IndexKeyFile, IndexTimeFile}
import scouter.server.util.EnumerScala
import scouter.util.{FileUtil, IClose}

object ZipkinSpanIndex {
    val POSTFIX_TIME = "_tim"
    val POSTFIX_GID = "_gid"
    val POSTFIX_TID = "_tid"

    val table = new util.Hashtable[String, ZipkinSpanIndex]()

    def open(file: String): ZipkinSpanIndex = {
        table.synchronized {
            var index = table.get(file)
            if (index != null) {
                index.ref += 1
            } else {
                index = new ZipkinSpanIndex(file)
                table.put(file, index)
            }
            index
        }
    }
}

class ZipkinSpanIndex(val filePathName: String) extends IClose {

    var ref = 0
    var gxidIndex: IndexKeyFile = _
    var timeIndex: IndexTimeFile = _

    def setByGxid(gxid: Long, pos: Long): Unit = {
        if (gxid == 0)
            return ;
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(filePathName + ZipkinSpanIndex.POSTFIX_GID)
        }
        this.gxidIndex.put(DataOutputX.toBytes(gxid), DataOutputX.toBytes5(pos))
    }

    def setByTime(time: Long, pos: Long): Unit = {
        if (this.timeIndex == null) {
            this.timeIndex = new IndexTimeFile(filePathName + ZipkinSpanIndex.POSTFIX_TIME)
        }
        this.timeIndex.put(time, DataOutputX.toBytes5(pos))
    }

    def getByGxid(gxid: Long): util.List[Long] = {
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(filePathName + ZipkinSpanIndex.POSTFIX_GID)
        }
        val blist = this.gxidIndex.getAll(DataOutputX.toBytes(gxid))
        val olist = new util.ArrayList[Long]()
        EnumerScala.foreach(blist.iterator(), (bb: Array[Byte]) => {
            olist.add(DataInputX.toLong5(bb, 0))
        })

        olist
    }

    def readByGxid(handler: (Array[Byte], Array[Byte]) => Any, dr: (Long) => Array[Byte]): Unit = {
        if (this.gxidIndex == null) {
            this.gxidIndex = new IndexKeyFile(filePathName + ZipkinSpanIndex.POSTFIX_GID)
        }
        this.gxidIndex.read(handler, dr)
    }

    override def close(): Unit = {
        ZipkinSpanIndex.table.synchronized {
            if (this.ref == 0) {
                ZipkinSpanIndex.table.remove(this.filePathName)
                FileUtil.close(this.gxidIndex)
                FileUtil.close(this.timeIndex)
            } else {
                this.ref -= 1;
            }
        }
    }

}