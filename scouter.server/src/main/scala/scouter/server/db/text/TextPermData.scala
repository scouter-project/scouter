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

package scouter.server.db.text;

import java.io.IOException
import java.io.RandomAccessFile
import java.util.Hashtable
import scouter.util.IClose
import scouter.util.IntKeyLinkedMap
import scouter.util.StringKeyLinkedMap


object TextPermData {
  val table = new StringKeyLinkedMap[TextPermData]();
  def get(div: String): TextPermData = {
    table.synchronized {
      var reader = table.get(div);
      if (reader != null) {
        reader.refrence += 1;
      }
      return reader;
    }
  }
  def open(div: String, file: String): TextPermData = {
    table.synchronized {
      var reader = table.get(div);
      if (reader != null) {
        reader.refrence += 1;
      } else {
        reader = new TextPermData(div, file);
        table.put(div, reader);
      }
      return reader;
    }
  }
  def closeAll() {
    while (table.size() > 0) {
      table.removeFirst().close();
    }
  }
}

class TextPermData(div: String, file: String) extends IClose {

  var refrence = 0;
  val dataFile = new RandomAccessFile(file + ".data", "rw");

  def close() {
    TextPermData.table.synchronized {
      if (this.refrence == 0) {
        TextPermData.table.remove(this.div);
        try {
          dataFile.close();
        } catch {
          case e: Throwable =>
            e.printStackTrace()
        }
      } else {
        this.refrence -= 1;
      }
    }

  }

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

  def write(data: Array[Byte]): Long = {
    this.synchronized {
      val location = dataFile.length();
      dataFile.seek(location);
      dataFile.writeInt(data.length);
      dataFile.write(data);
      return location;
    }
  }

  def update(pos: Long, data: Array[Byte]) {
    dataFile.seek(pos);
    dataFile.writeInt(data.length);
    dataFile.write(data);
  }
}