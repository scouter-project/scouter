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

import scouter.lang.ref.INT;
import scouter.lang.ref.LONG;
import scouter.util.MeteringUtil;
import scouter.util.MeteringUtil.Handler;

class MeterService(_group: Int) {
    val group = _group

    class Bucket {
        var count = 0
        var error = 0
        var time = 0L
    }

    val meter = new MeteringUtil[Bucket]() {
        protected def create(): Bucket = {
            return new Bucket();
        };

        protected def clear(o: Bucket) {
            o.count = 0;
            o.error = 0;
            o.time = 0L;
        }
    };

    def add(elapsed: Long, err: Boolean) {
        this.synchronized {
            val b = meter.getCurrentBucket();
            b.count += 1;
            b.time += elapsed;
            if (err)
                b.error += 1;
        }
    }

    def getTPS(period: Int): Float = {
        var sum = 0
        val p = meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                sum += b.count;
            }
        });
        return (sum.toDouble / period).toFloat;
    }

    def getElapsedTime(period: Int): Int = {
        var sum = 0L
        var cnt = 0
        meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                sum += b.time;
                cnt += b.count;
            }
        });
        return if (cnt == 0) 0 else (sum / cnt).toInt
    }

    def getError(period: Int): Float = {
        var cnt = 0
        var err = 0
        meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                cnt += b.count;
                err += b.error;
            }
        });
        return if (cnt == 0) 0 else ((err.toFloat / cnt) * 100.0).toFloat;
    }

    def getServiceCount(period: Int): Int = {

        var sum = 0
        meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                sum += b.count;
            }
        });
        return sum;
    }

    def getServiceError(period: Int): Int = {
        var sum = 0
        meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                sum += b.error;
            }
        });
        return sum;
    }

    def getPerfStat(period: Int): PerfStat = {
        val stat = new PerfStat();
        meter.search(period, new Handler[Bucket]() {
            def process(b: Bucket) {
                stat.count += b.count;
                stat.elapsed += b.time;
                stat.error += b.error;
            }
        });
        return stat;
    }

}