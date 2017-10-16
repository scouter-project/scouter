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


@SuppressWarnings("serial")
abstract public class NumberValue extends Number implements Value {
	public NumberValue add(NumberValue num) {
		if (num == null)
			return this;
		switch (this.getValueType()) {
		case ValueEnum.DECIMAL:
			((DecimalValue) this).value += num.longValue();
			return this;
		case ValueEnum.FLOAT:
			((FloatValue) this).value += num.floatValue();
			return this;
		case ValueEnum.DOUBLE:
			((DecimalValue) this).value += num.doubleValue();
			return this;
		default:
			return new DoubleValue(this.doubleValue() + num.doubleValue());
		}
	}
	
	abstract public double doubleValue();
	abstract public float floatValue();
	abstract public int intValue();
	abstract public long longValue();

}