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

package scouter.lang.step;

import scouter.lang.TextTypes;

public class StepEnum {
	public final static byte METHOD = 1;
	public final static byte METHOD2 = 10;
	public final static byte SPAN = 51;
	public final static byte SQL = 2;
	public final static byte SQL2 = 8;
	public final static byte SQL3 = 16;
	public final static byte MESSAGE = 3;
	public final static byte SOCKET = 5;
	public final static byte APICALL = 6;
	public final static byte APICALL2 = 15;
	public final static byte SPANCALL = 52;
	public final static byte THREAD_SUBMIT = 7;
	public final static byte HASHED_MESSAGE = 9;
	public final static byte PARAMETERIZED_MESSAGE = 17;
	public final static byte DUMP = 12;
	public final static byte DISPATCH = 13;
	public final static byte THREAD_CALL_POSSIBLE = 14;

	public final static byte METHOD_SUM = 11;
	public final static byte SQL_SUM = 21;
	public final static byte MESSAGE_SUM = 31;
	public final static byte SOCKET_SUM = 42;
	public final static byte APICALL_SUM = 43;
	public final static byte CONTROL = 99;

	public enum Type {
		METHOD(StepEnum.METHOD, MethodStep.class, TextTypes.METHOD),
		METHOD2(StepEnum.METHOD2, MethodStep2.class, TextTypes.METHOD),
		SQL(StepEnum.SQL, SqlStep.class, TextTypes.SQL),
		SQL2(StepEnum.SQL2, SqlStep2.class, TextTypes.SQL),
		SQL3(StepEnum.SQL3, SqlStep3.class, TextTypes.SQL),
		MESSAGE(StepEnum.MESSAGE, MessageStep.class, null),
		SOCKET(StepEnum.SOCKET, SocketStep.class, null),
		APICALL(StepEnum.APICALL, ApiCallStep.class, TextTypes.APICALL),
		APICALL2(StepEnum.APICALL2, ApiCallStep2.class, TextTypes.APICALL),
		THREAD_SUBMIT(StepEnum.THREAD_SUBMIT, ThreadSubmitStep.class, TextTypes.APICALL),
		HASHED_MESSAGE(StepEnum.HASHED_MESSAGE, HashedMessageStep.class, TextTypes.HASH_MSG),
		PARAMETERIZED_MESSAGE(StepEnum.PARAMETERIZED_MESSAGE, ParameterizedMessageStep.class, TextTypes.HASH_MSG),
		DUMP(StepEnum.DUMP, DumpStep.class, null),
		DISPATCH(StepEnum.DISPATCH, DispatchStep.class, TextTypes.APICALL),
		THREAD_CALL_POSSIBLE(StepEnum.THREAD_CALL_POSSIBLE, ThreadCallPossibleStep.class, TextTypes.APICALL),
		METHOD_SUM(StepEnum.METHOD_SUM, MethodSum.class, TextTypes.METHOD),
		SQL_SUM(StepEnum.SQL_SUM, SqlSum.class, TextTypes.SQL),
		MESSAGE_SUM(StepEnum.MESSAGE_SUM, MessageSum.class, null),
		SOCKET_SUM(StepEnum.SOCKET_SUM, SocketSum.class, null),
		APICALL_SUM(StepEnum.APICALL_SUM, ApiCallSum.class, TextTypes.APICALL),
		CONTROL(StepEnum.CONTROL, StepControl.class, null),
		SPAN(StepEnum.SPAN, SpanStep.class, TextTypes.SERVICE),
		SPANCALL(StepEnum.SPANCALL, SpanCallStep.class, TextTypes.SERVICE),
		;

		byte code;
		Class<? extends Step> clazz;
		String associatedMainTextTypeName;

		Type(byte code, Class<? extends Step> clazz, String textType) {
			this.code = code;
			this.clazz = clazz;
			this.associatedMainTextTypeName = textType;
		}

		public static Type of(String name) {
			for(Type t : Type.values()) {
				if (t.name().equals(name)) {
					return t;
				}
			}
			throw new RuntimeException("unknown profile type=" + name);
		}

		public static Type of(byte code) {
			for(Type t : Type.values()) {
				if (t.code == code) {
					return t;
				}
			}
			throw new RuntimeException("unknown profile type=" + code);
		}

		public Step create() throws IllegalAccessException, InstantiationException {
			return this.clazz.newInstance();
		}

		public byte getCode() {
			return this.code;
		}

		public Class<? extends Step> getClazz() {
			return this.clazz;
		}

		public String getAssociatedMainTextTypeName() {
			return associatedMainTextTypeName;
		}
	}

	public static Step create(byte type) {
		try {
			return Type.of(type).create();
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
