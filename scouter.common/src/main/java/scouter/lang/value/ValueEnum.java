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

public class ValueEnum {
	public final static byte NULL = 0;
	public final static byte BOOLEAN = 10;
	public final static byte DECIMAL = 20;
	public final static byte FLOAT = 30;
	public final static byte DOUBLE = 40;
	
	public final static byte DOUBLE_SUMMARY = 45;
	public final static byte LONG_SUMMARY = 46;

	public final static byte TEXT = 50;
	public final static byte TEXT_HASH = 51;
	public final static byte BLOB = 60;
	public final static byte IP4ADDR = 61;
	
	public final static byte LIST = 70;
	
	public final static byte ARRAY_INT = 71;
	public final static byte ARRAY_FLOAT = 72;
	public final static byte ARRAY_TEXT = 73;
	public final static byte ARRAY_LONG = 74;
	
	public final static byte MAP = 80;
	
	public static Value create(byte code) {
		switch (code) {
		case NULL:
			return new NullValue();
		case BOOLEAN:
			return new BooleanValue();
		case DECIMAL:
			return new DecimalValue();
		case FLOAT:
			return new FloatValue();
		case DOUBLE:
			return new DoubleValue();
		case TEXT:
			return new TextValue();
		case TEXT_HASH:
			return new TextHashValue();
		case BLOB:
			return new BlobValue();

		case IP4ADDR:
			return new IP4Value();

		case LIST:
			return new ListValue();
		case MAP:
			return new MapValue();

		case LONG_SUMMARY:
			return new LongSummary();
		case DOUBLE_SUMMARY:
			return new DoubleSummary();

			
		case ARRAY_INT:
			return new IntArray();
		case ARRAY_FLOAT:
			return new FloatArray();
		case ARRAY_TEXT:
			return new TextArray();
		case ARRAY_LONG:
			return new LongArray();

			
		default:
			throw new RuntimeException("unknown value type=" + code);
		}
	}

	public Value toValue(Object o) {
		if (o == null)
			return new NullValue();
		else if (o instanceof Value)
			return (Value) o;
		else if (o instanceof String)
			return new TextValue((String) o);
		else if (o instanceof Number) {
			Number n = (Number) o;
			if (n instanceof Double)
				return new DoubleValue((Double) o);
			if (n instanceof Float)
				return new FloatValue((Float) o);
			return new DecimalValue(n.longValue());
		} else
			return new TextValue(o.toString());
	}

}