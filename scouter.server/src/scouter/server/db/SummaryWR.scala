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
 *
 */

package scouter.server.db;

import scouter.lang.pack.MapPack
import scouter.io.DataOutputX
import scouter.server.Logger
import scouter.server.db.summary.SummaryDataWriter
import scouter.server.db.summary.SummaryIndex
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.RequestQueue
import java.io.File
import scouter.server.util.ThreadScala
import scouter.server.util.OftenAction

object SummaryWR {

    val queue = new RequestQueue[MapPack](DBCtr.MAX_QUE_SIZE);

    ThreadScala.start("SummaryWR") {

        var curDateUnit = 0L;
        while (DBCtr.running) {

            val p = queue.get();
            try {
                val time = p.getLong("time");
                val objHash = p.getInt("objHash");
                val stype = p.getInt("type").toByte

                if (curDateUnit != DateUtil.getDateUnit(time)) {
                    curDateUnit = DateUtil.getDateUnit(time);
                    close();
                    open(DateUtil.yyyymmdd(time));
                }
                if (index == null) {
                    OftenAction.act("SummaryWR", 10) {
                        queue.clear();
                        curDateUnit = 0;
                    }
                    Logger.println("StatWR", 10, "can't open db");
                } else {
                    val b = new DataOutputX().writePack(p).toByteArray();
                    val location = writer.write(b);
                    index.add(time, objHash, stype, location);
                }
            } catch {
                case e: Exception => e.printStackTrace();
            }
        }
        close()
    }

    def add(time: Long, stype: Byte, data: MapPack) {
        data.put("time", time);
        data.put("type", stype);

        val ok = queue.put(data);
        if (ok == false) {
            Logger.println("StatWR", 10, "queue exceeded!!");
        }
    }

    var index: SummaryIndex = null;
    var writer: SummaryDataWriter = null;

    def close() {
        FileUtil.close(index);
        FileUtil.close(writer);
        index = null;
        writer = null;
    }

    def open(date: String) {
        try {
            val path = getDBPath(date);
            val f = new File(path);
            if (f.exists() == false)
                f.mkdirs();
            val file = path + "/stat";
            index = SummaryIndex.open(file);
            writer = SummaryDataWriter.open(file);
        } catch {
            case e: Throwable => {
                e.printStackTrace();
                close()
            }
        }
    }

    def getDBPath(date: String): String = {
        val sb = new StringBuffer()
        sb.append(DBCtr.getRootPath())
        sb.append("/").append(date).append("/stat")
        return sb.toString()
    }
}