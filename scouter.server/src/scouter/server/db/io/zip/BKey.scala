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
 */

package scouter.server.db.io.zip;

class BKey(_date: String, _blockNum: Int) {
    val date = _date
    val blockNum = _blockNum;
    override def hashCode() = blockNum ^ (if (date == null) 0 else date.hashCode());
    override def equals(obj: Any): Boolean = {
        if (obj != null && obj.isInstanceOf[BKey]) {
            val other = obj.asInstanceOf[BKey]
            if (this.date == null) return other.date == null && this.blockNum == other.blockNum;
            else return this.date.equals(other.date) && this.blockNum == other.blockNum;
        }
        return false;
    }

}