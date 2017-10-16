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

package scouter.server.db;

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Iterator
import java.util.List
import java.util.Map
import scouter.lang.TextTypes
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.ShutdownManager
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.RequestQueue
import scouter.util.HashUtil
import scouter.util.IClose
import scouter.util.IShutdown
import scouter.util.ThreadUtil
import scouter.server.db.obj.ObjectIndex
import scouter.server.db.obj.ObjectData
import scouter.server.util.ThreadScala

object ObjectWR {
    val BLOCK_SIZE = 4096;

    val queue = new RequestQueue[Order](DBCtr.MAX_QUE_SIZE);

    ThreadScala.start("scouter.server.db.ObjectWR") {
        while (DBCtr.running) {
            val ord = queue.get();
            try {
                val dateunit = DateUtil.getDateUnit(System.currentTimeMillis());
                if (curDbw == null) {
                    curDbw = open(DateUtil.yyyymmdd());
                } else if (curDbw.dateunit != dateunit) {
                    curDbw.close();
                    curDbw = open(DateUtil.yyyymmdd());
                }
                curDbw.dateunit = dateunit;

                if (ord.add) {
                    val p = ord.pack;

                    var location = curDbw.index.get(p.objHash);
                    if (location < 0) {
                        location = curDbw.writer.write(toBytes(p));
                        curDbw.index.set(p.objHash, location);
                    } else {
                        curDbw.writer.update(location, toBytes(p));
                    }
                }
                if (ord.delete) {
                    var location = curDbw.index.get(ord.hash);
                    if (location >= 0) {
                        curDbw.index.delete(ord.hash);
                    }
                }
            } catch {
                case t: Throwable => t.printStackTrace();
            }
        }
        close();
    }

    class Order {
        var add: Boolean = true
        var pack: ObjectPack = null
        var delete: Boolean = false
        var hash: Int = 0
    }

    def createAdd(pack: ObjectPack): Order = {
        val o = new Order();
        o.add = true;
        o.pack = pack;
        return o;
    }

    def createRm(hash: Int): Order = {
        val o = new Order();
        o.delete = true;
        o.hash = hash;
        return o;
    }

    def add(obj: ObjectPack) {
        val ok = queue.put(createAdd(obj));
        if (ok == false) {
            Logger.println("S131", 10, "queue exceeded!!");
        }
    }
    def remove(objHash: Int) {
        val ok = queue.put(createRm(objHash));
        if (ok == false) {
            Logger.println("S132", 10, "queue exceeded!!");
        }
    }

    def toBytes(m: ObjectPack): Array[Byte] = {
        val b = new DataOutputX().writePack(m).toByteArray();
        val buff = new Array[Byte](BLOCK_SIZE);
        System.arraycopy(b, 0, buff, 0, b.length);
        return buff;
    }

    var curDbw: DBW = null

    class DBW extends IClose {
        var dateunit: Long = 0
        var index: ObjectIndex = null
        var writer: ObjectData = null

        override def close() {
            FileUtil.close(index);
            FileUtil.close(writer);
        }
    }

    var dbs = new HashMap[String, DBW]();

    def close() {
        val _dbs = dbs
        dbs = new HashMap[String, DBW]();
        val itr = _dbs.keySet().iterator();
        while (itr.hasNext()) {
            val key = itr.next();
            FileUtil.close(_dbs.get(key));
        }
    }

    def open(date: String): DBW = {
        try {
            val path = getDBPath(date);
            val f = new File(path);
            if (f.exists() == false)
                f.mkdirs();
            val file = path + "/obj";
            val dbw = new DBW();
            dbw.index = ObjectIndex.open(file);
            dbw.writer = ObjectData.open(file);
            return dbw;
        } catch {
            case e: Throwable => {
                e.printStackTrace();
                close();
            }
        }
        return null;
    }

    def getDBPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/" + date + "/object");
        return sb.toString();
    }

}
