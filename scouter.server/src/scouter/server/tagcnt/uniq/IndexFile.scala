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

import java.io.IOException;
import java.util.Hashtable;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.server.Logger;
import scouter.server.db.io.MemHashBlock;
import scouter.util.CompareUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.IClose;
object IndexFile {

  private val table = new Hashtable[String, IndexFile]();

  def open(path: String): IndexFile = {
    table.synchronized {
      var inx = table.get(path);
      if (inx != null) {
        inx.refrence += 1;
        return inx;
      } else {
        inx = new IndexFile(path);
        table.put(path, inx);
        return inx;
      }
    }
  }
}
class IndexFile(path: String, hashSize: Int = 1) extends IClose {

  var refrence = 0;

  val MB = 1024 * 1024;
  val hashFile = new MemHashBlock(path, hashSize * MB)
  val keyFile = new KeyFile(path);
  val countFile = new DataFile(path)

  def getKeyFile() = keyFile

  def put(key: Array[Byte], inx: Int, ucnt: Int) {
    this.synchronized {
      if (key == null) {
        throw new IOException("invalid key");
      }
      val keyHash = HashUtil.hash(key);
      var pos = hashFile.get(keyHash);
      val firstPos = pos;
      val value = new Array[Int](1440);
      value(inx) = ucnt;
      val vpos = this.countFile.append(value);
      pos = this.keyFile.append(firstPos, key, vpos);
      this.hashFile.put(keyHash, pos);
    }
  }

  def getVPos(key: Array[Byte]): Long = {
    if (key == null) {
      throw new IOException("invalid key");
    }
    val keyHash = HashUtil.hash(key);
    var pos = hashFile.get(keyHash);
    while (pos > 0) {
      val okey = this.keyFile.getKey(pos);
      if (CompareUtil.equals(okey, key)) {
        val r = this.keyFile.getRecord(pos);
        return r.datapos;
      }
      pos = this.keyFile.getHashLink(pos);
    }

    return 0;
  }

  def updateAdd(key: Array[Byte], inx: Int, ucnt: Int): Int = {

    if (key == null) {
      throw new IOException("invalid key");
    }
    val keyHash = HashUtil.hash(key);
    var pos = hashFile.get(keyHash);

    val firstPos = pos;
    while (pos > 0) {
      val okey = this.keyFile.getKey(pos);
      if (CompareUtil.equals(okey, key)) {
        val datapos = this.keyFile.getDataPosition(pos);
        this.countFile.updateAdd(datapos, inx, ucnt);
        return 1;
      }
      pos = this.keyFile.getHashLink(pos);
    }
    val value = new Array[Int](1440);
    value(inx) = ucnt;

    val vpos = this.countFile.append(value);
    pos = this.keyFile.append(firstPos, key, vpos);
    this.hashFile.put(keyHash, pos);
    return 0
  }

  def hasKey(key: Array[Byte]): Boolean = {
    if (key == null) {
      throw new IOException("invalid key");
    }

    val keyHash = HashUtil.hash(key);
    var pos = hashFile.get(keyHash);
    while (pos > 0) {
      val okey = this.keyFile.getKey(pos);
      if (CompareUtil.equals(okey, key)) {
        return true;
      }
      pos = this.keyFile.getHashLink(pos);
    }

    return false;
  }

  def read(handler:  (Long,Array[Int])=>Any): Boolean = {
    if (this.keyFile == null)
      return false;
    var pos = this.keyFile.getFirstPos();
    val length = this.keyFile.getLength();
    var done = 0;
    try {

      while (pos < length && pos >0 ) {
        val r = this.keyFile.getRecord(pos);

        val in = new DataInputX(r.key);
        val tag = in.readLong();
        val values = this.countFile.getValue(r.datapos, 1440);
        handler(tag, values) 
        done += 1;
        pos = r.next;
      }
    } catch {
      case t: Throwable =>
        Logger.println("S186", this.keyFile + " : read=" + done + " pos=" + pos + " file-len=" + length + " " + t);
    }
    return true;
  }

  def getValue(vpos: Long, len: Int): Array[Int] = {
    if (vpos <= 0)
      return null;
    return this.countFile.getValue(vpos, len);
  }

  private def _close() {
    this.hashFile.close();
    FileUtil.close(this.keyFile);
    FileUtil.close(this.countFile);
  }

  // //////////
  def add(tagkey: Long, inx: Int, ucnt: Int) {
    try {

      val key = DataOutputX.toBytes(tagkey);
      this.updateAdd(key, inx, ucnt);
    } catch {
      case e: Throwable =>
    }
  }

  def get(tagkey: Long): Array[Int] = {
    val vpos = this.getVPos(DataOutputX.toBytes(tagkey));
    return this.getValue(vpos, 1440);
  }

  def hasKey(tagkey: Long): Boolean = {
    return this.hasKey(DataOutputX.toBytes(tagkey));
  }

  override def close() {
    IndexFile.table.synchronized {
      if (this.refrence == 0) {
        this._close();
        IndexFile.table.remove(this.path);
      } else {
        this.refrence -= 1
      }
    }
  }

}
