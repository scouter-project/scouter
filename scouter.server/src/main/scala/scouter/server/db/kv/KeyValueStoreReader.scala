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

import java.io.{IOException, RandomAccessFile}

import scouter.server.ShutdownManager
import scouter.server.db.kv.KeyValueStoreIndex.table
import scouter.util.{FileUtil, IClose, IShutdown, StringKeyLinkedMap}

object KeyValueStoreReader {
  val table = new StringKeyLinkedMap[KeyValueStoreReader]()

  ShutdownManager.add(new IShutdown() {
    override def shutdown(): Unit = {
      while (table.size() > 0) {
        table.removeFirst().close();
      }
    }
  })

  def get(div: String): KeyValueStoreReader = {
    table.synchronized {
      var reader = table.get(div);
      if (reader != null) {
        return reader
      } else {
        return null
      }
    }
  }

  def open(div: String, file: String): KeyValueStoreReader = {
    table.synchronized {
      var reader = table.get(div);
      if (reader == null) {
        reader = new KeyValueStoreReader(div, file);
        table.put(div, reader);
      }
      return reader;
    }
  }
}

class KeyValueStoreReader(div: String, file: String) extends IClose {
  val dataFile = new RandomAccessFile(file + ".kvdata", "r");

  def read(fpos: Long): Array[Byte] = {
    this.synchronized {
      try {
        dataFile.seek(fpos);
        val length = dataFile.readInt();
        val buffer = new Array[Byte](length);
        dataFile.read(buffer);
        return buffer;
      } catch {
        case e: IOException =>
          throw new RuntimeException(e);
      }
    }
  }

  def close() {
    KeyValueStoreReader.table.synchronized {
      KeyValueStoreWriter.table.remove(this.div)
      FileUtil.close(dataFile)
      dataFile == null
    }
  }
}