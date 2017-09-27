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

package scouter.server.db.io

;

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

import scala.collection.mutable.ListBuffer

/**
  * timestamp(long) based index file
  * @param _path Index File Path
  */
class IndexTimeFile(_path: String) extends IClose {

    protected var path = _path
    protected var timeBlockHash = new MemTimeBlock(_path);
    protected var keyFile = new RealKeyFile(_path);

    def put(time: Long, dataPos: Array[Byte]): Long = {
        if (time <= 0 || dataPos == null) {
            throw new IOException("invalid key/value");
        }
        var prevKeyPos = timeBlockHash.get(time);
        var newKeyPos = this.keyFile.append(prevKeyPos, DataOutputX.toBytes(time), dataPos);
        this.timeBlockHash.put(time, newKeyPos);
        this.timeBlockHash.addCount(1);
        return newKeyPos;
    }

    private def getSecAll(time: Long): ArrayList[TimeToData] = {
        if (time <= 0) {
            throw new IOException("invalid key");
        }
        val set = new TreeSet[TimeToData]();
        var pos = timeBlockHash.get(time);
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val record = this.keyFile.getRecord(pos);
                set.add(new TimeToData(DataInputX.toLong(record.timeKey, 0), record.dataPos));
            }
            pos = this.keyFile.getPrevPos(pos);
        }
        return new ArrayList[TimeToData](set);
    }

    private def getSecAllV2(time: Long): List[TimeToData] = {
        if (time <= 0) {
            throw new IOException("invalid key")
        }
        var buffer = new ListBuffer[TimeToData]()
        var pos = timeBlockHash.get(time)
        while (pos > 0) {
            if (this.keyFile.isDeleted(pos) == false) {
                val record = this.keyFile.getRecord(pos)
                buffer += new TimeToData(DataInputX.toLong(record.timeKey, 0), record.dataPos)
            }
            pos = this.keyFile.getPrevPos(pos)
        }

        return buffer.toList
    }

    private def getDataPosFirst(_stime: Long, _etime: Long): Array[Byte] = {
        if (_stime <= 0 || _etime <= 0) {
            throw new IOException("invalid key")
        }
        var stime = _stime
        var i = 0
        var dataPos = getDataPosFirst(stime)
        while (dataPos == null && i < DateUtil.SECONDS_PER_DAY * 2 && stime <= _etime) {
            stime += 500L
            dataPos = getDataPosFirst(stime)
            i += 1
        }
        return dataPos
    }

    private def getDataPosFirst(time: Long): Array[Byte] = {
        if (time <= 0) {
            throw new IOException("invalid key")
        }
        var pos = timeBlockHash.get(time)
        while (pos > 0) {
            val prevPos = this.keyFile.getPrevPos(pos)
            if(prevPos == 0) {
                return this.keyFile.getDataPos(pos)
            }
            pos = prevPos
        }
        return null
    }

    private def getDataPosLast(_stime: Long, _etime: Long): Array[Byte] = {
        if (_etime <= 0 || _stime <= 0) {
            throw new IOException("invalid key")
        }
        var etime = _etime
        var i = 0
        var dataPos = getDataPosLast(etime)
        while (dataPos == null && i < DateUtil.SECONDS_PER_DAY * 2 && _stime <= etime) {
            etime -= 500L
            dataPos = getDataPosLast(etime)
            i += 1
        }

        return dataPos
    }

    private def getDataPosLast(time: Long): Array[Byte] = {
        if (time <= 0) {
            throw new IOException("invalid key")
        }
        var pos = timeBlockHash.get(time)
        if(pos == 0) return null
        return this.keyFile.getDataPos(pos)
    }

    def getStartEndDataPos(stime: Long, etime: Long): (Array[Byte], Array[Byte]) = {
        var startDataPos = getDataPosFirst(stime, etime)
        var endDataPos = getDataPosLast(stime, etime)
        return (startDataPos, endDataPos)
    }

    def getDirect(pos: Long): TimeToData = {

        if (this.keyFile.isDeleted(pos) == false) {
            val m = this.keyFile.getRecord(pos);
            return new TimeToData(DataInputX.toLong(m.timeKey, 0), m.dataPos);
        }
        return null;
    }

    def delete(time: Long): Int = {
        if (time <= 0) {
            throw new IOException("invalid key");
        }
        this.synchronized {
            var pos = timeBlockHash.get(time);

            var deleted = 0;
            while (pos > 0) {
                if (this.keyFile.isDeleted(pos) == false) {
                    this.keyFile.setDelete(pos, true);
                    deleted += 1;
                }
                pos = this.keyFile.getPrevPos(pos);
            }
            this.timeBlockHash.put(time, 0);
            this.timeBlockHash.addCount(-deleted);
            return deleted;
        }
    }

    def read(_stime: Long, etime: Long, handler: (Long, Array[Byte]) => Any) {
        if (this.keyFile == null)
            return

        var i = 0
        var stime = _stime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(stime);
            EnumerScala.forward(data, (timeToData: TimeToData) => {
                handler(timeToData.time, timeToData.dataPos)
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

            EnumerScala.backward(data, (tv: TimeToData) => {
                handler(tv.time, tv.dataPos)
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

            EnumerScala.forward(data, (tv: TimeToData) => {
                if (tv.time >= _stime && tv.time <= etime) {
                    handler(tv.time, reader(DataInputX.toLong5(tv.dataPos, 0)))
                }
            })

            i += 1
            stime += 500L
        }
    }

    /**
      * read xlog index and invoke a data handler received.
      * @param stime
      * @param etime
      * @param limitCount
      * @param handler
      * @param reader
      * @return time last searched
      */
    def readByLimitCount(stime: Long, etime: Long, lastBucketTime: Long, limitCount: Int, handler: (Long, Array[Byte]) => Int, reader: (Long) => Array[Byte]) {
        if (this.keyFile == null)
            return

        var timeBucketTime = if(lastBucketTime != 0) lastBucketTime else stime
        var counted = 0
        var timeBucketCount = 0

        while (timeBucketCount < DateUtil.SECONDS_PER_DAY * 2 && timeBucketTime <= etime) {
            getSecAllV2(timeBucketTime).filter(tv => tv.time >= stime && tv.time <= etime).foreach(tv => {
                if(counted > limitCount) {
                    return
                }
                counted = handler(timeBucketTime, reader(DataInputX.toLong5(tv.dataPos, 0)))
            })

            timeBucketCount += 1
            timeBucketTime += 500L
        }
    }

    def readFromEnd(stime: Long, _etime: Long, handler: (Long, Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
        if (this.keyFile == null)
            return

        var i = 0
        var etime = _etime
        while (i < DateUtil.SECONDS_PER_DAY * 2 && stime <= etime) {
            val data = getSecAll(etime);

            EnumerScala.backward(data, (tv: TimeToData) => {
                if (tv.time >= stime && tv.time <= _etime) {
                    handler(tv.time, reader(DataInputX.toLong5(tv.dataPos, 0)))
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
                    handler(r.timeKey, r.dataPos)
                }
                done += 1;
                pos = r.offset;
            }
        } catch {
            case t: Throwable =>
                Logger.println("S127", this.keyFile.path + " : read=" + done + " pos=" + pos + " file-len=" + length + " "
                        + t);
        }
    }

    override def close() {
        timeBlockHash.close();
        keyFile.close();
    }

    class TimeToData(_time: Long, _dataPos: Array[Byte]) extends Comparable[TimeToData] {
        val time = _time
        val dataPos = _dataPos

        override def compareTo(t: TimeToData): Int = {
            val v = this.time - t.time;
            if (v == 0) {
                return CompareUtil.compareTo(this.dataPos, t.dataPos);
            }
            return if (v > 0) 1 else -1
        }

        override def equals(obj: Any): Boolean = {
            if (obj.isInstanceOf[TimeToData]) {
                return compareTo(obj.asInstanceOf[TimeToData]) == 0;
            } else false
        }

        override def hashCode(): Int = {
            return time.toInt
        }

        override def toString(): String = {
            return DateUtil.timestamp(time) + " byte[" + BytesUtil.getLength(dataPos) + "]";
        }
    }

}
