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

package scouter.server.db.io;

import java.io.IOException
import java.util.ArrayList
import java.util.TreeSet
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.Logger
import scouter.server.util.EnumerScala
import scouter.util.BytesUtil
import scouter.util.CompareUtil
import scouter.util.DateUtil
import scouter.util.IClose

class IndexTimeFile(_path: String) extends IClose {

    class TimeValue(_time: Long, _value: Array[Byte]) extends Comparable[TimeValue] {
        val time = _time
        val value = _value

        override def compareTo(t: TimeValue): Int = {
            val v = this.time - t.time;
            if (v == 0) {
                return CompareUtil.compareTo(this.value, t.value);
            }
            return if (v > 0) 1 else -1
        }

        override def equals(obj: Any): Boolean = {
            if (obj.isInstanceOf[TimeValue]) {
                return compareTo(obj.asInstanceOf[TimeValue]) == 0;
            } else false
        }
        override def hashCode(): Int = {
            return time.toInt
        }
        override def toString(): String = {
            return DateUtil.timestamp(time) + " byte[" + BytesUtil.getLength(value) + "]";
        }
    }

    protected var path = _path
    protected var hashFile = new MemTimeBlock(_path);
    protected var keyFile = new RealKeyFile(_path);

    def put(time: Long, value: Array[Byte]): Long = {
        if (time <= 0 || value == null) {
            throw new IOException("invalid key/value");
        }
        var pos = hashFile.get(time);
        pos = this.keyFile.append(pos, DataOutputX.toBytes(time), value);
        this.hashFile.put(time, pos);
        this.hashFile.addCount(1);
        return pos;
    }

    private def getSecAll(time: Long): ArrayList[TimeValue] = {
        if (time <= 0) {
            throw new IOException("invalid key");
        }
        val map = new TreeSet[TimeValue]();
        var pos = hashFile.get(time);
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val m = this.keyFile.getRecord(pos);
                map.add(new TimeValue(DataInputX.toLong(m.key, 0), m.value));
            }
            pos = this.keyFile.getHashLink(pos);
        }
        return new ArrayList[TimeValue](map);
    }

    def getDirect(pos: Long): TimeValue = {

        if (this.keyFile.isDeleted(pos) == false) {
            val m = this.keyFile.getRecord(pos);
            return new TimeValue(DataInputX.toLong(m.key, 0), m.value);
        }
        return null;
    }

    def delete(time: Long): Int = {
        if (time <= 0) {
            throw new IOException("invalid key");
        }
        this.synchronized {
            var pos = hashFile.get(time);

            var deleted = 0;
            while (pos > 0) {
                if (this.keyFile.isDeleted(pos) == false) {
                    this.keyFile.setDelete(pos, true);
                    deleted += 1;
                }
                pos = this.keyFile.getHashLink(pos);
            }
            this.hashFile.put(time, 0);
            this.hashFile.addCount(-deleted);
            return deleted;
        }
    }

    def read(_stime: Long, etime: Long, handler: (Long, Array[Byte]) => Any) {
        if (this.keyFile == null)
            return ;

        var i = 0
        var stime = _stime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(stime);
            EnumerScala.forward(data, (tv: TimeValue) => {
                handler(tv.time, tv.value)
            })
            i += 1
            stime = stime + 500L
        }
    }

    def readFromEnd(stime: Long, _etime: Long, handler: (Long, Array[Byte]) => Any) {
        if (this.keyFile == null)
            return

        var i = 0
        var etime = _etime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(etime);

            EnumerScala.backward(data, (tv: TimeValue) => {
                handler(tv.time, tv.value)
            })
            i += 1
            etime = etime - 500L
        }
    }

    def read(_stime: Long, etime: Long, handler: (Long, Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
        if (this.keyFile == null)
            return

        var i = 0
        var stime = _stime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(stime);

            EnumerScala.forward(data, (tv: TimeValue) => {
                if (tv.time >= _stime && tv.time <= etime) {
                    handler(tv.time, reader(DataInputX.toLong5(tv.value, 0)))
                }
            })

            i += 1
            stime +=  500L
        }
    }

    def readFromEnd(stime: Long, _etime: Long, handler: (Long, Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
        if (this.keyFile == null)
            return

        var i = 0
        var etime = _etime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(etime);

            EnumerScala.backward(data, (tv: TimeValue) => {
                  if (tv.time >= stime && tv.time <= _etime) {
                      handler(tv.time, reader(DataInputX.toLong5(tv.value, 0)))
                  }
            })
            
            i += 1
            etime = etime - 500L
        }
    }

    def read(handler: (Array[Byte], Array[Byte]) => Any) {
        if (this.keyFile == null)
            return

        var pos = this.keyFile.getFirstPos();
        val length = this.keyFile.getLength();
        var done = 0;
        try {
            while (pos < length) {
                val r = this.keyFile.getRecord(pos);
                if (r.deleted == false) {
                    handler(r.key, r.value)
                }
                done += 1;
                pos = r.next;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S127", this.keyFile.path + " : read=" + done + " pos=" + pos + " file-len=" + length + " "
                    + t);
        }
    }

    override def close() {
        hashFile.close();
        keyFile.close();
    }

}
