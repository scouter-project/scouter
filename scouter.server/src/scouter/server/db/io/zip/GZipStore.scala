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
package scouter.server.db.io.zip;

import java.io.IOException
import java.util.Enumeration
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.ShutdownManager
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.IClose
import scouter.util.IShutdown
import scouter.util.LinkedMap
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scala.util.control.Breaks._

object GZipStore extends Thread with IClose with IShutdown {

    ShutdownManager.add(this)

    Logger.println("S128", "COMPRESS MODE ENABLED");

    class Key(_date: String, _unitNum: Int) {
        val date = _date
        val unitNum = _unitNum;

        override def hashCode(): Int = {
            return unitNum ^ date.hashCode();
        }

        override def equals(obj: Any): Boolean = {
            if (obj.isInstanceOf[Key]) {
                val o = obj.asInstanceOf[Key]
                return this.unitNum == o.unitNum && this.date.equals(o.date);
            }
            return false;
        }
    }

    var table = new LinkedMap[String, Block]();
    var brun = true;
    ThreadScala.start("GZipStore") {
        while (brun) {
            val now = System.currentTimeMillis();
            val en = table.keys();
            breakable {
                while (en.hasMoreElements()) {
                    {
                        val key = en.nextElement();
                        val bk = table.get(key);
                        if (bk == null)
                            break
                        if (now > bk.lastAccessTime + 10000 && bk.dirty) {
                            IOChannel.store(bk);
                            bk.setLastAccessTime(System.currentTimeMillis());
                        }
                    }
                }
            }
            for (i <- 0 to 10) {
                if (brun) ThreadUtil.sleep(100);
            }

        }
    }

    private val conf = Configure.getInstance();

    def write(date: String, data: Array[Byte]): Long = {
        return write(date, data, 0);
    }

    def write(date: String, data: Array[Byte], next: Long): Long = {
        this.synchronized {
            val dout = new DataOutputX();
            dout.writeLong5(next);
            dout.writeInt3(data.length);
            dout.write(data);
            val saveData = dout.toByteArray();
            var bk = table.get(date);
            if (bk == null) {
                bk = IOChannel.getLastWriteBlock(date);
                if (bk != null) {
                    while (table.size() >= conf.gzip_writing_block) {
                        val bb = table.removeFirst();
                        IOChannel.store(bb);
                    }
                    table.put(date, bk);
                } else {
                    System.err.println("ERROR -1 : write main data");
                    return -1;
                }
            }
            bk.setLastAccessTime(System.currentTimeMillis());
            try {
                var pos = bk.getOffset();
                var ok = bk.write(saveData);
                if (ok) {
                    return pos;
                }
                IOChannel.store(bk);
                bk = bk.createNextBlock();
                table.put(date, bk);
                pos = bk.getOffset();
                ok = bk.write(saveData);
                return if (ok) pos else -1;
            } catch {
                case ee: Throwable =>
                    System.out.println(ee.toString() + " => " + bk);
                    ee.printStackTrace();
                    return -1;
            }
        }
    }

    private def getReadBlock(date: String, blockNum: Int, pos: Long): Block = {
        val b = table.get(date);
        if (b == null)
            return null;
        if (b.blockNum == blockNum && b.readable(pos))
            return b;
        return null;
    }

    def read(date: String, _pos: Long): Array[Byte] = {
        var pos = _pos
        if (pos < 0)
            return null;
        val out = new DataOutputX();
        while (GZipStore.brun) {
            val blockNum = (pos / GZipCtr.BLOCK_MAX_SIZE).toInt
            var bk = getReadBlock(date, blockNum, pos);
            if (bk == null) bk = IOChannel.getReadBlock(date, blockNum);
            if (bk == null) return out.toByteArray();
            
            try {
                val next = DataInputX.toLong5(bk.read(pos, 5), 0);
                val len = DataInputX.toInt3(bk.read(pos + 5, 3), 0);
                val record = bk.read(pos + 8, len);
                out.write(record);
                if (next <= 0)
                    return out.toByteArray();
                else
                    pos = next;
            } catch {
                case ne: NullPointerException => // end of file then just return Array[Byte]
                    return out.toByteArray();
                case t: Throwable =>
                    t.printStackTrace();
                    return out.toByteArray();
            }
        }
        return null
    }

    def flush() {
        if (table.size() == 0)
            return ;
        val blocks = table;
        table = new LinkedMap[String, Block]();
        while (blocks.size() > 0) {
            val bk = blocks.removeFirst();
            IOChannel.store(bk);
        }
    }

    def shutdown() {
        this.brun = false;
        close();
    }

    def close() {
        this.flush();
    }

    def close(date: String) {
        val bb = table.remove(date);
        IOChannel.store(bb);
        IOChannel.close(date);
    }

}
