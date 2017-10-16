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

import scouter.lang.pack.AlertPack
import scouter.io.DataOutputX
import scouter.server.Logger
import scouter.server.db.alert.AlertIndex
import scouter.server.db.alert.AlertWriter
import scouter.server.plugin.PlugInManager
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.RequestQueue
import java.io.File
import scouter.server.util.ThreadScala
import scouter.server.util.OftenAction
import scouter.server.core.AlertSummary

object AlertWR {

    val queue = new RequestQueue[AlertPack](DBCtr.MAX_QUE_SIZE)

    val alert = "alert"
    ThreadScala.start("scouter.server.db.AlertWR") {
        var currentDateUnit = 0L
        while (DBCtr.running) {
            val p = queue.get()
            try {

                if (currentDateUnit != DateUtil.getDateUnit(p.time)) {
                    currentDateUnit = DateUtil.getDateUnit(p.time)
                    close()
                    open(DateUtil.yyyymmdd(p.time))
                }

                if (index == null) {
                    queue.clear();
                    Logger.println("S120", 10, "can't open db")

                    OftenAction.act("AlertWR", 10) {
                        currentDateUnit = 0L
                    }
                } else {
                    val b = new DataOutputX().writePack(p).toByteArray()
                    val location = writer.write(b)
                    index.add(p.time, location)
                }
            } catch {
                case t: Throwable => t.printStackTrace()
            }
        }
        close()
    }

    def add(p: AlertPack) {
        val ok = queue.put(p)
        if (ok == false) {
            Logger.println("S121", 10, "queue exceeded!!")
        }
    }

    var index: AlertIndex = null
    var writer: AlertWriter = null

    def close() {
        FileUtil.close(index)
        FileUtil.close(writer)
        index = null
        writer = null
    }

    def open(date: String) {
        try {
            val path = getDBPath(date)
            val f = new File(path)
            if (f.exists() == false)
                f.mkdirs()
            val file = path + "/" + alert
            index = AlertIndex.open(file)
            writer = AlertWriter.open(file)
            return
        } catch {
            case e: Throwable => {
                e.printStackTrace()
                close()
            }
        }
    }
    def getDBPath(date: String): String = {
        val sb = new StringBuffer()
        sb.append(DBCtr.getRootPath())
        sb.append("/").append(date).append("/alert")
        return sb.toString()
    }

}
