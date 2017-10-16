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

package scouter.server.db

import java.io.File
import java.util.ArrayList

import scouter.server.Logger
import scouter.server.core.cache.TextCache
import scouter.server.core.{CoreRun, ServerStat}
import scouter.server.db.text.TextTable
import scouter.server.util.{EnumerScala, ThreadScala}
import scouter.util.{DateUtil, FileUtil, LinkedMap, RequestQueue}

object TextWR {

    protected val database = new LinkedMap[String, TextTable]();
    protected var idleConns = new ArrayList[String]();

    val queue = new RequestQueue[Data](DBCtr.LARGE_MAX_QUE_SIZE);

    // executed every 10sec
    ThreadScala.start("scouter.server.db.TextWR", {CoreRun.running}, 10000) {
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

    ThreadScala.start("scouter.server.db.TextWR-2") {
        while (DBCtr.running) {
            closeIdle();
            val data = queue.get(10000); //check 10 sec
            ServerStat.put("text.db.queue", queue.size());

            if (data != null) {
                try {
                    process(data);
                } catch {
                    case t: Throwable => t.printStackTrace();
                }
            }
        }
        close();
    }

    protected def closeIdle() {
        if (idleConns.size() == 0)
        return;
        val x = idleConns
        idleConns = new ArrayList[String]();
        EnumerScala.foreach(x.iterator(), (id: String) => {
            FileUtil.close(database.remove(id))
        })
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

    def process(data: Data) {
        if (TextPermWR.isA(data.div)) {
            //성능:중복입력을 막아야한다.
            TextDupCheck.addDuplicated(data.div, data.textUnit);
            TextPermWR.add(data.div, data.hash, data.text);
        } else {
            val textTable = open(data.date);
            if (textTable == null) {
                queue.clear();
                Logger.println("S139", 10, "can't open db");
            } else {
                val ok = textTable.hasKey(data.div, data.hash);
                if (!ok) {
                    TextDupCheck.addDuplicated(data.div, data.textUnit);
                    textTable.set(data.div, data.hash, data.text.getBytes("UTF8"));
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
        if (!ok) {
            Logger.println("S140", 10, "queue exceeded!!");
        }
    }

    /**
      * Text Data Type (date, div, hash, text, textUnit(date&hash))
      * @param _date
      * @param _div
      * @param _hash
      * @param _text
      * @param _tu
      */
    class Data(_date: String, _div: String, _hash: Int, _text: String, _tu: TextDupCheck.TextUnit) {
        val date = _date
        val div = _div
        val hash = _hash
        val text = _text
        var textUnit = _tu
    }

    def open(date: String): TextTable = {
        var textTable = getTable(date)
        if (textTable != null)
            return textTable;
        try {
            this.synchronized {
                textTable = getTable(date)
                if (textTable != null)
                    return textTable;

                val dbDirByDate = getDBPath(date);
                val f = new File(dbDirByDate);
                if (f.exists() == false)
                    f.mkdirs();
                val textFilePath = dbDirByDate + "/text";
                textTable = TextTable.open(textFilePath);
                putTable(date, textTable);
                return textTable;
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
