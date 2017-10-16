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

import scouter.server.db.summary.SummaryIndex
import scouter.server.db.summary.SummaryReader
import scouter.util.DateUtil
import scouter.util.FileUtil
import java.io.File
import scouter.lang.SummaryEnum

object SummaryRD {

  def readByTime(stype: Byte, date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any) {

    val path = SummaryWR.getDBPath(date);
    if (new File(path).canRead()) {
      val file = path + "/" + SummaryWR.root;
      var reader: SummaryReader = null;
      var table: SummaryIndex = null;
      try {
        reader = SummaryReader.open(file)
        stype match {
          case SummaryEnum.APP => table = SummaryIndex.open(file + "_app")
          case SummaryEnum.SQL => table = SummaryIndex.open(file + "_sql")
          case SummaryEnum.ENDUSER_NAVIGATION_TIME |
            SummaryEnum.ENDUSER_AJAX_TIME |
            SummaryEnum.ENDUSER_SCRIPT_ERROR => table = SummaryIndex.open(file + "_enduser")
          case _ => table = SummaryIndex.open(file + "_other")
        }
        table.read(fromTime, toTime, handler, reader.read)
      } catch {
        case e: Throwable => e.printStackTrace();
      } finally {
        FileUtil.close(table);
        FileUtil.close(reader);
      }
    }
  }

  def readByTime(stype: Byte, date: String, handler: (Long, Array[Byte]) => Any) {
    val stime = DateUtil.yyyymmdd(date);
    val etime = stime + DateUtil.MILLIS_PER_DAY;
    readByTime(stype, date, stime, etime, handler);
  }

  def readFromEndTime(stype: Byte, date: String, handler: (Long, Array[Byte]) => Any) {
    val stime = DateUtil.yyyymmdd(date)
    val etime = stime + DateUtil.MILLIS_PER_DAY
    readFromEndTime(stype, date, stime, etime, handler)
  }

  def readFromEndTime(stype: Byte, date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any) {

    val path = SummaryWR.getDBPath(date);
    if (new File(path).canRead()) {
      val file = path + "/" + SummaryWR.root;
      var reader: SummaryReader = null;
      var table: SummaryIndex = null;
      try {
        reader = SummaryReader.open(file);
        stype match {
          case SummaryEnum.APP => table = SummaryIndex.open(file + "_app")
          case SummaryEnum.SQL => table = SummaryIndex.open(file + "_sql")
          case SummaryEnum.ENDUSER_NAVIGATION_TIME |
            SummaryEnum.ENDUSER_AJAX_TIME |
            SummaryEnum.ENDUSER_SCRIPT_ERROR => table = SummaryIndex.open(file + "_enduser")
          case _ => table = SummaryIndex.open(file + "_other")
        }
        table.readFromEnd(fromTime, toTime, handler, reader.read)
      } catch {
        case e: Throwable => e.printStackTrace();
      } finally {
        FileUtil.close(table);
        FileUtil.close(reader);
      }
    }
  }

}