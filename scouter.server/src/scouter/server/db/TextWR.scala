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
import scouter.util.StringUtil
import scouter.server.Logger
import scouter.server.ShutdownManager
import scouter.server.core.cache.TextCache
import scouter.server.db.text.TextTable
import scouter.util.FileUtil
import scouter.util.RequestQueue
import scouter.util.HashUtil
import scouter.util.IClose
import scouter.util.IShutdown
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scouter.util.LinkedMap
import scouter.util.ICloseDB
import scouter.util.DateUtil
import java.util.ArrayList
import scala.collection.mutable.ListBuffer
import scouter.server.core.CoreRun
import scouter.server.util.EnumerScala
import scouter.server.core.ServerStat

object TextWR {

    protected val database = new LinkedMap[String, TextTable]();

    protected var idleConns = new ArrayList[String]();

    // executed every 10sec
    ThreadScala.start("scouter.server.db.TextWR", { CoreRun.running }, 10000) {
        val now = System.currentTimeMillis();
        val en = database.keys();
        while (en.hasMoreElements()) {
            val key = en.nextElement();
            val db = database.get(key);
            if (db != null) {
                if (now - db.getLastActive() > DateUtil.MILLIS_PER_FIVE_MINUTE) {
                    idleConns.add(key)
                }
            }
        }
    }
    protected def closeIdle() {
        if (idleConns.size() == 0)
            return ;
        val x = idleConns
        idleConns = new ArrayList[String]();
        EnumerScala.foreach(x.iterator(), (id: String) => { FileUtil.close(database.remove(id)) })
    }

    protected def getTable(date: String): TextTable = {
        val table = database.get(date);
        if (table != null) {
            table.setActive(System.currentTimeMillis());
        }
        return table;
    }

    protected def putTable(date: String, table: TextTable) {
        database.put(date, table);
    }

    val queue = new RequestQueue[Data](DBCtr.LARGE_MAX_QUE_SIZE);
    ThreadScala.start("scouter.server.db.TextWR-2") {
        while (DBCtr.running) {
            closeIdle();
            val m = queue.get(10000); //check 10 sec
            ServerStat.put("text.db.queue",queue.size());
            
            if (m != null) {
                try {
                    process(m);
                } catch {
                    case t: Throwable => t.printStackTrace();
                }
            }
        }
        close();
    }
    def process(m: Data) {
        if (TextPermWR.isA(m.div)) {
            //성능:중복입력을 막아야한다.
            TextDupCheck.addDuplicated(m.div, m.textUnit);
            TextPermWR.add(m.div, m.hash, m.text);
        } else {
            val writingTable = open(m.date);
            if (writingTable == null) {
                queue.clear();
                Logger.println("S139", 10, "can't open db");
            } else {
                val ok = writingTable.hasKey(m.div, m.hash);
                if (ok == false) {
                    TextDupCheck.addDuplicated(m.div, m.textUnit);
                    writingTable.set(m.div, m.hash, m.text.getBytes("UTF8"));
                }
            }
        }
    }
    def add(date: String, div: String, hash: Int, text: String): Unit = {
       
        TextCache.put(div, hash, text);

        val tu = new TextDupCheck.TextUnit(date, hash);
        if (TextDupCheck.isDuplicated(div, tu))
            return;

        val ok = queue.put(new Data(date, div, hash, text, tu));
        if (ok == false) {
            Logger.println("S140", 10, "queue exceeded!!");
        }
    }

    class Data(_date: String, _div: String, _hash: Int, _text: String, _tu: TextDupCheck.TextUnit) {
        val date = _date
        val div = _div
        val hash = _hash
        val text = _text
        var textUnit = _tu
    }

    def open(date: String): TextTable = {
        var table = getTable(date)
        if (table != null)
            return table;
        try {
            this.synchronized {
                table = getTable(date)
                if (table != null)
                    return table;

                val path = getDBPath(date);
                val f = new File(path);
                if (f.exists() == false)
                    f.mkdirs();
                val file = path + "/text";
                table = TextTable.open(file);
                putTable(date, table);
                return table;
            }
        } catch {
            case e: Throwable =>
                e.printStackTrace();
                close();
        }
        return null;
    }

    def getDBPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/" + date + "/text");
        return sb.toString();
    }

    def close() {
        database.synchronized {
            while (database.size() > 0) {
                database.removeFirst().close();
            }
        }
    }
}
