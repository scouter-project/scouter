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

import scouter.lang.pack.XLogPack
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
import scouter.util.StringKeyLinkedMap
import scouter.server.util.cardinality.HyperLogLog
import scouter.util.DateUtil
import scouter.util.ThreadUtil
import scouter.util.IntKeyLinkedMap
import java.io.File
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.Hexa32
import scouter.server.util.EnumerScala
import scouter.server.core.CoreRun
import scouter.lang.value.ListValue
import scouter.lang.value.DecimalValue

object VisitorDB {
    val rsd = 20

    //Daily Execute
    ThreadScala.startDaemon("scouter.server.db.VisitorDB") {
        var dateUnit = DateUtil.getDateUnit()
        while (DBCtr.running) {
            if (dateUnit != DateUtil.getDateUnit()) {
                dateUnit = DateUtil.getDateUnit()
                objTypeHyperTable.clear()
                objHashHyperTable.clear()
            }
            flush()
            ThreadUtil.sleep(1000)
        }
    }
    private var lastFlush = System.currentTimeMillis()
    private def flush() {
        val now = System.currentTimeMillis()
        if (now - lastFlush >= 10000) {
            lastFlush = now
            EnumerScala.foreach(objHashHyperTable.keys(), (h: Int) => {
                try {
                    val hhl = objHashHyperTable.get(h)
                    if (hhl != null && hhl.dirty == true) {
                        hhl.dirty = false
                        save(Hexa32.toString32(h), hhl)
                    }
                } catch {
                    case _: Throwable =>
                }
            })
            EnumerScala.foreach(objTypeHyperTable.keys(), (o: String) => {
                try {
                    val hhl = objTypeHyperTable.get(o)
                    if (hhl != null && hhl.dirty == true) {
                        hhl.dirty = false
                        save(o, hhl)
                    }
                } catch {
                    case _: Throwable =>
                }
            })
        }
    }

    val objHashHyperTable = new IntKeyLinkedMap[HyperLogLog].setMax(500);
    val objTypeHyperTable = new StringKeyLinkedMap[HyperLogLog].setMax(100);

    def getNewObjType(objType: String): HyperLogLog = {
        var h = objTypeHyperTable.get(objType)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), objType)
            if (h == null) {
                h = new HyperLogLog(rsd)
            }
            objTypeHyperTable.put(objType, h)
        }
        h.dirty = true
        return h
    }

    def getNewObject(objHash: Int): HyperLogLog = {
        var h = objHashHyperTable.get(objHash)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), Hexa32.toString32(objHash))
            if (h == null) {
                h = new HyperLogLog(rsd)
            }
            objHashHyperTable.put(objHash, h)
        }
        h.dirty = true
        return h
    }

    private def load(date: String, name: String): HyperLogLog = {
        val path = getDBPath(date);
        val f = new File(path);
        if (f.exists() == false)
            f.mkdirs();
        val file = new File(path + "/" + name + ".usr");
        if (file.exists()) {
            val bytes = FileUtil.readAll(file)
            HyperLogLog.build(bytes)
        } else {
            null
        }
    }
    private def save(name: String, hll: HyperLogLog) {
        val path = getDBPath(DateUtil.yyyymmdd());
        val f = new File(path);
        if (f.exists() == false)
            f.mkdirs();
        val file = new File(path + "/" + name + ".usr");
        FileUtil.save(file, hll.getBytes())
    }
    private def getDBPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date).append("/visit");
        return sb.toString();
    }

    def getVisitorObjType(objType: String): Long = {
        var h = objTypeHyperTable.get(objType)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), objType)
        }
        if (h == null) 0 else h.cardinality()
    }

    def getVisitorObjType(date: String, objType: String): Long = {
        val h = load(date, objType)
        if (h == null) 0 else h.cardinality()
    }
    def getVisitorObject(objHash: Int): Long = {
        var h = objHashHyperTable.get(objHash)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), Hexa32.toString32(objHash))
        }
        if (h == null) 0 else h.cardinality()
    }
    def getVisitorObject(date: String, objHash: Int): Long = {
        val h = load(date, Hexa32.toString32(objHash))
        if (h == null) 0 else h.cardinality()
    }
    
    def getMergedVisitorObject(objHashLv: ListValue): Long = {
        val totalVisitor = new HyperLogLog(rsd)
        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
          var h = objHashHyperTable.get(obj.intValue())
          if (h == null) {
              h = load(DateUtil.yyyymmdd(), Hexa32.toString32(obj.intValue()))
          }
          if (h != null) totalVisitor.addAll(h)
        })
        totalVisitor.cardinality()
    }
    
    def getMergedVisitorObject(date: String, objHashLv: ListValue): Long = {
        val totalVisitor = new HyperLogLog(rsd)
        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
          val h = load(date, Hexa32.toString32(obj.intValue()))
          if (h != null) totalVisitor.addAll(h)
        })
        totalVisitor.cardinality()
    }
}
