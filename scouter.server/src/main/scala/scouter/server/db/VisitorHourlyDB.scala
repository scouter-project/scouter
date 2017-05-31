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

import scouter.lang.value.DecimalValue
import scouter.lang.value.ListValue
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala
import scouter.server.util.cardinality.HyperLogLog
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.Hexa32
import scouter.util.IntKeyLinkedMap
import scouter.util.ThreadUtil

object VisitorHourlyDB {
    val rsd = 20

    //Hourly Execute
    ThreadScala.startDaemon("scouter.server.db.VisitorHourlyDB") {
        var hourUnit = DateUtil.getHour(System.currentTimeMillis())
        while (DBCtr.running) {
            var currentHour = DateUtil.getHour(System.currentTimeMillis())
            if (hourUnit != currentHour) {
                hourUnit = currentHour
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
                        save(getFileName(Hexa32.toString32(h)), hhl)
                    }
                } catch {
                    case _: Throwable =>
                }
            })
        }
    }

    val objHashHyperTable = new IntKeyLinkedMap[HyperLogLog].setMax(500);

    def getNewObject(objHash: Int): HyperLogLog = {
        var h = objHashHyperTable.get(objHash)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), getFileName(Hexa32.toString32(objHash)))
            if (h == null) {
                h = new HyperLogLog(rsd)
            }
            objHashHyperTable.put(objHash, h)
        }
        h.dirty = true
        return h
    }
    
    private def load(date: String, name: String): HyperLogLog = {
      return load(date, System.currentTimeMillis(), name)
    }

    private def load(date: String, time: Long, name: String): HyperLogLog = {
        val path = getDBPath(date);
        val f = new File(path);
        if (f.exists() == false)
            f.mkdirs();
        val file = new File(path + "/" + getFileName(name, time) + ".usr");
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
        sb.append("/").append(date).append("/visit_hourly");
        return sb.toString();
    }
    
    private def getFileName(name : String) : String = {
      return getFileName(name, System.currentTimeMillis())
    }
    
    private def getFileName(name : String, time : Long) : String = {
      return name + "_" + DateUtil.getHour(time)
    }

    def getVisitorObject(objHash: Int): Long = {
        var h = objHashHyperTable.get(objHash)
        if (h == null) {
            h = load(DateUtil.yyyymmdd(), Hexa32.toString32(objHash))
        }
        if (h == null) 0 else h.cardinality()
    }
    
    def getVisitorObject(date: String, time: Long, objHash: Int): Long = {
        val h = load(date, time, Hexa32.toString32(objHash))
        if (h == null) 0 else h.cardinality()
    }
    
    def getMergedVisitorObject(date: String, time: Long, objHashLv: ListValue): Long = {
        var cardinality = 0L;
        val totalVisitor = new HyperLogLog(rsd)
        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
          val h = load(date, time, Hexa32.toString32(obj.intValue()))
          if (h != null) totalVisitor.addAll(h)
        })
        totalVisitor.cardinality()
    }
}
