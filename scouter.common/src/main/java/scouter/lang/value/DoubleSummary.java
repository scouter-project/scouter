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
import java.text.DecimalFormat;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class DoubleSummary extends SummaryValue implements Value {

	public double sum;
	public int count;
	public double min;
	public double max;

	public byte getValueType() {
		return ValueEnum.DOUBLE_SUMMARY;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeDouble(sum);
		out.writeInt(count);
		out.writeDouble(min);
		out.writeDouble(max);
	}

	public Value read(DataInputX in) throws IOException {
		this.sum = in.readDouble();
		this.count = in.readInt();
		this.min = in.readDouble();
		this.max = in.readDouble();
		return this;
	}

	public String toString() {
		DecimalFormat fmt = new DecimalFormat("#0.0#################");
		StringBuffer sb = new StringBuffer();
		sb.append("[sum=").append(fmt.format(new Double(sum)));
		sb.append(",count=").append(count);
		sb.append(",min=").append(fmt.format(new Double(min)));
		sb.append(",max=").append(fmt.format(new Double(max)));
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
		if (value == null)
			return this;
		if (this.count == 0) {
			this.sum = value.doubleValue();
			this.count = 1;
			this.max = value.doubleValue();
			this.min = value.doubleValue();
		} else {
			this.sum += value.doubleValue();
			this.count++;
			this.max = Math.max(this.max, value.doubleValue());
			this.min = Math.min(this.min, value.doubleValue());
		}
		return this;
	}

	public SummaryValue add(SummaryValue other) {
		if (other == null || other.getCount() == 0)
			return this;
		this.count += other.getCount();
		this.sum += other.doubleSum();
		this.min = Math.min(this.min, other.doubleMin());
		this.max = Math.max(this.max, other.doubleMax());
		return this;
	}

	public long longSum() {
		return (long) this.sum;
	}

	public long longMin() {
		return (long) this.min;
	}

	public long longMax() {
		return (long) this.max;
	}

	public long longAvg() {
		return (long) (count == 0 ? 0 : sum / count);
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