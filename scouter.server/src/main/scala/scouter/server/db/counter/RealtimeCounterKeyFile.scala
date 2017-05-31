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

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.value.MapValue
import scouter.server.db.io.IndexTimeFile
import scouter.util.IntKeyMap

class RealtimeCounterKeyFile(path: String) extends IndexTimeFile(path) {

    def write(objHash: Int, time: Long, pos: Long) {
        val dout = new DataOutputX();
        dout.writeLong5(pos);
        dout.writeInt(objHash);
        put(time, dout.toByteArray());
    }

    def read(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Any,
        dataMap: IntKeyMap[String], reader: (Long) => Array[Byte]): Unit = {
        try {
            super.read(stime, etime, (time: Long, data: Array[Byte]) => {
                val in = new DataInputX(data);
                val pos = in.readLong5();
                val hash = in.readInt();
                if (stime <= time && time <= etime && hash == objHash) {
                    val items = RealtimeCounterDBHelper.setTagBytes(dataMap, reader(pos));
                    handler(time, items)
                }
            });
        } catch {
            case t: Throwable =>
        }
    }

    def readFromEnd(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Any,
        dataMap: IntKeyMap[String], reader: (Long) => Array[Byte]): Unit = {
        try {

            super.readFromEnd(stime, etime, (time: Long, data: Array[Byte]) => {
                val in = new DataInputX(data);

                val pos = in.readLong5();
                val hash = in.readInt();

                if (stime <= time && time <= etime && hash == objHash) {
                    val items = RealtimeCounterDBHelper.setTagBytes(dataMap, reader(pos));
                    handler(time, items)
                }
            });
        } catch {
            case t: Throwable =>
        }
    }

}