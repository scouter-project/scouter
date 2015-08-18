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

package scouter.lang.pack;

import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;


public class PackEnum {
	public final static byte MAP = 10;
	public final static byte XLOG = 21;
	public final static byte XLOG_PROFILE = 26;
	public final static byte TEXT = 50;
	public final static byte PERF_COUNTER = 60;
	public final static byte PERF_STATUS = 61;
	public final static byte ALERT = 70;
	public final static byte OBJECT = 80;
	public final static byte STACK = 90;

	public static Pack create(byte p) {
		switch (p) {
		case MAP:
			return new MapPack();
		case PERF_COUNTER:
			return new PerfCounterPack();
		case PERF_STATUS:
			return new StatusPack();
		case XLOG_PROFILE:
			return new XLogProfilePack();
		case XLOG:
			return new XLogPack();
		case TEXT:
			return new TextPack();
		case ALERT:
			return new AlertPack();
		case OBJECT:
			return new ObjectPack();
		case STACK:
			return new StackPack();
		default:
			throw new RuntimeException("Unknown pack type= " + p);
		}
	}

	public static Value toValue(Object value) throws Exception {
		if (value == null) {
			return new NullValue();
		} else if (value instanceof String) {
			return new TextValue((String) value);
		} else if (value instanceof Number) {
			if (value instanceof Float)
				return new FloatValue((Float) value);
			else if (value instanceof Double)
				return new DoubleValue((Double) value);
			else
				return new DecimalValue(((Number) value).longValue());
		} else {
			return new TextValue(value.toString());
		}
	}
	// public static byte[] toBytes(Packet p) throws IOException {
	// if (p == null)
	// return null;
	// DataOutputX out = new DataOutputX();
	// out.writePacket(p);
	// return out.toByteArray();
	// }

}