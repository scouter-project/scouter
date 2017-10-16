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

import java.io.File
import java.util.ArrayList
import java.util.HashMap

import scouter.lang.value.Value
import scouter.server.Logger
import scouter.server.tagcnt.core.CountEnv
import scouter.server.tagcnt.core.DBKey
import scouter.server.tagcnt.core.NextTagCountData
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala
import scouter.util.BackJob
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.LinkedMap
import scouter.util.RequestQueue
import scouter.util.ThreadUtil

object NextTagCountDB extends IClose {

    val database = new LinkedMap[DBKey, WorkDB]();
    var idleConns = new ArrayList[DBKey]();

    val r = new Runnable() {
        override def run() {
            val now = System.currentTimeMillis();
            EnumerScala.foreach(database.keys(), (key: DBKey) => {

                val db = database.get(key);
                if (db != null) {
                    if (now - db.lastActive > DateUtil.MILLIS_PER_FIVE_MINUTE) {
                        idleConns.add(key);
                    }
                }
            })
        }
    }

    BackJob.getInstance().add("BG-NextTagCountDB", 10000, r);

    val queue = new RequestQueue[HashMap[NextTagCountData, Array[Float]]](CountEnv.MAX_QUE_SIZE + 1);

    def getQueueSize() = queue.size();

    def isQueueOk() = queue.size() < CountEnv.MAX_QUE_SIZE

    def add(data: HashMap[NextTagCountData, Array[Float]]) {
        while (isQueueOk() == false) {
            ThreadUtil.qWait();
            Logger.println("S185", 10, "NextTagCountDB queue is exceeded");
        }
        queue.put(data);
    }

    val lastflush = System.currentTimeMillis();

    ThreadScala.start("scouter.server.tagcnt.next.NextTagCountDB") {

        while (CountEnv.running) {
            closeIdleConnections();
            val p = queue.get();
            process(p);
        }
        FileUtil.close(this);

    }

    def process(p: HashMap[NextTagCountData, Array[Float]]) {
        EnumerScala.foreach(p.keySet().iterator(), (key: NextTagCountData) => {
            val value = p.get(key);
            val db = openWrite(key.time, key.objType);
            try {
                if (db != null) {
                    db.table.add(key.tagKey, key.value, (key.hourUnit % 24).toInt, value);
                }
            } catch {
                case t: Throwable =>
                    t.printStackTrace();
            }
        })
    }

    private def openWrite(time: Long, objType: String): WorkDB = {
        this.synchronized {
            val dateunit = DateUtil.getDateUnit(time);
            var db = database.get(new DBKey(dateunit, objType));
            if (db != null) {
                db.lastActive = System.currentTimeMillis();
                return db;
            }
            db = open(time, objType);
            if (db == null)
                return null;

            db.lastActive = System.currentTimeMillis();
            database.synchronized {
                while (database.size() >= CountEnv.MAX_ACTIVEDB) {
                    try {
                        database.removeFirst().close();
                    } catch {
                        case _: Throwable =>
                    }
                }
                database.put(new DBKey(dateunit, objType), db);
            }
            return db;
        }
    }

    private def closeIdleConnections() {
        if (idleConns.isEmpty())
            return ;
        val idles = idleConns;
        idleConns = new ArrayList[DBKey]();

        for (i <- 0 to idles.size() - 1) {
            val o = database.remove(idles.get(i));
            FileUtil.close(o);
        }
    }

    def open(date: String, objType: String): WorkDB = {
        val time = DateUtil.getTime(date, "yyyyMMdd");
        return open(time, objType);
    }

    def open(time: Long, objType: String): WorkDB = {
        this.synchronized {
            if (time == 0) {
                new Throwable().printStackTrace();
            }
            var db: WorkDB = null;
            try {
                val date = DateUtil.yyyymmdd(time);

                val path = CountEnv.getDBPath(date, objType);
                val f = new File(path);
                if (f.exists() == false)
                    f.mkdirs();

                db = new WorkDB(path);
                db.open();
                db.objType = objType;
                db.logDate = date;

            } catch {
                case e: Exception =>
                    e.printStackTrace();
            }
            return db;
        }
    }

    def getTagValueCount(date: String, objType: String, tagKey: Long, value: Value): Array[Float] = {
        val db = open(date, objType);
        try {
            return db.table.get(tagKey, value);
        } catch {
            case e: Exception =>
                e.printStackTrace();
        } finally {
            FileUtil.close(db);
        }
        return null;
    }

    // def read(date: String, objType: String, handler: (Long,Value,Int,Array[Long],IndexFile,Long)=>Any) {
    def read(date: String, objType: String, handler: (Long, Value, Float, Array[Long], IndexFile, Long) => Any) {
        val db = open(date, objType);
        try {
            db.table.read(handler);
        } catch {
            case e: Throwable =>
        } finally {
            FileUtil.close(db);
        }
    }

    def close() {
        database.synchronized {
            while (database.size() > 0) {
                database.removeFirst().close();
            }
        }
    }
}
