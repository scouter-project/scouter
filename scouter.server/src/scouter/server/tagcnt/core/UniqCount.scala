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

class UniqCount {
    private val uc1440 = new Array[Int](1440);

    def set(values: Array[Int]) {
        if (values == null)
            return ;
        System.arraycopy(values, 0, uc1440, 0, 1440);
    }

    def add(s1440: Array[Int]) {
        if (s1440 == null)
            return ;
        var inx = 0;
        while (inx < 1440) {
            if (s1440(inx) > 0) {
                uc1440(inx) += 1;
            }
            inx += 1
        }
    }

    def getResult(): Array[Int] = {
        return uc1440;
    }
}