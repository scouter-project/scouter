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

import java.util

import scouter.server.db.io.RealDataFile
import scouter.util.{FileUtil, IClose}

/**
  * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
  */
object ZipkinSpanDataWriter {
    val table = new util.Hashtable[String, ZipkinSpanDataWriter]()

    def open(date: String, file: String): ZipkinSpanDataWriter = {
        table.synchronized {
            var writer = table.get(file)
            if (writer != null) {
                writer.ref += 1
            } else {
                writer = new ZipkinSpanDataWriter(date, file)
                table.put(file, writer)
            }
            writer
        }
    }
}

class ZipkinSpanDataWriter(date: String, filePathName: String) extends IClose {
    var ref = 0
    val out: RealDataFile = new RealDataFile(filePathName + ".data")

    def write(bytes: Array[Byte]): Long = {
        this.synchronized {
            val point = out.getOffset()
            out.writeShort(bytes.length.toShort)
            out.write(bytes)
            out.flush()
            point
        }
    }

    override def close(): Unit = {
        ZipkinSpanDataWriter.table.synchronized {
            if (this.ref == 0) {
                ZipkinSpanDataWriter.table.remove(this.filePathName)
                FileUtil.close(out)
            } else {
                this.ref -= 1
            }
        }
    }
}
