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

import scouter.lang.value.Value;
import scouter.util.DateUtil;
import scouter.util.HashUtil;

class NextTagCountData(_time: Long, _objType: String, _tagKey: Long, _value: Value) {

    val objType = _objType;
    val time = _time;
    val hourUnit = DateUtil.getHourUnit(time);
    val tagKey = _tagKey;
    val value = _value;

    override def hashCode(): Int = {
        return (objType.hashCode() ^ hourUnit).toInt ^ (tagKey ^ (tagKey >>> 32)).toInt ^ value.hashCode();
    }

    override def equals(obj: Any): Boolean = {
        if (obj == null) return false
        if (obj.isInstanceOf[NextTagCountData]) {
            val other = obj.asInstanceOf[NextTagCountData]
            return objType.equals(other.objType) && hourUnit == other.hourUnit && tagKey == other.tagKey && value.equals(other.value);
        }
        return false;
    }
}
