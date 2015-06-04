/*
 *  Copyright 2015 LG CNS.
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
package scouter.server.tagcnt.uniq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import scouter.io.BufferedRandomAccessX;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.FileUtil;
import scouter.util.IClose;

class DataFile(path: String) extends IClose {

  val file = new File(path + ".data")
  var raf = new BufferedRandomAccessX(new RandomAccessFile(file, "rw"), 1024, 2, true);
  if (this.raf.getLength() == 0) {
    this.raf.write(0, Array(0xCA.toByte, 0xFE.toByte));
  }

  def getValue(pos: Long, len: Int): Array[Int] = {
    if (pos <= 0)
      return null;

    this.synchronized {
      val buf = this.raf.read(pos, 4 * len);
      val cnt = new Array[Int](len)
      for (i <- 0 to len - 1) {
        cnt(i) = DataInputX.toInt(buf, i * 4);
      }
      return cnt;
    }
  }

  def write(pos: Long, value: Array[Int]) {
    this.synchronized {
      val out = new DataOutputX();
      for (i <- 0 to value.length - 1) {
        out.writeInt(value(i));
      }
      this.raf.write(pos, out.toByteArray());
    }
  }

  def update(pos: Long, inx: Int, value: Int) {
    this.synchronized {
      this.raf.write(pos + inx * 4, DataOutputX.toBytes(value));
    }
  }

  def updateAdd(pos: Long, inx: Int, value: Int): Int = {
    this.synchronized {
      val oldb = this.raf.read(pos + inx * 4, 4);
      val oldi = DataInputX.toInt(oldb, 0);
      this.raf.write(pos + inx * 4, DataOutputX.toBytes(oldi + value));

      return oldi + value;
    }
  }

  def updateAdd(pos: Long, value: Array[Int]): Int = {
    this.synchronized {
      val oldbytes = this.raf.read(pos, value.length * 4);
      val out = new DataOutputX();
      for (idx <- 0 to value.length - 1) {
        val old = DataInputX.toInt(oldbytes, idx * 4);
        out.writeInt(old + value(idx));
      }
      this.raf.write(pos, out.toByteArray());
      return value.length;
    }
  }

  def append(value: Array[Int]): Long = {
    this.synchronized {
      val pos = this.raf.getLength();
      write(pos, value);
      return pos;
    }
  }

  def close() {
    this.synchronized {
      if (this.raf != null) {
        FileUtil.close(this.raf);
        this.raf = null
      }
    }
  }

  def getFirstPos() = 2L
  def getLength() = if (raf == null) 0L else raf.getLength()

}
