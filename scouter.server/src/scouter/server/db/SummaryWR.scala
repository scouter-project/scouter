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
import scouter.lang.pack.SummaryPack
import scouter.io.DataOutputX
import scouter.server.Logger
import scouter.server.db.summary.SummaryIndex
import scouter.server.db.summary.SummaryWriter
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.RequestQueue
import java.io.File
import scouter.server.util.ThreadScala
import scouter.server.util.OftenAction
import scouter.lang.SummaryEnum
import scouter.server.core.ServerStat
object SummaryWR {
  val root = "sum";
  val queue = new RequestQueue[SummaryPack](DBCtr.MAX_QUE_SIZE);
  ThreadScala.start("scouter.server.db.SummaryWR") {
    var currentDateUnit = 0L
    while (DBCtr.running) {
      val p = queue.get();
      ServerStat.put("summary.db.queue", queue.size());
      try {
        if (currentDateUnit != DateUtil.getDateUnit(p.time)) {
          currentDateUnit = DateUtil.getDateUnit(p.time);
          close();
          open(DateUtil.yyyymmdd(p.time));
        }
        if (iapp == null) {
          OftenAction.act("SummaryWR", 10) {
            queue.clear();
            currentDateUnit = 0;
          }
          Logger.println("S210", 10, "can't open db");
        } else {
          val b = new DataOutputX().writePack(p).toByteArray()
          val location = writer.write(b);
          p.stype match {
            case SummaryEnum.APP => iapp.add(p.time, location);
            case SummaryEnum.SQL => isql.add(p.time, location);
            case SummaryEnum.ENDUSER_NAVIGATION_TIME |
              SummaryEnum.ENDUSER_AJAX_TIME |
              SummaryEnum.ENDUSER_SCRIPT_ERROR => ienduser.add(p.time, location);
            case _ => iothers.add(p.time, location);
          }
        }
      } catch {
        case t: Throwable => t.printStackTrace();
      }
    }
    close()
  }
  def add(p: SummaryPack) {
    val ok = queue.put(p);
    if (ok == false) {
      Logger.println("S211", 10, "queue exceeded!!");
    }
  }
  var iapp: SummaryIndex = null
  var isql: SummaryIndex = null
  var ienduser: SummaryIndex = null
  var iothers: SummaryIndex = null
  var writer: SummaryWriter = null
  def close() {
    FileUtil.close(iapp);
    FileUtil.close(isql);
    FileUtil.close(ienduser);
    FileUtil.close(iothers);
    FileUtil.close(writer);
    iapp = null;
    isql = null;
    iothers = null;
    writer = null;
  }
  def open(date: String) {
    try {
      val path = getDBPath(date);
      val f = new File(path);
      if (f.exists() == false)
        f.mkdirs();
      val file = path + "/" + root;
      iapp = SummaryIndex.open(file + "_app");
      isql = SummaryIndex.open(file + "_sql");
      ienduser = SummaryIndex.open(file + "_enduser");
      iothers = SummaryIndex.open(file + "_other");
      writer = SummaryWriter.open(file);
    } catch {
      case e: Throwable => {
        e.printStackTrace();
        close();
      }
    }
  }
  def getDBPath(date: String): String = {
    val sb = new StringBuffer();
    sb.append(DBCtr.getRootPath());
    sb.append("/").append(date).append("/").append(root);
    return sb.toString();
  }
}
