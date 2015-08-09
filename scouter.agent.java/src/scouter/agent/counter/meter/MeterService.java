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

package scouter.agent.counter.meter;

import scouter.lang.ref.INT;
import scouter.lang.ref.LONG;
import scouter.util.IntBoundList;
import scouter.util.MeteringUtil;
import scouter.util.SortUtil;
import scouter.util.MeteringUtil.Handler;
import scouter.util.StringUtil;

public class MeterService {
	private static MeterService inst = new MeterService();

	public static MeterService getInstance() {
		return inst;
	}

	static class Bucket {
		final short[] underSec = new short[10];
		final short[] overSec = new short[20];

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
			for (int i = 0; i < 10; i++) {
				o.underSec[i] = 0;
				o.overSec[i] = 0;
			}
			for (int i = 10; i < 20; i++) {
				o.overSec[i] = 0;
			}
		}
	};

	public synchronized void add(int elapsed, boolean err) {
		Bucket b = meter.getCurrentBucket();
		b.count++;
		b.time += elapsed;
		if (err) {
			b.error++;
		}
		if (elapsed < 1000) {
			int x = (int) (elapsed / 100);
			b.underSec[x]++;
		} else if (elapsed < 20000) {
			int x = (int) (elapsed / 1000);
			b.overSec[x]++;
		}

	}

	public float getTPS(int period) {
		final INT sum = new INT();
		period = meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.count;
			}
		});
		return (float) ((double) sum.value / period);
	}

	public int getElapsedTime(int period) {
		final LONG sum = new LONG();
		final INT cnt = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.time;
				cnt.value += b.count;

			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

	public int getReponse90Pct(int period) {
		final LONG sum = new LONG();
		final INT cnt = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				int total = (int) (b.count * 0.9);
				if (total == 0)
					return;

				for (int i = 0; i < 10; i++) {
					if (total >= b.underSec[i]) {
						total -= b.underSec[i];
					} else {
						sum.value += i * 100;
						cnt.value++;
						return;
					}
				}
				for (int i = 1; i < 20; i++) {
					if (total >= b.overSec[i]) {
						total -= b.overSec[i];
					} else {
						sum.value += i * 1000;
						cnt.value++;
						return;
					}
				}
				sum.value += 20000;
				cnt.value++;
			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

	public float getError(int period) {

		final INT cnt = new INT();
		final INT err = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				cnt.value += b.count;
				err.value += b.error;
			}
		});
		return (float) ((cnt.value == 0) ? 0 : (((double) err.value / cnt.value) * 100.0));
	}

	public int getServiceCount(int period) {

		final INT sum = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.count;
			}
		});
		return sum.value;
	}

	public int getServiceError(int period) {
		final INT sum = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.error;
			}
		});
		return sum.value;
	}
}