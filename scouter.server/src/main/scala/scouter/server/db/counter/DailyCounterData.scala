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

package scouter.server.db.counter

import java.io.IOException
import java.io.RandomAccessFile
import java.util.Hashtable

import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.value.Value
import scouter.lang.value.ValueEnum
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.util.FileUtil
import scouter.util.IClose


object DailyCounterData {
    val table = new Hashtable[String, DailyCounterData]();
    val preFixForWriter = "w--";

    def openForWrite(fileName: String): DailyCounterData = {
        val wFileName = preFixForWriter + fileName;
        table.synchronized {
            var writer = table.get(wFileName);
            if (writer != null) {
                writer.refrence += 1
            } else {
                writer = new DailyCounterData(fileName, "rw")
                table.put(wFileName, writer);
            }
            return writer
        }
    }

    def open(fileName: String): DailyCounterData = {
        table.synchronized {
            var reader = table.get(fileName)
            if (reader != null) {
                reader.refrence += 1
            } else {
                reader = new DailyCounterData(fileName, "r")
                table.put(fileName, reader)
            }
            return reader
        }
    }
}

class DailyCounterData(fileName: String, mode: String) extends IClose {
    var refrence = 0;
    var dataFile = new RandomAccessFile(fileName + ".data", mode);

    override def close() {
        DailyCounterData.table.synchronized {
            if (this.refrence == 0) {
                DailyCounterData.table.remove(this.fileName);
                DailyCounterData.table.remove(DailyCounterData.preFixForWriter + this.fileName);
                dataFile = FileUtil.close(dataFile);
            } else {
                this.refrence -= 1;
            }
        }
    }

    def closeForce() {
        DailyCounterData.table.synchronized {
            DailyCounterData.table.remove(this.fileName);
            DailyCounterData.table.remove(DailyCounterData.preFixForWriter + this.fileName);
            dataFile = FileUtil.close(dataFile);
            this.refrence = 0;
        }
    }

    def read(offset: Long): Array[Byte] = {
        this.synchronized {
            try {
                dataFile.seek(offset);
                val valueType = dataFile.readByte();
                val timetype = dataFile.readByte()

                val valueLen = DailyCounterUtils.getLength(valueType);
                val bucketCount = DailyCounterUtils.getBucketCount(timetype);

                dataFile.seek(offset + 2);
                val buffer = new Array[Byte](valueLen * bucketCount);
                dataFile.read(buffer);
                return buffer;
            } catch {
                case e: IOException =>
                    throw new RuntimeException(e);
            }
        }
    }

    def getValues(offset: Long): Array[Value] = {
        this.synchronized {
            try {
                dataFile.seek(offset);
                val valueType = dataFile.readByte();
                val timetype = dataFile.readByte()

                val valueLen = DailyCounterUtils.getLength(valueType);
                val bucketCount = DailyCounterUtils.getBucketCount(timetype);

                val buffer = new Array[Byte](valueLen * bucketCount)
                dataFile.read(buffer);

                val values = new Array[Value](bucketCount)
                for (i <- 0 to values.length - 1) {
                    values(i) = new DataInputX(buffer, i * valueLen).readValue();
                }
                return values;
            } catch {
                case e: Exception =>
                    throw new RuntimeException(e);
            }
        }
    }

    def getValue(offset: Long, hhmm: Int): Value = {
        this.synchronized {
            try {
                dataFile.seek(offset);
                val valueType = dataFile.readByte();
                val intervalType = dataFile.readByte()

                val valueLen = DailyCounterUtils.getLength(valueType);
                val bucketCount = DailyCounterUtils.getBucketCount(intervalType);
                val bucketPos = DailyCounterUtils.getBucketPos(intervalType, hhmm);
                if (bucketPos < bucketCount) {
                    dataFile.seek(offset + 2 + valueLen * bucketPos);
                    val buffer = new Array[Byte](valueLen);
                    dataFile.read(buffer);
                    return new DataInputX(buffer).readValue();
                }
                return null;
            } catch {
                case e: IOException =>
                    throw new RuntimeException(e);
            }
        }
    }

    def write(offset: Long, key: CounterKey, hhmm: Int, value: Value) {
        dataFile.seek(offset);
        val valueType = dataFile.readByte();
        if (valueType != value.getValueType() && value.getValueType() != ValueEnum.NULL)
            return;
        val timetype = dataFile.readByte()
        if (timetype != key.timetype)
            return;

        val valueLen = DailyCounterUtils.getLength(valueType);
        val bucketPos = DailyCounterUtils.getBucketPos(timetype, hhmm);

        dataFile.seek(offset + 2 + bucketPos * valueLen);
        dataFile.write(new DataOutputX().writeValue(value).toByteArray());

    }

    def writeNew(key: CounterKey, hhmm: Int, value: Value): Long = {
        val valueType = value.getValueType();

        val valueLen = DailyCounterUtils.getLength(valueType);
        if (valueLen <= 0)
            return 0;
        val bucketCount = DailyCounterUtils.getBucketCount(key.timetype);
        if (bucketCount <= 0)
            return 0;

        val bucketPos = DailyCounterUtils.getBucketPos(key.timetype, hhmm);

        val location = dataFile.length();
        dataFile.seek(location);
        dataFile.writeByte(value.getValueType());
        dataFile.writeByte(key.timetype);
        dataFile.write(new Array[Byte](valueLen * bucketCount)); //fill 1 day data with blank bytes

        dataFile.seek(location + 2 + bucketPos * valueLen);
        dataFile.write(new DataOutputX().writeValue(value).toByteArray());
        return location;
    }
}