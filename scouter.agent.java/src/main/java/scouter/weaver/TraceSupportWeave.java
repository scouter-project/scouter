package scouter.weaver;
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

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/21
 */
public class TraceSupportWeave {

	public static void touch() {
	}

	public static Object nothing(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return null;
	}

	public static Object startServiceAndGetCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startServiceAndGetCtxTransfer((String)arg[0]);
	}

	public static Object startServiceWithCustomTxidAndGetCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startServiceWithCustomTxidAndGetCtxTransfer((String)arg[0], (String)arg[1]);
	}

	public static Object endServiceByCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.endServiceByCtxTransfer(arg[0], (Throwable)arg[1]);
		return null;
	}

	public static Object endServiceByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.endServiceByCustomTxid((String) arg[0], (Throwable)arg[1]);
		return null;
	}

	public static Object endServiceOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.endServiceOnTheSameThread((Throwable)arg[0]);
		return null;
	}

	public static Object startMethodByCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startMethodByCtxTransfer(arg[0], (String)arg[1]);
	}

	public static Object startMethodByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startMethodByCustomTxid((String)arg[0], (String)arg[1]);
	}

	public static Object startMethodOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startMethodOnTheSameThread((String)arg[0]);
	}

	public static Object endMethodByMethodTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.endMethodByMethodTransfer(arg[0], (Throwable)arg[1]);
		return null;
	}

	public static Object addMessageProfileByCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addMessageProfileByCtxTransfer(arg[0], (String)arg[1]);
		return null;
	}

	public static Object addMessageProfileByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addMessageProfileByCustomTxid((String)arg[0], (String)arg[1]);
		return null;
	}

	public static Object addMessageProfileOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addMessageProfileOnTheSameThread((String)arg[0]);
		return null;
	}

	public static Object addHashedMessageProfileByCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileByCtxTransfer(arg[0], (String)arg[1], (int)arg[2], (int)arg[3]);
		return null;
	}

	public static Object addHashedMessageProfileByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileByCustomTxid((String)arg[0], (String)arg[1], (int)arg[2], (int)arg[3]);
		return null;
	}

	public static Object addHashedMessageProfileOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileOnTheSameThread((String)arg[0], (int)arg[1], (int)arg[2]);
		return null;
	}

	public static Object addParameterizedMessageProfileByCtxTransfer(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileByCtxTransfer(arg[0], (String)arg[1], (byte)arg[2], (int)arg[3], (String[])arg[4]);
		return null;
	}

	public static Object addParameterizedMessageProfileByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileByCustomTxid((String)arg[0], (String)arg[1], (byte)arg[2], (int)arg[3], (String[])arg[4]);
		return null;
	}

	public static Object addParameterizedMessageProfileOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileOnTheSameThread((String)arg[0], (byte)arg[1], (int)arg[2], (String[])arg[3]);
		return null;
	}

}
