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

import java.io.File
import java.util.ArrayList
import java.util.Enumeration
import java.util.HashSet
import java.util.List
import java.util.Set
import scouter.lang.value.Value
import scouter.server.Logger
import scouter.server.tagcnt.core.CountEnv
import scouter.server.tagcnt.core.DBKey
import scouter.server.tagcnt.core.MoveToNextCollector
import scouter.server.tagcnt.core.TagCountUtil
import scouter.server.tagcnt.core.Top100FileCache
import scouter.util.BackJob
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.IClose
import scouter.util.LinkedMap
import scouter.util.LongKeyLinkedMap
import scouter.util.LongKeyMap
import scouter.util.RequestQueue
import scouter.util.ThreadUtil
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala

object FirstTagCountDB extends IClose {

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
    };
    BackJob.getInstance().add("BG-FirstTagCountDB", 10000, r);

    val MAX_QUE_SIZE = 50000
    private val queue = new RequestQueue[FirstTCData](MAX_QUE_SIZE + 1);

    def add(data: FirstTCData) {
        while (queue.size() >= MAX_QUE_SIZE) {
            ThreadUtil.qWait();
            Logger.println("S183", 10, "FirstTagCountDB queue is exceeded");
        }
        queue.put(data);
    }
    var lastflush = System.currentTimeMillis();

    ThreadScala.startDaemon("scouter.server.tagcnt.first.FirstTagCountDB") {
        while (CountEnv.running) {
            closeIdleConnections();
            val now = System.currentTimeMillis();
            if (now > lastflush + 5000) {
                flush();
            }
            val data = queue.get();

            val db = openWrite(data.time, data.objType);
            if (db != null) {
                try {
                    val hhmm = TagCountUtil.hhmm(data.time);
                    val ok = countingFirst100(db, data.tagKey, data.tagValue, hhmm, data.cnt);
                    if (ok == false) {
                        MoveToNextCollector.add(data.time, data.objType, data.tagKey, data.tagValue, data.cnt);
                    }
                    Top100FileCache.add(db.logDate, data.objType);
                } catch {
                    case t: Throwable =>
                        t.printStackTrace();
                }
            }
        }
        FileUtil.close(this);
    }

    private val writeLock = new Object();

    /**
     * 새로 테그/값에 따른 1440의 갯수정보를 추가하면 데이터베이스에 저장한다.
     */
    def updateNewCounting(date: String, objType: String, tagKey: Long, tagValue: Value, count: Array[Float]) {
        TagCountUtil.check(count);
        var db: WorkDB = null;
        try {
            writeLock.synchronized {
                db = openWrite(DateUtil.getTime(date, "yyyyMMdd"), objType);
                db.table.update(tagKey, tagValue, count);
                db.entry.push(tagKey, tagValue);
            }
        } catch {
            case e: Exception =>
                e.printStackTrace();
        }
        // update db는 close하지 않는다.
    }

    private def countingFirst100(db: WorkDB, tagKey: Long, tagValue: Value, hhmm: Int, n: Float): Boolean = {
        return db.entry.add(tagKey, tagValue, hhmm, n);
    }

    private def flush() {
        lastflush = System.currentTimeMillis();
        writeLock.synchronized {
            val en = database.values();
            while (en.hasMoreElements()) {
                val ix = en.nextElement();
                ix.entry.save();
            }
        }
    }

    private def openWrite(time: Long, objType: String): WorkDB = {
        val dateunit = DateUtil.getDateUnit(time);
        var db = database.get(new DBKey(dateunit, objType));
        if (db != null) {
            db.lastActive = System.currentTimeMillis();
            return db;
        }

        db = open(DateUtil.yyyymmdd(time), objType);
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

    private val database = new LinkedMap[DBKey, WorkDB]();
    private var idleConns = new ArrayList[DBKey]();

    private def closeIdleConnections() {
        if (idleConns.isEmpty())
            return ;
        val idles = idleConns;
        idleConns = new ArrayList[DBKey]();

        var inx = 0
        while (inx < idles.size()) {
            val o = database.remove(idles.get(inx));
            FileUtil.close(o);
            inx += 1
        }
    }

    def open(date: String, objType: String): WorkDB = {
        var db: WorkDB = null;
        try {
            val path = CountEnv.getDBPath(date, objType);
            val f = new File(path);
            if (f.exists() == false)
                f.mkdirs();

            db = new WorkDB(path);
            db.open();
            db.objType = objType;
            db.logDate = date;

        } catch {
            case e: Throwable => e.printStackTrace();
        }
        return db;
    }

    def getTagValueCount(objType: String, date: String, tagKey: Long, tagValue: Value): Array[Float] = {
        val db = open(date, objType);
        try {
            return db.table.get(tagKey, tagValue);
        } catch {
            case e: Throwable => e.printStackTrace();
        } finally {
            FileUtil.close(db);
        }
        return null;
    }

    def getTagValues(objType: String, date: String): LongKeyMap[Set[Value]] = {
        var db = open(date, objType);
        try {
            if (db == null)
                return new LongKeyMap[Set[Value]]();
            else
                return db.entry.getTagValueSet();
        } finally {
            FileUtil.close(db);
        }
    }

    def getTagValues(objType: String, date: String, tagKey: Long): Set[Value] = {
        val db = open(date, objType);
        try {
            if (db == null)
                return new HashSet[Value]();
            else
                return db.entry.getTagValueSet().get(tagKey);
        } finally {
            FileUtil.close(db);
        }
    }

    def read(date: String, objType: String, handler: (Array[Byte], Array[Int]) => Any) {
        val db = open(date, objType);
        try {
            db.table.read(handler);
        } catch {
            case e: Throwable => e.printStackTrace();
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
