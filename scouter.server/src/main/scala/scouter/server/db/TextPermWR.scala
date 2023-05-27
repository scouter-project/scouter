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

import scouter.lang.TextTypes
import scouter.server.{Configure, Logger}
import scouter.server.core.cache.TextCache
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
import scouter.util.StringUtil
import scouter.server.db.text.TextPermIndex
import scouter.server.db.text.TextPermData

object TextPermWR {

  val queue = new RequestQueue[Data](DBCtr.LARGE_MAX_QUE_SIZE);

  //에러만 날짜별로 저장한다.-20151110
  def isA(divs: String): Boolean = {
    val conf = Configure.getInstance();

    divs match {
      case TextTypes.ERROR => false
      case TextTypes.SERVICE => !conf.mgr_text_db_daily_service_enabled
      case TextTypes.APICALL => !conf.mgr_text_db_daily_api_enabled
      case TextTypes.USER_AGENT => !conf.mgr_text_db_daily_ua_enabled
      case _ => true
    }
  }

  ThreadScala.start("scouter.server.db.TextPermWR") {
    while (DBCtr.running) {
      val data = queue.get();
      var (indexDb, dataDb) = open(data.div);
      try {
        if (indexDb == null) {
          queue.clear();
          Logger.println("S137", 10, "can't open db");
        } else {
          val ok = indexDb.hasKey(data.hash);
          if (ok == false) {
            val dataPos = dataDb.write(data.text.getBytes("UTF8"));
            indexDb.set(data.hash, dataPos);
          }
        }
      } catch {
        case t: Throwable => t.printStackTrace();
      }
    }
    close();
  }

  def add(divHash: String, hash: Int, text: String) {
    if (StringUtil.isEmpty(text))
      return

    TextCache.put(divHash, hash, text)
    val ok = queue.put(new Data(divHash, hash, text))
    if (ok == false) {
      Logger.println("S138", 10, "queue exceeded!!");
      queue.clear();
    }
  }

  class Data(_divs: String, _hash: Int, _text: String) {
    val div = _divs;
    val hash = _hash;
    val text = _text;
  }

  def open(div: String): (TextPermIndex, TextPermData) = {
    try {
      var indexDb = TextPermIndex.get(div);
      var dataDb = TextPermData.get(div);
      if (indexDb != null && dataDb != null) {
        return (indexDb, dataDb);
      }
      this.synchronized {

        val path = getDBPath();
        val f = new File(path);
        if (f.exists() == false)
          f.mkdirs();
        val file = path + "/text_" + div;

        indexDb = TextPermIndex.open(div, file);
        dataDb = TextPermData.open(div, file);

        return (indexDb, dataDb);
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace();
        close();
    }
    return (null,null);
  }

  def getDBPath(): String = {
    val sb = new StringBuffer();
    sb.append(DBCtr.getRootPath());
    sb.append("/00000000/text");
    return sb.toString();
  }
  def close() {
    TextPermIndex.closeAll();
    TextPermData.closeAll();
  }
 
}
