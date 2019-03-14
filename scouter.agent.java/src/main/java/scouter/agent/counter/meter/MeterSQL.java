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

package scouter.agent.counter.meter;

import scouter.lang.ref.INT;
import scouter.lang.ref.LONG;
import scouter.util.MeteringUtil;
import scouter.util.MeteringUtil.Handler;

public class MeterSQL {
	private static MeterSQL inst = new MeterSQL();

	public static MeterSQL getInstance() {
		return inst;
	}
	
	final static class Bucket {
		int count;
		long time;
		int error;
	}

	private MeteringUtil<Bucket> meter = new MeteringUtil<Bucket>() {
		protected Bucket create() {
			return new Bucket();
		};

		protected void clear(Bucket o) {
			o.count = 0;
			o.error = 0;
			o.time = 0L;
		}
	};

	public synchronized void add(int elapsed, boolean err) {
		Bucket b = meter.getCurrentBucket();
		b.count++;
		b.time += elapsed;
		if (err) {
			b.error++;
		}
	}

	public float getTps(int period) {
		final INT sum = new INT();
		period = meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.count;
			}
		});
		return (float) ((double) sum.value / period);
	}

	public int getAvgTime(int period) {
		final LONG sum = new LONG();
		final INT cnt = new INT();
		meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.time;
				cnt.value += b.count;

			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

	public long getSumTime(int period) {
		final LONG sum = new LONG();
		meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.time;
			}
		});
		return  sum.value;
	}
	

	public int getCount(int period) {

		final INT sum = new INT();
		meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.count;
			}
		});
		return sum.value;
	}

	public int getErrorCount(int period) {
		final INT sum = new INT();
		meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.error;
			}
		});
		return sum.value;
	}

	public float getErrorRate(int period) {
		final INT cnt = new INT();
		final INT err = new INT();
		meter.search(period, new Handler<MeterSQL.Bucket>() {
			public void process(Bucket b) {
				cnt.value += b.count;
				err.value += b.error;
			}
		});
		return (float) ((cnt.value == 0) ? 0 : (((double) err.value / cnt.value) * 100.0));
	}

}