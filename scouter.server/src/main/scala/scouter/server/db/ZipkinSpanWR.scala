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

import scouter.server.core.ServerStat
import scouter.server.db.span.{ZipkinSpanDataWriter, ZipkinSpanIndex}
import scouter.server.util.{OftenAction, ThreadScala}
import scouter.server.{Configure, Logger}
import scouter.util.{DateUtil, FileUtil, RequestQueue, ThreadUtil}

import scala.collection.mutable


object ZipkinSpanWR {
    case class SpanData(time: Long, gxid: Long, data: Array[Byte])
    case class StorageContainer(idleLimit: Long, lastAccess: Long, index: ZipkinSpanIndex, writer: ZipkinSpanDataWriter)

    val MAX_IDLE = 30 * 60 * 1000L
    val SPAN_DIR = "/span"
    val SPAN_PREFIX = "span"

    val queue = new RequestQueue[SpanData](Configure.getInstance().span_queue_size)
    val dailyContainer = mutable.Map[Long, StorageContainer]()

    ThreadScala.start("scouter.server.db.ZipkinSpanDataFileWatcher") {
        while (DBCtr.running) {
            ThreadUtil.sleep(5 * 60 * 1000)
            val now = System.currentTimeMillis()
            dailyContainer
                    .filter(kv => now - kv._2.lastAccess > kv._2.idleLimit)
                    .keys.foreach(k => close(k))
        }
    }

    ThreadScala.start("scouter.server.db.ZipkinSpanWR") {
        while (DBCtr.running) {
            val m = queue.get()

            ServerStat.put("zipkin-span.db.queue", queue.size())
            try {
                val currentDateUnit = DateUtil.getDateUnit(m.time)
                val container = dailyContainer.getOrElseUpdate(currentDateUnit, {
                    val (index, writer) = open(m.time)
                    StorageContainer(MAX_IDLE, System.currentTimeMillis(), index, writer)
                })

                if (container.index == null) {
                    OftenAction.act("ZipkinSpanWR", 10) {
                        dailyContainer.remove(currentDateUnit)
                        queue.clear()
                    }
                    Logger.println("SZ143", 10, "can't open ZipkinSpanWR")

                } else {
                    val location = container.writer.write(m.data)
                    container.index.setByTime(m.time, location)
                    container.index.setByGxid(m.gxid, location)
                }

            } catch {
                case t: Throwable => t.printStackTrace()
            }
        }
        closeAll()
    }

    def add(time: Long, gid: Long, data: Array[Byte]): Unit = {
        val ok = queue.put(SpanData(time, gid, data))
        if (!ok) {
            Logger.println("SZ144", 10, "queue exceeded!! - ZipkinSpanWR")
        }
    }

    def closeAll(): Unit = {
        dailyContainer.values.foreach (container => {
            FileUtil.close(container.index)
            FileUtil.close(container.writer)
        })
        dailyContainer.clear()
    }

    def close(time: Long): Unit = {
        dailyContainer.get(time).foreach(container => {
            FileUtil.close(container.index)
            FileUtil.close(container.writer)
        })
        dailyContainer.remove(time)
    }

    def open(time: Long): (ZipkinSpanIndex, ZipkinSpanDataWriter) = {
        val date = DateUtil.yyyymmdd(time)

        try {
            val path = getDBPath(date)
            val f = new File(path)
            if (!f.exists()) f.mkdirs()
            val file = path + "/" + SPAN_PREFIX
            val index = ZipkinSpanIndex.open(file)
            val writer = ZipkinSpanDataWriter.open(date, file)

            return (index, writer)

        } catch {
            case e: Throwable => {
                e.printStackTrace()
                close(time)
            }
        }

        (null, null)
    }

    def getDBPath(date: String): String = {
        val sb = new StringBuffer()
        sb.append(DBCtr.getRootPath())
        sb.append("/").append(date).append(SPAN_DIR)
        sb.toString
    }
}
