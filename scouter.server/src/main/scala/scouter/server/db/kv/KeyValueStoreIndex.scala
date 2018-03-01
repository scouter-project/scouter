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

package scouter.server.db.kv

import scouter.io.{DataInputX, DataOutputX}
import scouter.server.{Configure, ShutdownManager}
import scouter.server.db.io.IndexKeyFile2
import scouter.util.{FileUtil, IClose, IShutdown, StringKeyLinkedMap}

object KeyValueStoreIndex {
  private val conf = Configure.getInstance()
  private val DEFAULT_MB = conf._mgr_kv_store_index_default_mb

  val table = new StringKeyLinkedMap[KeyValueStoreIndex]()

  ShutdownManager.add(new IShutdown() {
    override def shutdown(): Unit = {
      while (table.size() > 0) {
        table.removeFirst().close();
      }
    }
  })

  def get(div: String): KeyValueStoreIndex = {
    table.synchronized {
      var index = table.get(div);
      if (index != null) {
        return index
      } else {
        return null
      }
    }
  }

  def open(div: String, file: String): KeyValueStoreIndex = {
    table.synchronized {
      var index = table.get(div);
      if (index != null) {
        return index;
      } else {
        index = new KeyValueStoreIndex(div, file);
        table.put(div, index);
        return index;
      }
    }
  }
}

class KeyValueStoreIndex(div: String, file: String) extends IClose {
  var index: IndexKeyFile2 = newIndexKeyFileByType()

  def set(key: Array[Byte], dataPos: Long) {
    this.index.updateOrPut(key, DataOutputX.toBytes5(dataPos))
  }

  def set(key: Array[Byte], dataPos: Long, ttl: Long) {
    this.index.updateOrPut(key, DataOutputX.toBytes5(dataPos), ttl)
  }

  def setTTL(key: Array[Byte], ttl: Long) {
    this.index.setTTL(key, ttl)
  }

  def delete(key: Array[Byte]): Int = {
    return this.index.delete(key)
  }

  def get(key: Array[Byte]): Long = {
    val buf = this.index.get(key)
    if (buf == null) -1 else DataInputX.toLong5(buf, 0)
  }

  def hasKey(key: Array[Byte]): Boolean = {
    return this.index.hasKey(key)
  }

  def newIndexKeyFileByType(): IndexKeyFile2 = {
    new IndexKeyFile2(file, KeyValueStoreIndex.DEFAULT_MB)
  }

  override def close() {
    KeyValueStoreIndex.table.synchronized {
      KeyValueStoreIndex.table.remove(this.div)
      FileUtil.close(this.index)
      this.index = null
    }
  }

}