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

import scouter.server.db.alert.AlertIndex
import scouter.server.db.alert.AlertReader
import scouter.util.DateUtil
import scouter.util.FileUtil
import java.io.File

object AlertRD {

    val alert = "alert"

    def readByTime(date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any) {

        val path = AlertWR.getDBPath(date)
        if (new File(path).canRead()) {
            val file = path + "/" + alert
            var reader: AlertReader = null
            var index: AlertIndex = null
            try {
                reader = AlertReader.open(file)
                index = AlertIndex.open(file)
                index.read(fromTime, toTime, handler, reader.read)
            } catch {
                case e: Throwable => e.printStackTrace()
            } finally {
                FileUtil.close(index)
                FileUtil.close(reader)
            }
        }
    }

    def readFromEndTime(date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Unit) {

        val path = AlertWR.getDBPath(date)
        if (new File(path).canRead()) {
            val file = path + "/" + alert
            var reader: AlertReader = null
            var index: AlertIndex = null
            try {
                reader = AlertReader.open(file)
                index = AlertIndex.open(file)
                index.readFromEnd(fromTime, toTime, handler, reader.read)
            } catch {
                case e: Throwable => e.printStackTrace()
            } finally {
                FileUtil.close(index)
                FileUtil.close(reader)
            }
        }
    }

    def readByTime(date: String, handler: (Long, Array[Byte]) => Unit) {
        val stime = DateUtil.yyyymmdd(date)
        val etime = stime + DateUtil.MILLIS_PER_DAY
        readByTime(date, stime, etime, handler)
    }

    def readFromEndTime(date: String, handler: (Long, Array[Byte]) => Unit) {
        val stime = DateUtil.yyyymmdd(date)
        val etime = stime + DateUtil.MILLIS_PER_DAY
        readFromEndTime(date, stime, etime, handler)
    }

}