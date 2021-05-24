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

package scouter.lang.pack;

import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;


/**
 * Pack type enum.
 * use less number than 100 as a pack type.(over 100 for extensions)
 */
public abstract class PackEnum {
    private static PackEnum extPackEnum; // extension pack

	public final static byte MAP = 10;
	public final static byte XLOG = 21;
	public final static byte DROPPED_XLOG = 22;
	public final static byte XLOG_PROFILE = 26;
	public final static byte XLOG_PROFILE2 = 27;

	public final static byte SPAN = 31;
    public final static byte SPAN_CONTAINER = 32;

	public final static byte TEXT = 50;
	public final static byte PERF_COUNTER = 60;
	public final static byte PERF_STATUS = 61;
	public final static byte STACK = 62;
	public final static byte SUMMARY = 63;
	public final static byte BATCH = 64;
    public final static byte PERF_INTERACTION_COUNTER = 65;

	public final static byte ALERT = 70;
	public final static byte OBJECT = 80;

    public static Pack create(byte packType) {
        Pack pack = createNonExt(packType);
        if(pack == null) {
            if(extPackEnum != null) {
            	pack = extPackEnum.createExt(packType);
            }
            if(pack == null) {
            	throw new RuntimeException("Unknown pack type= " + packType);
            }
        }
        return pack;
	}

    public static Pack createNonExt(byte packType) {
        switch (packType) {
            case MAP:
                return new MapPack();
            case PERF_COUNTER:
                return new PerfCounterPack();
            case PERF_STATUS:
                return new StatusPack();
            case XLOG_PROFILE:
                return new XLogProfilePack();
	        case XLOG_PROFILE2:
		        return new XLogProfilePack2();
            case XLOG:
                return new XLogPack();
	        case DROPPED_XLOG:
		        return new DroppedXLogPack();
            case TEXT:
                return new TextPack();
            case ALERT:
                return new AlertPack();
            case OBJECT:
                return new ObjectPack();
            case STACK:
                return new StackPack();
            case SUMMARY:
                return new SummaryPack();
            case BATCH:
            	return new BatchPack();
            case PERF_INTERACTION_COUNTER:
                return new InteractionPerfCounterPack();
            case SPAN:
                return new SpanPack();
            case SPAN_CONTAINER:
                return new SpanContainerPack();
            default:
                return null;
        }
    }

    public abstract Pack createExt(byte PackType);

    /**
     * add ext pack
     * @param packEnum
     */
    public static synchronized void registPackEnum(PackEnum packEnum) {
        extPackEnum = packEnum;
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
