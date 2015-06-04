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
package scouter.server.tagcnt.uniq;

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.IClose;
import scouter.util.FileUtil

class ITEM(_datapos: Long, _key: Array[Byte], _link: Long, _next: Long, _total: Int) {
  val datapos = _datapos
  val key = _key
  val link = _link
  val next = _next
  var total = _total
}

class KeyFile(path: String) extends IClose {

  val file = new File(path + ".kfile")
  var raf = new RandomAccessFile(file, "rw");
  if (this.raf.length() == 0) {
    this.raf.write(Array[Byte](0xCA.toByte, 0xFE.toByte))
  }

  def getRecord(pos: Long): ITEM = {
    this.synchronized {
      this.raf.seek(pos);

      var in = new DataInputX(this.raf);
      val buf = in.read(5 + 5);

      val link = DataInputX.toLong5(buf, 0);
      val datapos = DataInputX.toLong5(buf, 5);

      val key = in.readShortBytes();
      val next = this.raf.getFilePointer();
      return new ITEM(datapos, key, link, next, 0);
    }
  }

  def getHashLink(pos: Long): Long = {

    this.synchronized {
      this.raf.seek(pos);
      return new DataInputX(this.raf).readLong5();
    }
  }

  def getKey(pos: Long): Array[Byte] = {
    this.synchronized {
      this.raf.seek(pos + 5 + 5);
      val in = new DataInputX(this.raf);
      return in.readShortBytes();
    }
  }

  def getDataPosition(pos: Long): Long = {
    this.synchronized {
      this.raf.seek(pos + 5);
      return new DataInputX(this.raf).readLong5();
    }
  }

  def setHashLink(pos: Long, link: Long) {
    this.synchronized {
      this.raf.seek(pos);
      new DataOutputX(this.raf).writeLong5(link);
    }
  }

  def write(pos: Long, next: Long, key: Array[Byte], vpos: Long) {
    this.synchronized {
      val out = new DataOutputX();
      out.writeLong5(next);
      out.writeLong5(vpos);
      out.writeShortBytes(key);

      this.raf.seek(pos);
      this.raf.write(out.toByteArray());
    }
  }

  def append(next: Long, key: Array[Byte], vpos: Long): Long = {
    this.synchronized {
      val pos = this.raf.length();

      write(pos, next, key, vpos);
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
  def getLength() = if (raf == null) 0 else raf.length()

}
