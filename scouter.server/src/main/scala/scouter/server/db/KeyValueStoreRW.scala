/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.db

import java.io.File

import scouter.server.db.kv.{KeyValueStoreIndex, KeyValueStoreReader, KeyValueStoreWriter}
import scouter.server.Logger
import scouter.util.StringUtil

object KeyValueStoreRW {

  def get(div: String, key: String) : String = {
    if (StringUtil.isEmpty(key) || StringUtil.isEmpty(div)) {
      Logger.println("S223", 10, "illegal argument")
      return null
    }

    val (indexDb, dataWriter, dataReader) = open(div)
    if (indexDb == null || dataWriter == null || dataReader == null) {
      Logger.println("S224", 10, "can't open kv db")
      return null
    } else {
      val dataPos = indexDb.get(key.getBytes("UTF-8"))
      if(dataPos == -1) {
        return null
      }

      val bytes = dataReader.read(dataPos)
      return new String(bytes, "UTF-8")
    }
  }

  def set(div: String, key: String, text: String) : Boolean = {
    return set(div, key, text, -1L);
  }

  def set(div: String, key: String, text: String, ttl: Long) : Boolean = {
    if (StringUtil.isEmpty(text) || StringUtil.isEmpty(key) || StringUtil.isEmpty(div)) {
      Logger.println("S222", 10, "illegal argument")
      return false
    }

    val (indexDb, dataWriter, dataReader) = open(div)
    if (indexDb == null || dataWriter == null) {
      Logger.println("S221", 10, "can't open kv db")
      return false
    } else {
      val dataPos = dataWriter.write(text.getBytes("UTF8"))
      indexDb.set(key.getBytes("UTF8"), dataPos, ttl)
      return true
    }
  }

  def setTTL(div: String, key: String, ttl: Long): Boolean = {
    if (StringUtil.isEmpty(key) || StringUtil.isEmpty(div)) {
      Logger.println("S223", 10, "illegal argument")
      return false
    }

    val (indexDb, dataWriter, dataReader) = open(div)
    if (indexDb == null) {
      Logger.println("S221", 10, "can't open kv db")
      return false
    } else {
      indexDb.setTTL(key.getBytes("UTF8"), ttl)
      return true
    }
  }

  def delete(div: String, key: String): Int = {
    if (StringUtil.isEmpty(key) || StringUtil.isEmpty(div)) {
      Logger.println("S225", 10, "illegal argument")
      return -1
    }

    val (indexDb, dataWriter, dataReader) = open(div)
    if (indexDb == null) {
      Logger.println("S226", 10, "can't open kv db")
      return -1
    } else {
      return indexDb.delete(key.getBytes("UTF8"))
    }
  }

  def open(div: String): (KeyValueStoreIndex, KeyValueStoreWriter, KeyValueStoreReader) = {
    var indexDb: KeyValueStoreIndex = null;
    var dataWriter: KeyValueStoreWriter = null;
    var dataReader: KeyValueStoreReader = null;
    try {
      indexDb = KeyValueStoreIndex.get(div);
      dataWriter = KeyValueStoreWriter.get(div);
      dataReader = KeyValueStoreReader.get(div);
      if (indexDb != null && dataWriter != null && dataReader != null) {
        return (indexDb, dataWriter, dataReader);
      }

      this.synchronized {
        val path = getDBPath();
        val f = new File(path);
        if (f.exists() == false)
          f.mkdirs();
        val file = path + "/kv_" + div;

        indexDb = KeyValueStoreIndex.open(div, file);
        dataWriter = KeyValueStoreWriter.open(div, file);
        dataReader = KeyValueStoreReader.open(div, file);
        return (indexDb, dataWriter, dataReader);
      }
    } catch {
      case e: Throwable => {
        e.printStackTrace();
        indexDb.close();
        dataWriter.close();
        dataReader.close();
      }
    }
    return (null,null,null);
  }

  def getDBPath(): String = {
    val sb = new StringBuffer();
    sb.append(DBCtr.getRootPath());
    sb.append("/00000000/kv2");
    return sb.toString();
  }
}
