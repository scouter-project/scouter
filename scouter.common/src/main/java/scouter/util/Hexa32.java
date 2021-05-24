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

package scouter.util;

import scouter.extlib.com.google.common.primitives.UnsignedLongs;
import scouter.util.zipkin.HexCodec;

public class Hexa32 {

	private static final char PLUS = 'x';
	private static final char MINUS = 'z';

	public static String toUnsignedLongHex(long num) {
		return UnsignedLongs.toString(num, 16);
	}

	public static long fromUnsignedLongHex(String str) {
		return UnsignedLongs.parseUnsignedLong(str, 16);
	}

	public static String toString32(long num) {
		boolean minus = num < 0;
		if (minus) {
			if (num == Long.MIN_VALUE)
				return min;
			return MINUS + Long.toString(-num, 32);
		} else {
			if (num < 10)
				return Long.toString(num);
			else
				return PLUS + Long.toString(num, 32);
		}
	}

	private final static String min = "z8000000000000";

	public static long toLong32(String str) {
		if (str == null || str.length() == 0)
			return 0;

		switch (str.charAt(0)) {
		case MINUS:
			if (min.equals(str))
				return Long.MIN_VALUE;
			else
				return -1 * Long.parseLong(str.substring(1), 32);
		case PLUS:
			return Long.parseLong(str.substring(1), 32);
		default:
			return Long.parseLong(str);
		}
	}

	public static void main(String[] args) {
		System.out.println(toString32(792539709424970410L));
		System.out.println(toString32(-342343233040343034L));

		System.out.println(Hexa32.toLong32("z6eq8mqkdkpt7c"));
		System.out.println(Hexa32.toString32(100000001L));

		System.out.println(Long.toHexString(792539709424970410L));

		System.out.println("=================================================");
		System.out.println(Long.toHexString(-342343233040343034L));
		System.out.println(UnsignedLongs.toString(-342343233040343034L, 16));
		System.out.println(UnsignedLongs.parseUnsignedLong("fb3fc0a4b35f2006", 16));

		System.out.println("=================================================");
		System.out.println(HexCodec.lowerHexToUnsignedLong("fb3fc0a4b35f2006"));
	}
}
