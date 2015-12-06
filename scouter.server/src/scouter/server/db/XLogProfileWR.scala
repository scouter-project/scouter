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
import java.util.List
import scouter.server.Logger
import scouter.server.ShutdownManager
import scouter.server.db.xlog.XLogProfileDataReader
import scouter.server.db.xlog.XLogProfileDataWriter
import scouter.server.db.xlog.XLogProfileIndex
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.RequestQueue
import scouter.util.IClose
import scouter.util.IShutdown
import scouter.util.ThreadUtil
import java.io.File
import scouter.server.util.ThreadScala
import scouter.server.util.OftenAction
import scouter.server.core.ServerStat
import scouter.server.Configure
object XLogProfileWR extends IClose {
    val queue = new RequestQueue[Data](Configure.getInstance().profile_queue_size);
    class ResultSet(keys: List[Long], var reader: XLogProfileDataReader) {
        var max: Int = if (keys == null) 0 else keys.size()
        var x: Int = 0;
        def hasNext() = x < max
        def readNext() = {
            if (x >= max || reader == null) null else reader.read(keys.get(x));
            x = x + 1
        }
        def close() =
            if (this.reader != null) {
                this.reader.close();
                this.reader = null
            }
    }
    val prefix = "xlog";
    class Data(_time: Long, _txid: Long, _data: Array[Byte]) {
        val time = _time
        val txid = _txid
        val data = _data
    }
    var currentDateUnit: Long = 0
    var index: XLogProfileIndex = null
    var writer: XLogProfileDataWriter = null
    ThreadScala.start("scouter.server.db.XLogProfileWR") {
        while (DBCtr.running) {
            val m = queue.get();
              ServerStat.put("profile.db.queue",queue.size());
              try {
                if (currentDateUnit != DateUtil.getDateUnit(m.time)) {
                    currentDateUnit = DateUtil.getDateUnit(m.time);
                    close();
                    open(DateUtil.yyyymmdd(m.time));
                }
                if (index == null) {
                    OftenAction.act("XLogWR", 10) {
                        queue.clear();
                        currentDateUnit = 0;
                    }
                    Logger.println("S141", 10, "can't open ");
                } else {
                    val offset = writer.write(m.data)
                    index.addByTxid(m.txid, offset);
                }
            } catch {
                case e: Throwable => e.printStackTrace()
            }
        }
        close();
    }
    def add(time: Long, txid: Long, data: Array[Byte]) {
        val ok = queue.put(new Data(time, txid, data));
        if (ok == false) {
            Logger.println("S142", 10, "queue exceeded!!");
        }
    }
    def close() {
        FileUtil.close(index);
        FileUtil.close(writer);
        writer = null;
        index = null;
    }
    def open(date: String) {
        try {
            val path = getDBPath(date);
            val f = new File(path)
            if (f.exists() == false)
                f.mkdirs();
            var file = path + "/" + prefix;
            index = XLogProfileIndex.open(file);
            writer = XLogProfileDataWriter.open(date, file);
            return ;
        } catch {
            case e: Throwable => {
                close()
                e.printStackTrace()
            }
        }
        return ;
    }
    def getDBPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date).append(XLogWR.dir);
        return sb.toString();
    }
}
