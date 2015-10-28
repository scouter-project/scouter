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
 */

package scouter.server.tagcnt.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.value.ListValue;

class ValueCountTotal(_howManyValues: Int, _totalCount: Float, _values: List[ValueCount]) {
    var howManyValues = _howManyValues
    var totalCount = _totalCount
    var values = _values

    def this() {
        this(0, 0, new ArrayList[ValueCount])
    }

    def toByteArray(): Array[Byte] = {
        val out = new DataOutputX();
        out.writeInt(howManyValues);
        out.writeFloat(totalCount);

        val max = if (values == null) 0 else values.size();
        out.writeInt(max);
        var inx = 0
        while (inx < max) {
            val value = values.get(inx);
            out.writeValue(value.tagValue);
            out.writeDouble(value.valueCount);
            inx += 1
        }
        return out.toByteArray();
    }

    def toObject(b: Array[Byte]): ValueCountTotal = {
        if (b == null)
            return null;
        val din = new DataInputX(b);
        this.howManyValues = din.readInt();
        this.totalCount = din.readFloat();
        this.values = new ArrayList[ValueCount]();

        val max = din.readInt();
        var inx = 0
        while (inx < max) {
            val tagValue = din.readValue();
            val valueCount = din.readDouble();
            this.values.add(new ValueCount(tagValue, valueCount));
            inx += 1
        }
        return this;
    }
}