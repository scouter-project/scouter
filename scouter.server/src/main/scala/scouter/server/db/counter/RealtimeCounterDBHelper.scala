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

import java.io.File
import java.util
import java.util.Enumeration
import java.util.Iterator
import java.util.Map
import scouter.lang.value.MapValue
import scouter.lang.value.Value
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.db.DBCtr
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.IntKeyMap
import scouter.util.StringIntMap
import scouter.server.util.EnumerScala

import scala.collection.mutable.ArrayBuffer

object RealtimeCounterDBHelper {
    def setTagBytes(tag: IntKeyMap[String], buff: Array[Byte]): MapValue = {
        if (buff == null)
            return null;
        val items = new MapValue();
        try {
            val in = new DataInputX(buff)
            val count = in.readDecimal().toInt

            for (i <- 0 to count - 1) {
                val idx = in.readDecimal().toInt;
                val name = tag.get(idx);
                val value = in.readValue();
                items.put(name, value);
            }
        } catch {
            case e: Exception =>
        }
        return items;
    }

    def setTagBytesMulti(tag: IntKeyMap[String], buff: Array[Byte]): ArrayBuffer[MapValue] = {
        if (buff == null)
            return null;
        val arrBuffer = new ArrayBuffer[MapValue]();

        try {
            val in = new DataInputX(buff)
            val length = buff.length
            while(in.getOffset() < length) {
                in.skipBytes(4)
                val items = new MapValue();
                val count = in.readDecimal().toInt

                for (i <- 0 to count - 1) {
                    val idx = in.readDecimal().toInt;
                    val name = tag.get(idx);
                    val value = in.readValue();
                    items.put(name, value);
                }
                arrBuffer += items
            }
        } catch {
            case e: Exception =>
        }
        return arrBuffer
    }


    def getTagBytes(tagMap: StringIntMap, items: MapValue): Array[Byte] = {
        if (tagMap.size() == 0)
            return null;

        val out = new DataOutputX();
        try {
            out.writeDecimal(items.size());

            EnumerScala.foreach(items.keys(), (name: String) => {
                val value = items.get(name);
                val idx = tagMap.get(name);
                if (idx >= 0) {
                    out.writeDecimal(idx);
                    out.writeValue(value);
                }
            })

        } catch {
            case e: Exception =>
        }
        return out.toByteArray();
    }
}
class RealtimeCounterDBHelper extends IClose {
    var counterDbHeader: RealtimeCounterDBHeader = null
    var counterIndex: RealtimeCounterIndex = null
    var counterData: RealtimeCounterData = null
    var currentDateUnit = 0L
    var activeTime = 0L

    var path: String = null

    private def getDBPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date).append("/counter");
        return sb.toString();
    }

    def open(date: String, readOnly: Boolean): RealtimeCounterDBHelper = {
        path = getDBPath(date)
        val f = new File(path);
        if (readOnly) {
            if (f.exists() == false)
                return null;
        } else {
            if (f.exists() == false)
                f.mkdirs();
        }
        val file = path + "/real";

        this.counterDbHeader = RealtimeCounterDBHeader.open(file);
        this.counterIndex = RealtimeCounterIndex.open(file);
        this.counterData = RealtimeCounterData.open(file);
        this.activeTime = System.currentTimeMillis();

        return this;
    }
    def getKey(objName: String): String = {
        return objName;
    }

    override def close() {
        FileUtil.close(counterIndex);
        FileUtil.close(counterData);
        FileUtil.close(counterDbHeader);
        counterIndex = null;
        counterData = null;
    }

    def close(dbs: Map[String, RealtimeCounterDBHelper]) {
        val itr = dbs.values().iterator();
        while (itr.hasNext()) {
            itr.next().close();
        }

    }

}