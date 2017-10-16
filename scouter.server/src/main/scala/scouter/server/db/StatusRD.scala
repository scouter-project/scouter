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

import scouter.server.db.status.StatusIndex
import scouter.server.db.status.StatusReader
import scouter.util.DateUtil
import scouter.util.FileUtil
import java.io.File

object StatusRD {

  def readByTime(date: String, fromTime: Long, toTime: Long, handler:(Long, Array[Byte]) => Any) {

    val path = StatusWR.getDBPath(date);
    if (new File(path).canRead()) {
      val file = path + "/" + StatusWR.status;
      var reader: StatusReader = null;
      var table: StatusIndex = null;
      try {
        reader = StatusReader.open(file)
        table = StatusIndex.open(file)
        table.read(fromTime, toTime, handler, reader.read) 
      } catch {
        case e: Throwable => //e.printStackTrace();
      } finally {
        FileUtil.close(table);
        FileUtil.close(reader);
      }
    }
  }

  def readByTime(date: String, handler:(Long, Array[Byte]) => Any) {
    val stime = DateUtil.yyyymmdd(date);
    val etime = stime + DateUtil.MILLIS_PER_DAY;
    readByTime(date, stime, etime, handler);
  }

  def readFromEndTime(date: String,handler:(Long, Array[Byte]) => Any) {
    val stime = DateUtil.yyyymmdd(date)
    val etime = stime + DateUtil.MILLIS_PER_DAY
    readFromEndTime(date, stime, etime, handler)
  }

  def readFromEndTime(date: String, fromTime: Long, toTime: Long,handler:(Long, Array[Byte]) => Any) {

    val path = StatusWR.getDBPath(date);
    if (new File(path).canRead()) {
      val file = path + "/" + StatusWR.status;
      var reader: StatusReader = null;
      var table: StatusIndex = null;
      try {
        reader = StatusReader.open(file);
        table = StatusIndex.open(file);
        table.readFromEnd(fromTime, toTime, handler, reader.read) 
      } catch {
        case e: Throwable => //e.printStackTrace();
      } finally {
        FileUtil.close(table);
        FileUtil.close(reader);
      }
    }
  }

}