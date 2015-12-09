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
package scouter.server.core;
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserManager
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import scouter.lang.TextTypes
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.sqltable.TableFinder
import scouter.server.db.TextRD
import scouter.server.db.TextWR
import scouter.util.RequestQueue
import scouter.util.IntLinkedSet
import scouter.util.StringKeyLinkedMap
import java.io.StringReader
import scouter.server.util.ThreadScala
object SqlTables {
    val MAX_Q1 = 500;
    val MAX_Q2 = 10000;
    val failSet = new IntLinkedSet().setMax(10000);
    val queue1 = new RequestQueue[Data](MAX_Q1);
    val queue2 = new RequestQueue[Data](MAX_Q2);
    ThreadScala.startDaemon("scouter.server.core.SqlTables", { CoreRun.running }) {
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
            return ;
        val data = new Data(date, sqlHash, sqlText);
        var ok = queue1.put(data);
        if (ok) {
            return ;
        }
        data.sqlText = null;
        ok = queue2.put(data);
        if (ok == false) {
            Logger.println("S111", 10, "queue exceeded!!");
        }
    }
    val parsed = new StringKeyLinkedMap[IntLinkedSet]().setMax(2);
    def process(data: Data) {
        try {
            var sqlHashSet: IntLinkedSet = parsed.get(data.date);
            if (sqlHashSet == null) {
                sqlHashSet = new IntLinkedSet().setMax(10000);
                parsed.put(data.date, sqlHashSet);
            }
            if (sqlHashSet.contains(data.sqlHash))
                return ;
            if (data.sqlText == null) {
                data.sqlText = TextRD.getString(data.date, TextTypes.SQL, data.sqlHash);
            }
            if (data.sqlText == null)
                return ;
            if (failSet.contains(data.sqlHash))
                return ;
            val sb = doAction(data.sqlText);
            sqlHashSet.put(data.sqlHash);
            TextWR.add(data.date, TextTypes.SQL_TABLES, data.sqlHash, sb.toString());
        } catch {
            case t: Throwable => {
                failSet.put(data.sqlHash);
                Logger.println("S112", data.sqlText + "\n" + t);
            }
        }
    }
    def doAction(sqlText: String): String = {
        val sb = new StringBuffer();
        val statement = new CCJSqlParserManager().parse(new StringReader(sqlText.replace('@', '0')));
        if (statement.isInstanceOf[Select]) {
            val select = statement.asInstanceOf[Select];
            val tableFinder = new TableFinder();
            if (select.getSelectBody() != null) {
                select.getSelectBody().accept(tableFinder);
                val iter = tableFinder.getTableList().iterator()
                while (iter.hasNext()) {
                    val tableName = iter.next();
                    if (sb.length() > 0)
                        sb.append(',');
                    sb.append(tableName).append("(S)");
                }
            }
        } else if (statement.isInstanceOf[Insert]) {
            val x = statement.asInstanceOf[Insert]
            val t = x.getTable();
            sb.append(x.getTable().getName()).append("(I)");
            if (x.getSelect() != null) {
                val selectStatement = x.getSelect();
                val tableFinder = new TableFinder();
                selectStatement.getSelectBody().accept(tableFinder);
                val iter = tableFinder.getTableList().iterator()
                while (iter.hasNext()) {
                    def tableName = iter.next();
                    sb.append(',').append(tableName).append("(S)");
                }
            }
        } else if (statement.isInstanceOf[Delete]) {
            val x = statement.asInstanceOf[Delete]
            val t = x.getTable();
            sb.append(x.getTable().getName()).append("(D)");
            if (x.getWhere() != null) {
                val tableFinder = new TableFinder();
                x.getWhere().accept(tableFinder);
                val iter = tableFinder.getTableList().iterator()
                while (iter.hasNext()) {
                    val tableName = iter.next();
                    sb.append(',').append(tableName).append("(S)");
                }
            }
        } else if (statement.isInstanceOf[Update]) {
            val x = statement.asInstanceOf[Update]
            val tables = x.getTables().iterator()
            while (tables.hasNext()) {
                val tableName = tables.next();
                sb.append(',')
                sb.append(tableName).append("(U)");
            }
            if (x.getWhere() != null) {
                val tableFinder = new TableFinder();
                x.getWhere().accept(tableFinder);
                val iter = tableFinder.getTableList().iterator()
                while (iter.hasNext()) {
                    val tableName = iter.next();
                    sb.append(',').append(tableName).append("(S)");
                }
            }
        }
        return sb.toString();
    }
}
