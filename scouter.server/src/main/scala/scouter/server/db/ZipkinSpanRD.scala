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

package scouter.server.db

import java.io.File
import java.util
import java.util.Vector

import scouter.io.DataOutputX
import scouter.server.db.io.IndexTimeFile
import scouter.server.db.span.{ZipkinSpanDataReader, ZipkinSpanIndex}
import scouter.util.FileUtil

import scala.collection.mutable

object ZipkinSpanRD {

    /**
      * read zipkin spans in limited count in time
      */
    def readByTimeLimitCount(date: String, fromTime: Long, toTime: Long, lastBucketTime: Long, limitCount: Int, handler: (Long, Array[Byte]) => Int): Unit = {
        val path = ZipkinSpanWR.getDBPath(date)

        if (new File(path).canRead()) {
            val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX
            var reader: ZipkinSpanDataReader = null
            var table: IndexTimeFile = null
            try {
                reader = ZipkinSpanDataReader.open(date, file)
                table = new IndexTimeFile(file + ZipkinSpanIndex.POSTFIX_TIME)
                table.readByLimitCount(fromTime, toTime, lastBucketTime, limitCount, handler, reader.read)

            } catch {
                case e: Exception => e.printStackTrace()
                case _ :Throwable=>

            } finally {
                FileUtil.close(table)
                FileUtil.close(reader)
            }
        }
    }

    def readByTime(date: String, fromTime: Long, toTime: Long, handler: (Long, Array[Byte]) => Any): Unit = {
        val path = ZipkinSpanWR.getDBPath(date)

        if (new File(path).canRead()) {
            val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX
            var reader: ZipkinSpanDataReader = null
            var table: IndexTimeFile = null
            try {
                reader = ZipkinSpanDataReader.open(date, file)
                table = new IndexTimeFile(file + ZipkinSpanIndex.POSTFIX_TIME)
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
        val path = ZipkinSpanWR.getDBPath(date)

        if (new File(path).canRead()) {
            val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX
            var reader: ZipkinSpanDataReader = null
            var table: IndexTimeFile = null
            try {
                reader = ZipkinSpanDataReader.open(date, file)
                table = new IndexTimeFile(file + ZipkinSpanIndex.POSTFIX_TIME)
                table.readFromEnd(fromTime, toTime, handler, reader.read)

            } catch {
                case e: Throwable => //e.printStackTrace();

            } finally {
                FileUtil.close(table);
                FileUtil.close(reader);
            }
        }
    }

    def readByGxid(date: String, gxid: Long, handler: (Array[Byte], Array[Byte]) => Any): Unit = {

        val path = ZipkinSpanWR.getDBPath(date)
        if (!new File(path).canRead()) {
            return;
        }
        val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX

        var result: java.util.List[Long] = null
        var idx: ZipkinSpanIndex = null

        try {
            idx = ZipkinSpanIndex.open(file)
            result = idx.getByGxid(gxid)

        } catch {
            case e: Exception => e.printStackTrace()

        } finally {
            FileUtil.close(idx)
        }

        if (result == null || result.size() == 0) return;

        val gidb = DataOutputX.toBytes(gxid)
        var reader: ZipkinSpanDataReader = null
        try {
            reader = ZipkinSpanDataReader.open(date, file)

            for (i <- 0 until result.size()) {
                handler(gidb, reader.read(result.get(i)))
            }

        } catch {
            case e: Throwable => e.printStackTrace();

        } finally {
            FileUtil.close(reader)
        }
    }

    def readByGxid(date: String, handler: (Array[Byte], Array[Byte]) => Any): Unit = {
        val path = ZipkinSpanWR.getDBPath(date)

        if (!new File(path).canRead()) {
            return;
        }

        val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX

        var idx: ZipkinSpanIndex = null
        var reader: ZipkinSpanDataReader = null
        try {
            idx = ZipkinSpanIndex.open(file)
            reader = ZipkinSpanDataReader.open(date, file)

            idx.readByGxid(handler, reader.read)

        } catch {
            case e: Exception => e.printStackTrace();

        } finally {
            FileUtil.close(idx)
            FileUtil.close(reader)
        }

    }

    def getByGxid(date: String, guid: Long): List[Array[Byte]] = {

        var spanList = new util.ArrayList[Array[Byte]]()
        val path = ZipkinSpanWR.getDBPath(date)

        if (!new File(path).canRead()) {
            return null;
        }

        val file = path + "/" + ZipkinSpanWR.SPAN_PREFIX
        var result: java.util.List[Long] = null
        var idx: ZipkinSpanIndex = null
        try {
            idx = ZipkinSpanIndex.open(file)
            result = idx.getByGxid(guid)
            if (result.size() == 0) return null

        } catch {
            case e: Exception => return null;

        } finally {
            FileUtil.close(idx)
        }

        var reader: ZipkinSpanDataReader = null;
        try {
            reader = ZipkinSpanDataReader.open(date, file)
            for (i <- 0 until result.size()) {
                val buff = reader.read(result.get(i).longValue())
                if (buff != null) {
                    spanList.add(buff)
                }
            }

        } catch {
            case e: Exception => e.printStackTrace()

        } finally {
            FileUtil.close(reader)
        }

        import scala.collection.JavaConversions._
        spanList.toList
    }

}
