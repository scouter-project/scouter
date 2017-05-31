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

package scouter.lang.value;

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class LongSummary extends SummaryValue implements Value {

	public long sum;
	public int count;
	public long min;
	public long max;

	public byte getValueType() {
		return ValueEnum.LONG_SUMMARY;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeLong(sum);
		out.writeInt(count);
		out.writeLong(min);
		out.writeLong(max);
	}

	public Value read(DataInputX in) throws IOException {
		this.sum = in.readLong();
		this.count = in.readInt();
		this.min = in.readLong();
		this.max = in.readLong();
		return this;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[sum=").append(sum);
		sb.append(",count=").append(count);
		sb.append(",min=").append(min);
		sb.append(",max=").append(max);
		sb.append("]");
		return sb.toString();
	}

	public Object toJavaObject() {
		return this;
	}
	
	public void addcount() {
		this.count++;
	}	

	public SummaryValue add(Number value) {
		if(value==null)
			return this;
		if (this.count == 0) {
			this.sum = value.longValue();
			this.count = 1;
			this.max = value.longValue();
			this.min = value.longValue();
		} else {
			this.sum += value.doubleValue();
			this.count++;
			this.max = Math.max(this.max, value.longValue());
			this.min = Math.min(this.min, value.longValue());
		}
		return this;
	}

	public SummaryValue add(SummaryValue other) {
		if (other == null || other.getCount() == 0)
			return this;
		
		this.count += other.getCount();
		this.sum += other.longSum();
		this.min = Math.min(this.min, other.longMin());
		this.max = Math.max(this.max, other.longMax());
		return this;
	}

	public long longSum() {
		return this.sum;
	}

	public long longMin() {
		return this.min;
	}

	public long longMax() {
		return this.max;
	}

	public long longAvg() {
		return count == 0 ? 0 : sum / count;
	}

	public double doubleSum() {
		return this.sum;
	}

	public double doubleMin() {
		return this.min;
	}

	public double doubleMax() {
		return this.max;
	}

	public double doubleAvg() {
		return count == 0 ? 0 : sum / count;
	}

	public int getCount() {
		return this.count;
	}
}