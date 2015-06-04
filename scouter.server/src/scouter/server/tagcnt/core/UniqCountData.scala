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
package scouter.server.tagcnt.core;

import scouter.server.tagcnt.uniq.UniqTagCountDB;

class UniqCountData {

    private var time = 0L
    private var objType = ""
    private var tagKey = 0L
    private var hour = 0

    def addFirstOccur(hour: Int, minCount: Array[Int]) {
        var m = 0
        while (m < 60) {
            if (minCount(m) > 0) {
                UniqTagCountDB.add(time, objType, tagKey, hour * 60 + m, 1);
            }
            m += 1
        }
    }

    def addMin(inx: Int) {
        UniqTagCountDB.add(time, objType, tagKey, hour * 60 + inx, 1);
    }

    def setWorkingLog(time: Long, objType: String) {
        this.time = time;
        this.objType = objType;

    }

    def setTag(tagKey: Long) {
        this.tagKey = tagKey;
    }

    def setHour(hour: Int) {
        this.hour = hour;
    }

   def reset() {
        this.time = 0;
        this.objType = null;
        this.tagKey = 0;
        this.hour = 0;
    }

}
