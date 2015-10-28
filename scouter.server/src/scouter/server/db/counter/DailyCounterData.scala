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


object DailyCounterData  {
	val table = new Hashtable[String, DailyCounterData]();
	
	def  open( file:String) :DailyCounterData={
		table.synchronized {
			var reader = table.get(file);
			if (reader != null) {
				reader.refrence+=1;
			} else {
				reader = new DailyCounterData(file);
				table.put(file, reader);
			}
				return reader;
		}
	}

}
 class DailyCounterData(file:String) extends IClose {
	

   var refrence = 0;
	var  dataFile = new RandomAccessFile(file + ".data", "rw");
	

	override def close() {
		DailyCounterData.table.synchronized  {
			if (this.refrence == 0) {
				DailyCounterData.table.remove(this.file);
				try {
					dataFile = FileUtil.close(dataFile);
				} catch  {
				    case e:Throwable=>
					e.printStackTrace();
				}
			} else {
				this.refrence-=1;
			}
		}

	}

	def read( pos:Long):Array[Byte]= {
	    this.synchronized{
		try {
			dataFile.seek(pos);
			val valueType = dataFile.readByte();
			val timetype = dataFile.readByte()

			val valueLen = DailyCounterUtils.getLength(valueType);
			val bucketCount = DailyCounterUtils.getBucketCount(timetype);

			dataFile.seek(pos + 2);
			val buffer = new Array[Byte](valueLen * bucketCount);
			dataFile.read(buffer);
			return buffer;
		} catch{
		    case e:IOException=>
			throw new RuntimeException(e);
		}
	    }
	}

	def getValues( location:Long):Array[Value]= {
	    this.synchronized{
		try {
			dataFile.seek(location);
			val valueType = dataFile.readByte();
			val timetype = dataFile.readByte()

			val valueLen = DailyCounterUtils.getLength(valueType);
			val bucketCount = DailyCounterUtils.getBucketCount(timetype);

			val buffer = new Array[Byte](valueLen * bucketCount)
			dataFile.read(buffer);

			val values = new Array[Value](bucketCount)
			for ( i <- 0 to values.length-1) {
				values(i) = new DataInputX(buffer, i*valueLen).readValue();
			}
			return values;
		} catch  {
		    case e:IOException=>
			throw new RuntimeException(e);
		}
	}}

	def  getValue( location:Long, hhmm:Int):Value= {
	    this.synchronized{
		try {
			dataFile.seek(location);
			val valueType = dataFile.readByte();
			val intervalType = dataFile.readByte()

			val valueLen = DailyCounterUtils.getLength(valueType);
			val bucketCount = DailyCounterUtils.getBucketCount(intervalType);
			val bucketPos = DailyCounterUtils.getBucketPos(intervalType, hhmm);
			if (bucketPos < bucketCount) {
				dataFile.seek(location + 2 + valueLen * bucketPos);
				val buffer = new Array[Byte](valueLen);
				dataFile.read(buffer);
				return new DataInputX(buffer).readValue();
			}
			return null;
		} catch  {
		    case e:IOException=>
			throw new RuntimeException(e);
		}
	    }
	}

	def write( location:Long,  key:CounterKey,  hhmm:Int,  value:Value)  {
		dataFile.seek(location);
		val valueType = dataFile.readByte();
		if (valueType != value.getValueType() && value.getValueType() != ValueEnum.NULL)
			return;
		val timetype = dataFile.readByte()
		if (timetype != key.timetype)
			return;

		val valueLen = DailyCounterUtils.getLength(valueType);
		val bucketPos = DailyCounterUtils.getBucketPos(timetype, hhmm);

		dataFile.seek(location + 2 + bucketPos * valueLen);
		dataFile.write(new DataOutputX().writeValue(value).toByteArray());

	}

	def  write( key:CounterKey,  hhmm:Int,  value:Value):Long= {
        //파일에 존재하지 않는 레코드...
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
		dataFile.write(new Array[Byte](valueLen * bucketCount)); // 하룻치데이터 전체를 먼저 공백으로 기록한다.

		dataFile.seek(location + 2 + bucketPos * valueLen);
		dataFile.write(new DataOutputX().writeValue(value).toByteArray());
		return location;
	}

}