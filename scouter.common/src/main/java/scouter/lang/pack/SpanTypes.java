/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.lang.pack;

public class SpanTypes {
	public final static byte CLIENT = 0;
	public final static byte SERVER = 1;
	public final static byte PRODUCER = 2;
	public final static byte CONSUMER = 3;
	public final static byte UNKNOWN = 99;

	public enum Type {
		CLIENT(SpanTypes.CLIENT),
		SERVER(SpanTypes.SERVER),
		PRODUCER(SpanTypes.PRODUCER),
		CONSUMER(SpanTypes.CONSUMER),
		UNKNOWN(SpanTypes.UNKNOWN),
		;

		byte value;

		Type(byte value) {
			this.value = value;
		}

		public static Type of(byte value) {
			for (Type type : Type.values()) {
				if (type.value == value) {
					return type;
				}
			}
			return UNKNOWN;
		}
	}

	public static boolean isXLoggable(byte typeByte) {
		SpanTypes.Type type = Type.of(typeByte);
		return type == Type.SERVER || type == Type.CONSUMER;
	}

	public static boolean isApiable(byte typeByte) {
		SpanTypes.Type type = Type.of(typeByte);
		return type == Type.CLIENT || type == Type.PRODUCER;
	}

	public static boolean isBoundary(byte typeByte) { // CLIENT, SERVER Share there ID.
		SpanTypes.Type type = Type.of(typeByte);
		return type == Type.SERVER || type == Type.CONSUMER || type == Type.CLIENT || type == Type.PRODUCER;
	}

	public static boolean isParentXLoggable(byte typeByte) { // CLIENT, SERVER Share there ID.
		SpanTypes.Type type = Type.of(typeByte);
		return type == Type.SERVER || type == Type.CONSUMER || type == Type.CLIENT || type == Type.PRODUCER;
	}

	public static boolean isXLoggable(SpanTypes.Type type) {
		return type == Type.SERVER || type == Type.CONSUMER;
	}

	public static boolean isApiable(SpanTypes.Type type) {
		return type == Type.CLIENT || type == Type.PRODUCER;
	}
}

