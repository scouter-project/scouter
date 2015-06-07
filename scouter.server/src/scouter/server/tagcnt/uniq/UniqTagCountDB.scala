/*
 *  Copyright 2015 LG CNS.
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
package scouter.server.tagcnt.uniq;

import java.io.File
import java.util.ArrayList
import java.util.Enumeration
import java.util.List
import scouter.server.Logger
import scouter.server.core.CoreRun
import scouter.server.tagcnt.core.CountEnv
import scouter.server.tagcnt.core.DBKey
import scouter.util.BackJob
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.IntSet
import scouter.util.LinkedMap
import scouter.util.RequestQueue
import scouter.util.ThreadUtil
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala

object UniqTagCountDB extends IClose {

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

    };
    BackJob.getInstance().add("UCountingTB", 10000, r);

    class Data(_datetime: Long, _objType: String, _tagKey: Long, _inx: Int, _uniqCount: Int) {
        val datetime = _datetime;
        val objType = _objType;
        val tagKey = _tagKey;
        val inx = _inx;
        val uniqCount = _uniqCount;
    }

    private val queue = new RequestQueue[Data](CountEnv.MAX_QUE_SIZE + 1);
    def getQueueSize() = queue.size()

    def add(datetime: Long, objType: String, tagKey: Long, inx: Int, ucnt: Int) {
        if (ucnt <= 0)
            return ;
        while (isQueueOk() == false) {
            ThreadUtil.qWait();
        }
        queue.put(new Data(datetime, objType, tagKey, inx, ucnt));
    }

    def isQueueOk() = (queue.size() < CoreRun.MAX_QUE_SIZE)

    val ignoreTag = new IntSet();
    var lastflush = System.currentTimeMillis();

    ThreadScala.start("scouter.server.tagcnt.uniq.UniqTagCountDB") {

        while (CountEnv.running) {
            closeIdleConnections();
            val p = queue.get();
            val db = openWrite(p.datetime, p.objType);
            try {
                if (db != null) {
                    db.table.add(p.tagKey, p.inx, p.uniqCount);
                }
            } catch {
                case t: Throwable => t.printStackTrace();
            }
        }
        FileUtil.close(this);
    }

    private def openWrite(time: Long, objType: String): WorkDB = {
        this.synchronized {
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
                        case t: Throwable => t.printStackTrace();
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

        EnumerScala.foreach(idles.iterator(), (dbk: DBKey) => {
            val o = database.remove(dbk);
            FileUtil.close(o);
        })
    }

    def open(date: String, objType: String): WorkDB = {
        try {
            val path = CountEnv.getDBPath(date, objType);
            val f = new File(path);
            if (f.exists() == false)
                f.mkdirs();

            val db = new WorkDB(path);
            db.open();
            db.objType = objType;
            db.logDate = date;
            return db;

        } catch {
            case e: Exception =>
                e.printStackTrace();
        }
        return null
    }

    def getUCount(objType: String, date: String, tagKey: Long): Array[Int] = {
        val db = open(date, objType);
        try {
            return db.table.get(tagKey);
        } catch {
            case e: Exception =>
                e.printStackTrace();
        } finally {
            FileUtil.close(db);
        }
        return null;
    }

    def read(objType: String, date: String, handler: (Long, Array[Int]) => Any) {
        val db = open(date, objType);
        try {
            db.table.read(handler);
        } catch {
            case e: Throwable =>
                e.printStackTrace();
        } finally {
            FileUtil.close(db);
        }
    }

    override def close() {
        database.synchronized {
            while (database.size() > 0) {
                database.removeFirst().close();
            }
        }
    }
}
