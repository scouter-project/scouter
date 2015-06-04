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
 */
package scouter.server.tagcnt.next;

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import scouter.io.BufferedRandomAccessX
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.tagcnt.core.UniqCountData
import scouter.util.IClose;
import scouter.util.FileUtil

class DataFile(path: String) extends IClose {

  val file = new File(path + ".data")
  var raf = new BufferedRandomAccessX(new RandomAccessFile(file, "rw"), 1024, 2, true);
  if (this.raf.getLength() == 0) {
    this.raf.write(0, Array[Byte](0xCA.toByte, 0xFE.toByte));
  }

  def getValue(pos: Long): Array[Int] = {
    this.synchronized {
      val buf = this.raf.read(pos, 4 * 60);
      val cnt = new Array[Int](60)
      var i = 0
      while (i < 60) {
        cnt(i) = DataInputX.toInt(buf, i * 4);
        i += 1
      }
      return cnt;
    }
  }

  def write(pos: Long, value: Array[Int]) {
    this.synchronized {
      val out = new DataOutputX();
      var i = 0
      while (i < 60) {
        out.writeInt(value(i));
        i += 1
      }
      this.raf.write(pos, out.toByteArray());
    }
  }

  def updateAdd(pos: Long, value: Array[Int], uc: UniqCountData): Int = {
    this.synchronized {
      var m = 0;
      while (m < 60) {
        if (value(m) > 0) {
          val oldbytes = this.raf.read(pos + m * 4, 4);
          val old = DataInputX.toInt(oldbytes, 0);
          if (old <= 0) {
            uc.addMin(m);
          }
          this.raf.write(pos + m * 4, DataOutputX.toBytes(old + value(m)));
        }
        m += 1
      }
      return value.length;
    }
  }

  def updateAdd(pos: Long, value: Array[Int]): Int = {
    this.synchronized {
      var mInx = 0;
      while (mInx < 60) {
        if (value(mInx) > 0) {
          val oldbytes = this.raf.read(pos + mInx * 4, 4);
          val old = DataInputX.toInt(oldbytes, 0);
          this.raf.write(pos + mInx * 4, DataOutputX.toBytes(old + value(mInx)));
        }
        mInx += 1
      }
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
