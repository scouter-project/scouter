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

public class StepEnum {
	public final static byte METHOD = 1;
	public final static byte METHOD2 = 10;
	public final static byte SQL = 2;
	public final static byte SQL2 = 8;
	public final static byte SQL3 = 16;
	public final static byte MESSAGE = 3;
	public final static byte SOCKET = 5;
	public final static byte APICALL = 6;
	public final static byte APICALL2 = 15;
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
		METHOD(StepEnum.METHOD, MethodStep.class),
		METHOD2(StepEnum.METHOD, MethodStep2.class),
		SQL(StepEnum.SQL, SqlStep.class),
		SQL2(StepEnum.SQL2, SqlStep2.class),
		SQL3(StepEnum.SQL3, SqlStep3.class),
		MESSAGE(StepEnum.MESSAGE, MessageStep.class),
		SOCKET(StepEnum.SOCKET, SocketStep.class),
		APICALL(StepEnum.APICALL, ApiCallStep.class),
		APICALL2(StepEnum.APICALL2, ApiCallStep2.class),
		THREAD_SUBMIT(StepEnum.THREAD_SUBMIT, ThreadSubmitStep.class),
		HASHED_MESSAGE(StepEnum.HASHED_MESSAGE, HashedMessageStep.class),
		PARAMETERIZED_MESSAGE(StepEnum.PARAMETERIZED_MESSAGE, ParameterizedMessageStep.class),
		DUMP(StepEnum.DUMP, DumpStep.class),
		DISPATCH(StepEnum.DISPATCH, DispatchStep.class),
		THREAD_CALL_POSSIBLE(StepEnum.THREAD_CALL_POSSIBLE, ThreadCallPossibleStep.class),
		METHOD_SUM(StepEnum.METHOD_SUM, MethodSum.class),
		SQL_SUM(StepEnum.SQL_SUM, SqlSum.class),
		MESSAGE_SUM(StepEnum.MESSAGE_SUM, MessageSum.class),
		SOCKET_SUM(StepEnum.SOCKET_SUM, SocketSum.class),
		APICALL_SUM(StepEnum.APICALL_SUM, ApiCallSum.class),
		CONTROL(StepEnum.CONTROL, StepControl.class),
		;

		byte code;
		Class<? extends Step> clazz;
		Type(byte code, Class<? extends Step> clazz) {
			this.code = code;
			this.clazz = clazz;
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
