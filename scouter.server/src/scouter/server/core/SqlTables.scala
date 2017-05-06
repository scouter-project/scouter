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
package scouter.server.core

import java.io.StringReader

import net.sf.jsqlparser.parser.CCJSqlParserManager
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import scouter.lang.TextTypes
import scouter.server.core.sqltable.TableFinder
import scouter.server.db.{TextRD, TextWR}
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.{IntLinkedSet, RequestQueue, StringKeyLinkedMap}
import scouter.util.SQLSimpleParser;

object SqlTables {
    val MAX_Q1 = 500;
    val MAX_Q2 = 10000;
    val failSet = new IntLinkedSet().setMax(10000);
    val queue1 = new RequestQueue[Data](MAX_Q1);
    val queue2 = new RequestQueue[Data](MAX_Q2);

    val parsed = new StringKeyLinkedMap[IntLinkedSet]().setMax(2); //sqlHashSet by Date

    ThreadScala.startDaemon("scouter.server.core.SqlTables", {
        CoreRun.running
    }) {
        if (queue1.size() > 0) {
            process(queue1.get());
        } else if (queue2.size() > 0) {
            process(queue2.get());
        } else {
            process(queue1.get());
        }
    }

    class Data(_date: String, _sqlHash: Int, _sqlText: String) {
        val date = _date;
        val sqlHash = _sqlHash;
        var sqlText = _sqlText;
    }

    /*
	 * 어느정도 서버에 부담을 줄지 알수 없다. 따라서 이문제를 해결하기 위해 parse 여부를 옵션 처리한다.
	 */
    def add(date: String, sqlHash: Int, sqlText: String) {
        if (Configure.getInstance().sql_table_parsing_enabled == false)
            return;
        val data = new Data(date, sqlHash, sqlText);
        var ok = queue1.put(data);
        if (ok) {
            return;
        }
        data.sqlText = null;
        ok = queue2.put(data);
        if (!ok) {
            Logger.println("S111", 10, "queue exceeded!!");
        }
    }

    def process(data: Data) {
        try {
            //debug code
            val trace = Configure.getInstance()._trace;
            //if(trace) Logger.println("#################!@#$%^&*()", "1. Trace - sql table test");
            var sqlHashSet: IntLinkedSet = parsed.get(data.date);
            if (sqlHashSet == null) {
                sqlHashSet = new IntLinkedSet().setMax(10000);
                parsed.put(data.date, sqlHashSet);
            }
            //if(trace) Logger.println("#################!@#$%^&*()", "2. Trace - sql table test");
            if (sqlHashSet.contains(data.sqlHash)) {
                return
            }
            //if(trace) Logger.println("#################!@#$%^&*()", "3. Trace - sql table test");
            if (failSet.contains(data.sqlHash)) {
                return
            }
            //if(trace) Logger.println("#################!@#$%^&*()", "4. Trace - sql table test");
            if (data.sqlText == null) {
                data.sqlText = TextRD.getString(data.date, TextTypes.SQL, data.sqlHash);
            }
            //if(trace) Logger.println("#################!@#$%^&*()", "5. Trace - sql table test");
            if (data.sqlText == null) {
                return
            }
            //if(trace) Logger.println("#################!@#$%^&*()", "6. Trace - sql table test");
            val sbTables = parseTable(data.sqlText);
            //if(trace) Logger.println("#################!@#$%^&*()", "7. Trace - sqlText : " + data.sqlText);
            //if(trace) Logger.println("#################!@#$%^&*()", "7. Trace - table : " + sbTables);
            sqlHashSet.put(data.sqlHash);
            TextWR.add(data.date, TextTypes.SQL_TABLES, data.sqlHash, sbTables.toString());
        } catch {
            case t: Throwable => {
                failSet.put(data.sqlHash);
                if (Configure.getInstance().log_sql_parsing_fail_enabled) {
                    Logger.println("S112", data.sqlText + "\n" + t);
                }
            }
        }
    }

    def parseTable(sqlText: String): String = {
        return new SQLSimpleParser().getCrudInfo(sqlText);
    }
}
