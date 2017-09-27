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

package scouter.server.db;

import java.io.File
import java.util.List
import java.util.Vector
import scouter.io.DataOutputX
import scouter.server.db.xlog.XLogDataReader
import scouter.server.db.xlog.XLogIndex
import scouter.util.FileUtil
import scouter.server.db.io.IndexTimeFile

object XLogRD {

    /**
      * read xlog in limited count in time
      */
    def readByTimeLimitCount(date: String, fromTime: Long, toTime: Long, lastBucketTime: Long, limitCount: Int, handler: (Long, Array[Byte]) => Int) {
        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead()) {
            val file = path + "/" + XLogWR.prefix;
            var reader: XLogDataReader = null;
            var table: IndexTimeFile = null;
            try {
                reader = XLogDataReader.open(date, file);
                table = new IndexTimeFile(file + XLogIndex.POSTFIX_TIME);
                table.readByLimitCount(fromTime, toTime, lastBucketTime, limitCount, handler, reader.read)
            } catch {
                case e: Exception => e.printStackTrace()
                case _ :Throwable=>
            } finally {
                FileUtil.close(table);
                FileUtil.close(reader);
            }
        }
    }

    def readByTime(date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any) {
        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead()) {
            val file = path + "/" + XLogWR.prefix;
            var reader: XLogDataReader = null;
            var table: IndexTimeFile = null;
            try {
                reader = XLogDataReader.open(date, file);
                table = new IndexTimeFile(file + XLogIndex.POSTFIX_TIME);
                table.read(fromTime, toTime, handler, reader.read)
            } catch {
                case e: Exception => e.printStackTrace()
                case _ :Throwable=>
            } finally {
                FileUtil.close(table);
                FileUtil.close(reader);
            }
        }
    }

    def readFromEndTime(date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any) {

        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead()) {
            val file = path + "/" + XLogWR.prefix;
            var reader: XLogDataReader = null;
            var table: IndexTimeFile = null;
            try {
                reader = XLogDataReader.open(date, file);
                table = new IndexTimeFile(file + XLogIndex.POSTFIX_TIME);
                table.readFromEnd(fromTime, toTime, handler, reader.read)
            } catch {
                case e: Throwable => //e.printStackTrace();
            } finally {
                FileUtil.close(table);
                FileUtil.close(reader);
            }
        }
    }

    def getByTxid(date: String, txid: Long): Array[Byte] = {
        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val file = path + "/" + XLogWR.prefix;
        var fpos = 0L
        var idx: XLogIndex = null;
        try {
            idx = XLogIndex.open(file);
            fpos = idx.getByTxid(txid);
        } catch {
            case e: Exception => return null;
        } finally {
            FileUtil.close(idx);
        }
        if (fpos < 0)
            return null;
        var reader: XLogDataReader = null;
        try {
            reader = XLogDataReader.open(date, file);
            return reader.read(fpos);
        } catch {
            case e: Exception => return null;
        } finally {
            FileUtil.close(reader);
        }
    }

    def readByGxid(date: String, gxid: Long, handler: (Array[Byte], Array[Byte]) => Any) {

        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/" + XLogWR.prefix;

        var result: java.util.List[Long] = null;
        var idx: XLogIndex = null;
        try {
            idx = XLogIndex.open(file);
            result = idx.getByGxid(gxid);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
        }
        if (result == null || result.size() == 0)
            return ;

        val gidb = DataOutputX.toBytes(gxid);
        var reader: XLogDataReader = null;
        try {
            reader = XLogDataReader.open(date, file);

            for (i <- 0 to result.size() - 1) {
                handler(gidb, reader.read(result.get(i)));
            }
        } catch {
            case e: Throwable => e.printStackTrace();
        } finally {
            FileUtil.close(reader);
        }
    }

    def readByTxid(date: String, handler: (Array[Byte], Array[Byte]) => Any) {

        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/" + XLogWR.prefix;

        var idx: XLogIndex = null;
        var reader: XLogDataReader = null;
        try {
            idx = XLogIndex.open(file);
            reader = XLogDataReader.open(date, file);
            idx.readByTxid(handler, reader.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
            FileUtil.close(reader);
        }

    }

    def readByGxid(date: String, handler: (Array[Byte], Array[Byte]) => Any) {

        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/" + XLogWR.prefix;

        var idx: XLogIndex = null;
        var reader: XLogDataReader = null;
        try {
            idx = XLogIndex.open(file);
            reader = XLogDataReader.open(date, file);

            idx.readByGxid(handler, reader.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
            FileUtil.close(reader);
        }

    }

    def getByGxid(date: String, guid: Long): Vector[Array[Byte]] = {

        var vector = new Vector[Array[Byte]]()
        val path = XLogWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val file = path + "/" + XLogWR.prefix;
        var result: java.util.List[Long] = null;
        var idx: XLogIndex = null;
        try {
            idx = XLogIndex.open(file);
            result = idx.getByGxid(guid);
            if (result.size() == 0)
                return null;
        } catch {
            case e: Exception => return null;
        } finally {
            FileUtil.close(idx);
        }

        var reader: XLogDataReader = null;
        try {
            reader = XLogDataReader.open(date, file);
            for (i <- 0 to result.size() - 1) {
                val buff = reader.read(result.get(i).longValue());
                if (buff != null) {
                    vector.add(buff);
                }
            }
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(reader);
        }

        return vector;
    }

}
