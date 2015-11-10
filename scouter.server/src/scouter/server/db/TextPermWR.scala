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
import java.util.ArrayList
import scouter.lang.TextTypes
import scouter.server.Logger
import scouter.server.core.cache.TextCache
import scouter.server.db.text.TextTable
import scouter.server.util.ThreadScala
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.ICloseDB
import scouter.util.IntSet
import scouter.util.LinkedMap
import scouter.util.RequestQueue
import com.sun.xml.internal.ws.util.StringUtils
import scouter.util.StringUtil

object TextPermWR {

    var permTable: TextTable = null

    val queue = new RequestQueue[Data](DBCtr.LARGE_MAX_QUE_SIZE);

//    val common = new IntSet();
//    common.add(HashUtil.hash(TextTypes.METHOD));
//    common.add(HashUtil.hash(TextTypes.GROUP));
//    common.add(HashUtil.hash(TextTypes.CITY));
//    //move to perm db
//    common.add(HashUtil.hash(TextTypes.LOGIN));
//    common.add(HashUtil.hash(TextTypes.DESC));
//    common.add(HashUtil.hash(TextTypes.GROUP));
//    common.add(HashUtil.hash(TextTypes.USER_AGENT));

    //에러만 날짜별로 저장한다.-20151110
    def isA(divs: Int): Boolean = {
        return divs != TextTypes.ERROR;
    }

    ThreadScala.start("scouter.server.db.TextPermWR") {
        while (DBCtr.running) {
            val m = queue.get();
            var writingTable = open();
            try {
                if (writingTable == null) {
                    queue.clear();
                    Logger.println("S137", 10, "can't open db");
                } else {
                    val ok = writingTable.hasKey(m.div, m.hash);
                    if (ok == false) {
                        writingTable.set(m.div, m.hash, m.text.getBytes("UTF8"));
                    }
                }
            } catch {
                case t: Throwable => t.printStackTrace();
            }
        }
        close();
    }

    def add(divHash: Int, hash: Int, text: String) {
        if (StringUtil.isEmpty(text))
            return

        TextCache.put(divHash, hash, text)
        val ok = queue.put(new Data(divHash, hash, text))
        if (ok == false) {
            Logger.println("S138", 10, "queue exceeded!!");
        }
    }

    class Data(_divs: Int, _hash: Int, _text: String) {
        val div = _divs;
        val hash = _hash;
        val text = _text;
    }

    def open(): TextTable = {
        try {
            if (permTable != null)
                return permTable;

            this.synchronized {
                if (permTable != null)
                    return permTable;

                val path = getDBPath();
                val f = new File(path);
                if (f.exists() == false)
                    f.mkdirs();
                val file = path + "/ltext";

                permTable = TextTable.open(file);
                return permTable;
            }
        } catch {
            case e: Throwable =>
                e.printStackTrace();
                close();
        }
        return null;
    }

    def getDBPath(): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/00000000/text");
        return sb.toString();
    }
    def close() {
        if (permTable != null) {
            permTable.close()
        }
    }
}
