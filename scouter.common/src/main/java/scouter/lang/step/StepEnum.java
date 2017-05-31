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

import java.io.IOException;


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
	public static final byte THREAD_SUBMIT = 7;
	public final static byte HASHED_MESSAGE = 9;
	public final static byte PARAMETERIZED_MESSAGE = 17;
	public final static byte DUMP = 12;
	public final static byte DISPATCH = 13;
	public final static byte THREAD_CALL_POSSIBLE = 14;

	public final static byte METHOD_SUM = 11;
	public final static byte SQL_SUM = 21;
	public final static byte MESSAGE_SUM = 31;
	public static final byte SOCKET_SUM = 42;
	public static final byte APICALL_SUM = 43;
	public final static byte CONTROL = 99;


	public static Step create(byte type) throws IOException {
		switch (type) {
		case MESSAGE:
			return new MessageStep();
		case METHOD:
			return new MethodStep();
		case METHOD2:
			return new MethodStep2();
		case SQL:
			return new SqlStep();
		case SQL2:
			return new SqlStep2();
		case SQL3:
			return new SqlStep3();
		case SOCKET:
			return new SocketStep();
		case APICALL:
			return new ApiCallStep();
		case APICALL2:
			return new ApiCallStep2();
		case THREAD_SUBMIT:
			return new ThreadSubmitStep();
		case HASHED_MESSAGE:
			return new HashedMessageStep();
		case PARAMETERIZED_MESSAGE:
			return new ParameterizedMessageStep();
		case DUMP:
			return new DumpStep();
		case DISPATCH:
			return new DispatchStep();
		case THREAD_CALL_POSSIBLE:
			return new ThreadCallPossibleStep();
		case MESSAGE_SUM:
			return new MessageSum();
		case METHOD_SUM:
			return new MethodSum();
		case SQL_SUM:
			return new SqlSum();
		case SOCKET_SUM:
			return new SocketSum();	
		case APICALL_SUM:
			return new ApiCallSum();	

		case CONTROL:
			return new StepControl();	
		default:
			throw new RuntimeException("unknown profile type=" + type);
		}
	}

	public static void main(String[] args) {
	    System.out.println(SQL);
    }
}
