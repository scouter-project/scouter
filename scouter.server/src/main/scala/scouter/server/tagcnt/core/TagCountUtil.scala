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

import scouter.util.DateUtil;

 object TagCountUtil {
    
    val BUCKET_SIZE = 1440

    def getBucketPos(hhmm: Int): Int = {
        return (((hhmm / 100) * 60 + hhmm % 100)).toInt;
    }

    def getBucketPos(hh: Int, mm: Int): Int = {
        return ((hh * 60 + mm)).toInt
    }

    def hhmm(time: Long): Int = {
        var dtime = DateUtil.getTimeUnit(time) % DateUtil.MILLIS_PER_DAY;
        val hh = (dtime / DateUtil.MILLIS_PER_HOUR);
        dtime = (dtime % DateUtil.MILLIS_PER_HOUR);
        val mm = (dtime / DateUtil.MILLIS_PER_MINUTE);
        return hh.toInt * 100 + mm.toInt;
    }

    def check(count: Array[Float]) {
        if (count == null || count.length != BUCKET_SIZE)
            throw new RuntimeException("invalid count");
    }

    def sum(cnt: Array[Float]): Float = {
        if (cnt == null)
            return 0;
        var tot = 0.0f;
        var i = 0
        while (i < cnt.length) {
            tot += cnt(i);
            i += 1
        }
        return tot;
    }

}
