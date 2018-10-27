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

package scouter.server.db.span

import java.io.{File, IOException, RandomAccessFile}
import java.util

import scouter.util.{FileUtil, IClose}

/**
  * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
  */

object ZipkinSpanDataReader {
    val table = new util.Hashtable[String, ZipkinSpanDataReader]();

    def open(date: String, file: String): ZipkinSpanDataReader = {
        table.synchronized {
            var reader = table.get(file)
            if (reader != null) {
                reader.ref += 1
            } else {
                reader = new ZipkinSpanDataReader(date, file)
                table.put(file, reader)
            }
            reader
        }
    }

}

class ZipkinSpanDataReader(date: String, file: String) extends IClose {

    var ref = 0
    var pointFile: RandomAccessFile = _

    val dataFile = new File(file + ".data")
    if (dataFile.canRead) {
        this.pointFile = new RandomAccessFile(dataFile, "r")
    }

    def read(point: Long): Array[Byte] = {
        if (pointFile == null) return null

        try {
            this.synchronized {
                pointFile.seek(point)
                val len = pointFile.readShort()
                val buffer = new Array[Byte](len)
                pointFile.read(buffer)
                buffer
            }

        } catch {
            case e: IOException =>
                throw new RuntimeException(e);
        }
    }

    override def close(): Unit = {
        ZipkinSpanDataReader.table.synchronized {
            if (this.ref == 0) {
                ZipkinSpanDataReader.table.remove(this.file)
                pointFile = FileUtil.close(pointFile)
            } else {
                this.ref -= 1
            }
        }
    }

}