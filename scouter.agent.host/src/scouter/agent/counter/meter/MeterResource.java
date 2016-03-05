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

import scouter.lang.ref.DOUBLE;
import scouter.lang.ref.INT;
import scouter.util.MeteringUtil;
import scouter.util.MeteringUtil.Handler;

public class MeterResource  {

	static class Bucket {
		double value;
		int count;
	}
	private MeteringUtil<Bucket> meter = new MeteringUtil<Bucket>() {
		protected Bucket create() {
			return new Bucket();
		};

		protected void clear(Bucket o) {
			o.value=0;
			o.count = 0;
		}
	};
	
	public synchronized void add(double value) {
		Bucket b = meter.getCurrentBucket();
		b.value += value;
		b.count++;
	}

	public double getAvg(int period) {
		final INT count = new INT();
		final DOUBLE sum = new DOUBLE();
		meter.search(period, new Handler<MeterResource.Bucket>() {
			public void process(Bucket u) {
				sum.value += u.value;
				count.value += u.count;
			}
		});
		return count.value == 0 ? 0 : sum.value / count.value;
	}

	public double getSum(int period) {
		final DOUBLE sum = new DOUBLE();
		meter.search(period, new Handler<MeterResource.Bucket>() {
			public void process(Bucket u) {
				sum.value += u.value;
			}
		});
		return sum.value;
	}

}