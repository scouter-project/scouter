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

import scouter.server.ShutdownManager
import scouter.server.db.io.RealDataFile
import scouter.server.db.kv.KeyValueStoreIndex.table
import scouter.util.{FileUtil, IClose, IShutdown, StringKeyLinkedMap}

object KeyValueStoreWriter {
  val table = new StringKeyLinkedMap[KeyValueStoreWriter]()

  ShutdownManager.add(new IShutdown() {
    override def shutdown(): Unit = {
      while (table.size() > 0) {
        table.removeFirst().close();
      }
    }
  })

  def get(div: String): KeyValueStoreWriter = {
    table.synchronized {
      var reader = table.get(div);
      if (reader != null) {
        return reader
      } else {
        return null
      }
    }
  }

  def open(div: String, file: String): KeyValueStoreWriter = {
    table.synchronized {
      var reader = table.get(div);
      if (reader == null) {
        reader = new KeyValueStoreWriter(div, file);
        table.put(div, reader);
      }
      return reader;
    }
  }

}

class KeyValueStoreWriter(div: String, file: String) extends IClose {
  val out = new RealDataFile(file + ".kvdata")

  def write(bytes: Array[Byte]): Long = {
    this.synchronized {
      val point = out.getOffset();
      out.writeInt(bytes.length);
      out.write(bytes);
      out.flush();
      return point;
    }
  }

  override def close() {
    KeyValueStoreWriter.table.synchronized {
      KeyValueStoreWriter.table.remove(this.file)
      FileUtil.close(out)
      out == null
    }
  }
}