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

package scouter.server.db.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import scouter.io.DataOutputX;
import scouter.util.IClose;

class RealDataFile(_filename: String) extends IClose {
    val filename = _filename;
    private var offset = new File(filename).length();
    private val out = new DataOutputX(new BufferedOutputStream(new FileOutputStream(filename, true), 8192));

    def getOffset(): Long = {
        this.synchronized {
            return offset;
        }
    }

    def writeShort(s: Short): Long = {
        this.synchronized {
            val idx = offset;
            offset += 2;
            out.writeShort(s);
            return idx;
        }
    }
    def writeInt(i: Int): Long = {
        this.synchronized {
            val idx = offset;
            offset += 4;
            out.writeInt(i);
            return idx;
        }
    }

    def write(data: Array[Byte]): Long = {
        this.synchronized {
            val idx = offset;
            offset += data.length;
            out.write(data);
            return idx;
        }
    }

    override def close() {
        try {
            if (out != null)
                out.close();
        } catch {
            case e: Exception =>
        }
    }

    def flush() {
        out.flush();
    }
}