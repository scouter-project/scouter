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

package scouter.server.db.counter;

import java.io.IOException
import java.io.RandomAccessFile
import java.util.HashMap
import java.util.Hashtable
import java.util.Iterator
import java.util.Map
import java.util.Map.Entry
import java.util.Set
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.IntKeyMap
import scouter.lang.ref.INT
import scouter.server.util.EnumerScala
import scouter.util.StringIntMap
import scouter.util.StringIntMap.StringIntEntry

object RealtimeCounterDBHeader {
    val table = new Hashtable[String, RealtimeCounterDBHeader]();

    def open(file: String): RealtimeCounterDBHeader = {
        table.synchronized {
            var reader = table.get(file);
            if (reader != null) {
                reader.refrence += 1;
                return reader;
            } else {
                reader = new RealtimeCounterDBHeader(file);
                table.put(file, reader);
                return reader;
            }
        }
    }

}

class RealtimeCounterDBHeader(file: String) extends IClose {

    private val NAME_POS = 512L
    private val HEADER_SIZE = 2048

    var refrence = 0;
    val tagIntStr = new IntKeyMap[String]();
    val tagStrInt = new StringIntMap().setNullValue(-1);

    override def close() {
        RealtimeCounterDBHeader.table.synchronized {
            if (this.refrence == 0) {
                RealtimeCounterDBHeader.table.remove(this.file);
            } else {
                this.refrence -= 1;
            }
        }
    }

    var raf: RandomAccessFile = null
    try {
        raf = openHeaderFile()
        val len = DataInputX.toInt(getBytes(raf, NAME_POS, 4), 0);
        if (len > 0) {
            val buffer = getBytes(raf, NAME_POS + 4, len);
            val din = new DataInputX(buffer);
            val count = din.readInt();
            for (i <- 0 to count - 1) {
                val key = din.readText();
                val value = din.readInt();
                tagStrInt.put(key, value);
                tagIntStr.put(value, key);
            }
        }
    } catch {
        case e: Throwable => e.printStackTrace();
    } finally {
        FileUtil.close(raf);
    }

    def getBytes(raf: RandomAccessFile, pos: Long, len: Int): Array[Byte] = {
        val buffer = new Array[Byte](len)
        raf.seek(pos);
        raf.read(buffer);
        return buffer;
    }

    def intern(name: String, save: Boolean): Int = {
        var idx = tagStrInt.get(name);
        if (idx >= 0)
            return idx;
        idx = tagStrInt.size() + 1
        tagIntStr.put(idx, name);
        tagStrInt.put(name, idx);
        // /
        if (save) {
            saveTags();
        }
        // /
        return idx;
    }

    def intern(names: Set[String]) {
        val sz = tagIntStr.size()
        EnumerScala.foreach(names.iterator(), (z: String) => { intern(z, false) })

        if (tagIntStr.size() != sz) {
            saveTags();
        }
    }

    private def saveTags() {
        try {
            val out = new DataOutputX();
            out.writeInt(tagIntStr.size());

            EnumerScala.foreach(tagStrInt.entries(), (ent: StringIntEntry) => {
                out.writeText(ent.getKey());
                out.writeInt(ent.getValue());
            });
            val outBytes = out.toByteArray();

            var raf: RandomAccessFile = null;
            try {
                raf = openHeaderFile();
                raf.seek(NAME_POS);
                raf.write(DataOutputX.toBytes(outBytes.length));
                raf.write(outBytes)
            } finally {
                FileUtil.close(raf);
            }

        } catch {
            case t: Throwable =>
        }
    }

    private def openHeaderFile(): RandomAccessFile = {
        val raf = new RandomAccessFile(file + ".head", "rw");
        val len = raf.length().toInt
        if (len < HEADER_SIZE) {
            raf.seek(len);
            raf.write(new Array[Byte](HEADER_SIZE + len));
        }
        return raf;
    }

    def getTagIntStr(): IntKeyMap[String] = {
        tagIntStr;
    }

    def getTagStrInt(): StringIntMap = {
        tagStrInt;
    }
}