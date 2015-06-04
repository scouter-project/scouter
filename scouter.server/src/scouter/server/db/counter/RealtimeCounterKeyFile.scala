/*
*  Copyright 2015 LG CNS.
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
import java.util.Map
import scouter.server.db.TableReader
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.server.db.io.IndexTimeFile
import scouter.util.IntKeyMap

class RealtimeCounterKeyFile(path: String) extends IndexTimeFile(path) {

    def write(objHash: Int, time: Long, pos: Long) {
        val dout = new DataOutputX();
        dout.writeLong5(pos);
        dout.writeInt(objHash);
        put(time, dout.toByteArray());
    }

    def read(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Boolean,
        dataMap: IntKeyMap[String], reader: TableReader): Boolean = {
        try {
            super.read(stime, etime, (time: Long, data: Array[Byte]) => {
                try {
                    val in = new DataInputX(data);

                    val pos = in.readLong5();
                    val hash = in.readInt();

                    if (time < stime || etime < time) {
                        return true;
                    }

                    if (hash != objHash)
                        return true;

                    val items = RealtimeCounterDBHelper.setTagBytes(dataMap, reader.read(pos));
                    if (handler(time, items) == false) {
                        return false;
                    }
                    return true;
                } catch {
                    case t: Throwable => return false;
                }
            });
            return true;
        } catch {
            case t: Throwable => return false;
        }
    }

    def readFromEnd(objHash: Int, stime: Long, etime: Long, handler: (Long, MapValue) => Boolean,
        dataMap: IntKeyMap[String], reader: TableReader): Boolean = {
        try {

            super.readFromEnd(stime, etime, (time: Long, data: Array[Byte]) => {
                try {
                    val in = new DataInputX(data);

                    val pos = in.readLong5();
                    val hash = in.readInt();

                    if (time < stime || etime < time) {
                        return true;
                    }

                    if (hash != objHash)
                        return true;

                    val items = RealtimeCounterDBHelper.setTagBytes(dataMap, reader.read(pos));
                    if (handler(time, items) == false) {
                        return false;
                    }
                    return true;
                } catch {
                    case t: Throwable => return false;
                }
            });
            return true;
        } catch {
            case t: Throwable => return false;
        }
    }

}