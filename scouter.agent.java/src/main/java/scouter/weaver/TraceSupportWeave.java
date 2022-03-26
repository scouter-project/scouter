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


	public static Object isScouterJavaAgentActivated(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return true;
	}

	public static Object getCtxOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.getCtxOnTheSameThread();
	}

	public static Object getCtxByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.getCtxByCustomTxid((String)arg[0]);
	}

	public static Object startServiceAndGetCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startServiceAndGetCtx((String)arg[0]);
	}

	public static Object startServiceWithCustomTxidAndGetCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startServiceWithCustomTxidAndGetCtx((String)arg[0], (String)arg[1]);
	}

	public static Object endServiceByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.endServiceByCtx(arg[0], (Throwable)arg[1]);
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

	public static Object startMethodByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.startMethodByCtx(arg[0], (String)arg[1]);
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

	public static Object addMessageProfileByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addMessageProfileByCtx(arg[0], (String)arg[1]);
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

	public static Object addHashedMessageProfileByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileByCtx(arg[0], (String)arg[1], (Integer)arg[2], (Integer)arg[3]);
		return null;
	}

	public static Object addHashedMessageProfileByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileByCustomTxid((String)arg[0], (String)arg[1], (Integer)arg[2], (Integer)arg[3]);
		return null;
	}

	public static Object addHashedMessageProfileOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addHashedMessageProfileOnTheSameThread((String)arg[0], (Integer)arg[1], (Integer)arg[2]);
		return null;
	}

	public static Object addParameterizedMessageProfileByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileByCtx(arg[0], (String)arg[1], (Byte)arg[2], (Integer)arg[3], (String[])arg[4]);
		return null;
	}

	public static Object addParameterizedMessageProfileByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileByCustomTxid((String)arg[0], (String)arg[1], (Byte)arg[2], (Integer)arg[3], (String[])arg[4]);
		return null;
	}

	public static Object addParameterizedMessageProfileOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.addParameterizedMessageProfileOnTheSameThread((String)arg[0], (Byte)arg[1], (Integer)arg[2], (String[])arg[3]);
		return null;
	}


	//txid getters
	public static Object getTxidOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.getTxidOnTheSameThread();
	}

	public static Object getTxidByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.getTxidByCtx(arg[0]);
	}

	public static Object getTxidByCustomTxid(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		return TraceSupport.getTxidByCustomTxid((String)arg[0]);
	}

	//link custom txid
	public static Object linkCustomTxidOnTheSameThread(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.linkCustomTxidOnTheSameThread((String)arg[0]);
		return null;
	}

	public  static Object linkCustomTxidByCtx(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.linkCustomTxidByCtx((String)arg[0], arg[1]);
		return null;
	}

	//xlog info setters
	public  static Object setXlogServiceValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogServiceValue((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogIpValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogIpValue((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogUaValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogUaValue((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogErrorValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogErrorValue((Long)arg[0], (String)arg[1]);
		return null;
	}

	public  static Object setXlogLoginValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogLoginValue((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogDescValue(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogDescValue((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogText1Value(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogText1Value((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogText2Value(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogText2Value((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogText3Value(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogText3Value((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogText4Value(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogText4Value((Long)arg[0], (String)arg[1]);
		return null;
	}
	public  static Object setXlogText5Value(String className, String methodName, String methodDesc, Object this1, Object[] arg) {
		TraceSupport.setXlogText5Value((Long)arg[0], (String)arg[1]);
		return null;
	}

}
