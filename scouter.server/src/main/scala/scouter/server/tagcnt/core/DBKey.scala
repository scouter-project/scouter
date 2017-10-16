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

class DBKey(_dateUnit: Long, _objType: String) {

    val dateUnit = _dateUnit
    val objType = _objType

    override def equals(obj: Any): Boolean = {
        if (obj == null)
            return false;
        if (obj.isInstanceOf[DBKey]) {
            val o = obj.asInstanceOf[DBKey];
            return o.dateUnit == this.dateUnit && this.objType.equals(o.objType);
        }
        return false;
    }
    override def hashCode(): Int = {
        return objType.hashCode() ^ dateUnit.toInt
    }

    override def toString(): String = {
        return objType + "," + dateUnit;
    }

}
