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

package scouter.lang;

import scouter.util.IntKeyMap;
import scouter.util.ObjectUtil;
import scouter.util.StringIntMap;

public class TimeTypeEnum {
	public final static byte REALTIME = 1;
	public final static byte ONE_MIN = 2;
	public final static byte FIVE_MIN = 3;
	public final static byte TEN_MIN = 4;
	public final static byte HOUR = 5;
	public final static byte DAY = 6;

	private static final IntKeyMap<String> lookup = new IntKeyMap<String>();
	private static final StringIntMap lookname = new StringIntMap();

	static {
		add("REALTIME", REALTIME);
		add("ONE_MIN", ONE_MIN);
		add("FIVE_MIN", FIVE_MIN);
		add("TEN_MIN", TEN_MIN);
		add("HOUR", HOUR);
		add("DAY", DAY);

	}

	private static void add(String name, byte code) {
		lookup.put(code, name);
		lookname.put(name, code);
	}

	public static String get(byte code) {
		return lookup.get(code);
	}

	public static byte get(String name) {
		return (byte) lookname.get(name);
	}

	public static String getString(byte code) {
		return ObjectUtil.toString(lookup.get(code));
	}

	public static byte getCode(String name) {
		return get(name);
	}

	public static byte getCodeBySec(int sec) {
		if (sec < 60)
			return REALTIME;
		switch (sec) {
		case 60:
			return ONE_MIN;
		case 300:
			return FIVE_MIN;
		case 600:
			return TEN_MIN;
		case 3600:
			return HOUR;
		default:
			return DAY;
		}
	}

	public static int getTime(byte timeCode) {
		switch (timeCode) {
		case REALTIME:
			return 2000;
		case ONE_MIN:
			return 60 * 1000;
		case FIVE_MIN:
			return 300 * 1000;
		case HOUR:
			return 3600 * 1000;
		case DAY:
			return 24 * 3600 * 1000;
		}
		return 0;
	}

}