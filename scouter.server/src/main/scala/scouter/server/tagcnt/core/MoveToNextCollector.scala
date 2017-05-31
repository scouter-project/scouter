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

import java.util.HashMap
import scouter.lang.value.Value
import scouter.server.Logger
import scouter.server.core.CoreRun
import scouter.server.tagcnt.next.NextTagCountDB
import scouter.util.DateUtil
import scouter.util.IShutdown
import scouter.util.RequestQueue
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala

object MoveToNextCollector {
    private val MAX_MASTER = 1000;
    private val MAX_QUE_SIZE = 30000;

    private val queue = new RequestQueue[CountItem](MAX_QUE_SIZE + 1);

    class CountItem(_time: Long, _objType: String, _tagKey: Long, _tagValue: Value, _count: Float) {
        val time = _time
        val objType = _objType
        val tagKey = _tagKey
        val tagValue = _tagValue
        val count = _count
    }

    def isQueueOk() = queue.size() < MAX_QUE_SIZE

    def add(time: Long, objType: String, tagKey: Long, tagValue: Value, count: Float) {
        while (isQueueOk() == false) {
            ThreadUtil.qWait();
            Logger.println("S182", 10, "queue is full");
        }
        queue.put(new CountItem(time, objType, tagKey, tagValue, count));
    }

    protected var countTable = new HashMap[NextTagCountData, Array[Float]]();

    ThreadScala.startDaemon("scouter.server.tagcnt.core.MoveToNextCollector") {
        while (CountEnv.running) {
            checkSaveToDb();
            val p = queue.get();

            val key = new NextTagCountData(p.time, p.objType, p.tagKey, p.tagValue);
            var minCountForHour = countTable.get(key);
            if (minCountForHour == null) {
                minCountForHour = new Array[Float](60);
                countTable.put(key, minCountForHour);
            }

            val min = ((p.time / DateUtil.MILLIS_PER_MINUTE) % 60).toInt
            minCountForHour(min) += p.count;
        }
    }
    private var last_save_time = System.currentTimeMillis();

    private def checkSaveToDb() {
        val now = System.currentTimeMillis();
        if (now > last_save_time + 5000 || countTable.size() > MAX_MASTER) {
            last_save_time = System.currentTimeMillis();
            if (countTable.size() > 0) {
                NextTagCountDB.add(countTable);
                countTable = new HashMap[NextTagCountData, Array[Float]]();
            }
        }
    }

}
