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

public class MeterService {
	private static MeterService inst = new MeterService();

	public static MeterService getInstance() {
		return inst;
	}

	final static int PCT_MAX_TIME = 10000;
	final static int PCT_UNIT_TIME = 200;
	final static int PCT_BUCKET = PCT_MAX_TIME / PCT_UNIT_TIME;

	final static class Bucket {
		final short[] pct90 = new short[PCT_BUCKET];
		int count;
		long elapsedTime;
        long sqlTimeByService;
        long apiTmeByService;
        long queuingTime;

		int error;
	}

	private MeteringUtil<Bucket> meter = new MeteringUtil<Bucket>() {
		protected Bucket create() {
			return new Bucket();
		};

		protected void clear(Bucket o) {
			o.count = 0;
			o.error = 0;
			o.elapsedTime = 0L;
            o.sqlTimeByService = 0L;
            o.apiTmeByService = 0L;
            o.queuingTime = 0L;
			for (int i = 0; i < PCT_BUCKET; i++) {
				o.pct90[i] = 0;
			}
		}
	};

	public synchronized void add(long elapsed, int sqlTime, int apiTime, int queuingTime, boolean err) {
		if(elapsed < 0)
			elapsed = 0;

		Bucket b = meter.getCurrentBucket();
		b.count++;
		b.elapsedTime += elapsed;
        b.sqlTimeByService += sqlTime;
        b.apiTmeByService += apiTime;
        b.queuingTime += queuingTime;

		if (err) {
			b.error++;
		}
		if (elapsed < PCT_MAX_TIME) {
			int x = (int) (elapsed / PCT_UNIT_TIME);
			b.pct90[x]++;
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
				sum.value += b.elapsedTime;
				cnt.value += b.count;

			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

	public int getElapsed90Pct(int period) {
		final LONG sum = new LONG();
		final INT cnt = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				int total = (int) (b.count * 0.9);
				if (total == 0)
					return;

				for (int timeInx = 0; timeInx < PCT_BUCKET; timeInx++) {
					if (total >= b.pct90[timeInx]) {
						total -= b.pct90[timeInx];
					} else {
						sum.value += timeInx * PCT_UNIT_TIME;
						cnt.value++;
						return;
					}
				}
				sum.value += PCT_MAX_TIME;
				cnt.value++;
			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

    public int getSqlTime(int period) {
        final LONG sum = new LONG();
        final INT cnt = new INT();

        meter.search(period, new Handler<MeterService.Bucket>() {
            public void process(Bucket b) {
                sum.value += b.sqlTimeByService;
                cnt.value += b.count;

            }
        });
        return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
    }

    public int getApiTime(int period) {
        final LONG sum = new LONG();
        final INT cnt = new INT();

        meter.search(period, new Handler<MeterService.Bucket>() {
            public void process(Bucket b) {
                sum.value += b.apiTmeByService;
                cnt.value += b.count;

            }
        });
        return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
    }

	public int getQueuingTime(int period) {
		final LONG sum = new LONG();
		final INT cnt = new INT();
		meter.search(period, new Handler<MeterService.Bucket>() {
			public void process(Bucket b) {
				sum.value += b.queuingTime;
				cnt.value += b.count;

			}
		});
		return (int) ((cnt.value == 0) ? 0 : sum.value / cnt.value);
	}

	public float getErrorRate(int period) {

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
