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

package scouter.server.core.app;

import java.util.ArrayList
import java.util.Date
import java.util.Enumeration
import java.util.Hashtable
import java.util.List
import java.util.Map
import java.util.Set
import java.util.Timer
import java.util.TimerTask
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.XLogPack
import scouter.lang.value.DecimalValue
import scouter.server.Logger
import scouter.server.core.CoreRun
import scouter.server.db.DailyCounterWR
import scouter.util.DateUtil
import scouter.util.LinkedMap
import scouter.util.RequestQueue
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scouter.server.util.EnumerScala
import scouter.util.IntKeyMap
import scouter.util.IntSet

object XLogGroupPerf {

    val timer = new Timer(true); // daemon

    class Key(_objHash: Int, _group: Int) {
        val objHash = _objHash
        val group = _group

        override def hashCode(): Int = {
            return group ^ objHash;
        }
        override def equals(obj: Any): Boolean = {
            if (obj == null)
                return false
            if (obj.isInstanceOf[Key]) {
                val other = obj.asInstanceOf[Key]
                return group == other.group && objHash == other.objHash
            }
            return false
        }

    }

    //  1분마다 실행되면서 최근 5분데이터를 저장한다.
    // 업데이트를 반복한다. 5m 카운터로 저장한다.
    val stime = (System.currentTimeMillis() / DateUtil.MILLIS_PER_MINUTE + 1) * DateUtil.MILLIS_PER_MINUTE;
    timer.scheduleAtFixedRate(new TimerTask() {
        def run() {
            save5m();
        }

    }, new Date(stime), DateUtil.MILLIS_PER_MINUTE);

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);

    def add(p: XLogPack) {
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S107", 10, "queue exceeded!!");
        }
    }

    ThreadScala.startDaemon("scouter.server.core.app.XLogGroupPerf") {
        while (CoreRun.running) {
            clearEmpty();
            val p = queue.get();
            try {
                process(p);
            } catch {
                case e: Exception =>
                    e.printStackTrace();
            }
        }
    }

    private var emptySet = new ArrayList[Key]();

    private def clearEmpty() {
        if (emptySet.size() == 0)
            return ;
        val list = emptySet;
        emptySet = new ArrayList[Key]();
        EnumerScala.forward(list, (k: Key) => { perfGroupTable.remove(k) })
    }

    private val perfGroupTable = new LinkedMap[Key, MeterService]().setMax(2000);

    private def process(p: XLogPack) {
        if (p.group == 0)
            return ;
        calcGroup(new Key(p.objHash, p.group), p.elapsed, p.error != 0);
    }

    private def save5m() {
        val now = System.currentTimeMillis() - 10000; // 10초 이전시간을 기준으로 저장
        val date = Integer.parseInt(DateUtil.yyyymmdd(now));
        val hhmm = Integer.parseInt(DateUtil.hhmm(now));

        EnumerScala.foreach(perfGroupTable.keys(), (key: Key) => {
            val m = perfGroupTable.get(key);
            if (m != null) {
                val p = m.getPerfStat(300);
                if (p.count == 0) {
                    emptySet.add(key);
                }
                var k = new CounterKey(key.objHash, "cnt:" + key.group, TimeTypeEnum.FIVE_MIN);
                DailyCounterWR.add(date, k, hhmm, new DecimalValue(p.count));
                k = new CounterKey(key.objHash, "tm:" + key.group, TimeTypeEnum.FIVE_MIN);
                DailyCounterWR.add(date, k, hhmm, new DecimalValue(p.elapsed));
                k = new CounterKey(key.objHash, "err:" + key.group, TimeTypeEnum.FIVE_MIN);
                DailyCounterWR.add(date, k, hhmm, new DecimalValue(p.error));
            }
        })

    }

    private var cachedPerfMap = new Hashtable[Key, PerfStat]();
    private var lastBuildCacheTime = System.currentTimeMillis();

    private def build(): Hashtable[Key, PerfStat] = {
        XLogGroupPerf.this.synchronized {
            if (System.currentTimeMillis() < lastBuildCacheTime + 1000)
                return cachedPerfMap;
            val p = new Hashtable[Key, PerfStat]();
            val en = perfGroupTable.keys();
            while (en.hasMoreElements()) {
                val key = en.nextElement();
                val m = perfGroupTable.get(key);
                if (m != null) {
                    p.put(key, m.getPerfStat(30));
                }
            }
            cachedPerfMap = p;
            lastBuildCacheTime = System.currentTimeMillis();
            return p;
        }
    }

    def getGroupPerfStat(objSet: IntSet): IntKeyMap[PerfStat] = {
        val buildPerfMap = build();
        val p = new IntKeyMap[PerfStat]();
        val en = buildPerfMap.keys();
        while (en.hasMoreElements()) {
            val key = en.nextElement();
            if (objSet != null && objSet.contains(key.objHash) == false) {}
            else {
                val m = buildPerfMap.get(key);
                var outPerf = p.get(key.group);
                if (outPerf == null) {
                    outPerf = new PerfStat();
                    p.put(key.group, outPerf);
                }
                outPerf.add(m);
            }
        }
        return p;
    }

    private def calcGroup(key: Key, elapsed: Long, error: Boolean): MeterService = {
        var meter = perfGroupTable.get(key);
        if (meter == null) {
            meter = new MeterService(key.group);
            perfGroupTable.put(key, meter);
        }
        meter.add(elapsed, error);
        return meter;
    }
}
