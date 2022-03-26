package scouterx.weaver;
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
public class Scouter {

	/**
	 * check if scouter java agent is activated
	 * @return
	 */
	public static boolean isScouterJavaAgentActivated() {
		Object scouterJavaAgentActivated = Weaving.isScouterJavaAgentActivated();
		if (scouterJavaAgentActivated instanceof Boolean) {
			return (boolean) scouterJavaAgentActivated;
		} else {
			return false;
		}
	}

	/**
	 * get already started transfer context. if the trace is not started, it returns empty transfer(trace) context.
	 * @return
	 */
	public static TransferCtx getTransferCtxOnTheSameThread() {
		Object ctx = Weaving.getCtxOnTheSameThread();
		if (ctx == null) {
			return TransferCtx.EMPTY;
		}
		Object txid = Weaving.getTxidByCtx(ctx);
		return new TransferCtx(ctx, ScouterTxid.of(txid));
	}

	/**
	 * get already started transfer context. if the trace is not started, it returns empty transfer(trace) context.
	 * @param customTxid
	 * @return
	 */
	public static TransferCtx getTransferCtxByCustomTxid(String customTxid) {
		Object ctx = Weaving.getCtxByCustomTxid(customTxid);
		if (ctx == null) {
			return TransferCtx.EMPTY;
		}
		Object txid = Weaving.getTxidByCtx(ctx);
		return new TransferCtx(ctx, ScouterTxid.of(txid));
	}

	/**
	 * start trace(Xlog) for the serviceName and generate new transferCtx of the transaction for transferring a trace context via service flow.
	 * @param serviceName
	 * @return
	 */
	public static TransferCtx startServiceAndGetCtxTransfer(String serviceName) {
		try {
			Object ctx = Weaving.startServiceAndGetCtx(serviceName);
			if (ctx == null) {
				return TransferCtx.EMPTY;
			}
			Object txid = Weaving.getTxidByCtx(ctx);
			return new TransferCtx(ctx, ScouterTxid.of(txid));
		} catch (Exception e) {
			e.printStackTrace();
			return TransferCtx.EMPTY;
		}
	}

	/**
	 * start trace(Xlog) for the serviceName with connecting custom txid with scouter
	 * and generate new transferCtx of the transaction for transferring a trace context via service flow.
	 * @param serviceName
	 * @return
	 */
	public static TransferCtx startServiceWithCustomTxidAndGetCtxTransfer(String serviceName, String customTxid) {
		try {
			Object ctx = Weaving.startServiceWithCustomTxidAndGetCtx(serviceName, customTxid);
			if (ctx == null) {
				return TransferCtx.EMPTY;
			}
			Object txid = Weaving.getTxidByCtx(ctx);
			return new TransferCtx(ctx, ScouterTxid.of(txid));
		} catch (Exception e) {
			e.printStackTrace();
			return TransferCtx.EMPTY;
		}
	}

	/**
	 * end trace(Xlog) and internally measure an elapsed time of service.
	 * error is marked on the xlog if thr is not null
	 * @param ctxTransfer
	 * @param thr
	 */
	public static void endServiceByCtxTransfer(TransferCtx ctxTransfer, Throwable thr) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return;
		}
		try {
			Weaving.endServiceByCtx(ctxTransfer.ctx, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * end trace(Xlog) and internally measure an elapsed time of service.
	 * error is marked on the xlog if thr is not null
	 * @param customTxid
	 * @param thr
	 */
	public static void endServiceByCustomTxid(String customTxid, Throwable thr) {
		try {
			Weaving.endServiceByCustomTxid(customTxid, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * end trace(Xlog) and internally measure an elapsed time of service.
	 * error is marked on the xlog if thr is not null
	 * @param thr
	 */
	public static void endServiceOnTheSameThread(Throwable thr) {
		try {
			Weaving.endServiceOnTheSameThread(thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * start method profiling
	 * @param ctxTransfer
	 * @param name
	 * @return
	 */
	public static MethodCtx startMethodByCtxTransfer(TransferCtx ctxTransfer, String name) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return MethodCtx.EMPTY;
		}
		try {
			return new MethodCtx(Weaving.startMethodByCtx(ctxTransfer.ctx, name));
		} catch (Exception e) {
			e.printStackTrace();
			return MethodCtx.EMPTY;
		}
	}

	/**
	 * start method profiling
	 * @param name
	 * @return
	 */
	public static MethodCtx startMethodOnTheSameThread(String name) {
		try {
			return new MethodCtx(Weaving.startMethodOnTheSameThread(name));
		} catch (Exception e) {
			e.printStackTrace();
			return MethodCtx.EMPTY;
		}
	}

	/**
	 * start method profiling
	 * @param customTxid
	 * @param name
	 * @return
	 */
	public static MethodCtx startMethodByCustomTxid(String customTxid, String name) {
		try {
			return new MethodCtx(Weaving.startMethodByCustomTxid(customTxid, name));
		} catch (Exception e) {
			e.printStackTrace();
			return MethodCtx.EMPTY;
		}
	}

	/**
	 * end method profiling and mark this method's elapsed time on the profile.
	 * @param methodTransfer
	 * @param thr
	 */
	public static void endMethodByMethodTransfer(MethodCtx methodTransfer, Throwable thr) {
		if (methodTransfer == null || methodTransfer.isEmpty()) {
			return;
		}
		try {
			Weaving.endMethodByMethodTransfer(methodTransfer.lctx, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add message profile
	 * @param ctxTransfer
	 * @param message
	 */
	public static void addMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return;
		}
		try {
			Weaving.addMessageProfileByCtx(ctxTransfer.ctx, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add message profile
	 * @param customTxid
	 * @param message
	 */
	public static void addMessageProfileByCustomTxid(String customTxid, String message) {
		try {
			Weaving.addMessageProfileByCustomTxid(customTxid, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add message profile
	 * @param message
	 */
	public static void addMessageProfileOnTheSameThread(String message) {
		try {
			Weaving.addMessageProfileOnTheSameThread(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) message profile.
	 * @param ctxTransfer
	 * @param message
	 * @param elapsedMs
	 * @param anyValue
	 */
	public static void addHashedMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message, int elapsedMs, int anyValue) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return;
		}
		try {
			Weaving.addHashedMessageProfileByCtx(ctxTransfer.ctx, message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) message profile.
	 * @param customTxid
	 * @param message
	 * @param elapsedMs
	 * @param anyValue
	 */
	public static void addHashedMessageProfileByCustomTxid(String customTxid, String message, int elapsedMs, int anyValue) {
		try {
			Weaving.addHashedMessageProfileByCustomTxid(customTxid, message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) message profile.
	 * @param message
	 * @param elapsedMs
	 * @param anyValue
	 */
	public static void addHashedMessageProfileOnTheSameThread(String message, int elapsedMs, int anyValue) {
		try {
			Weaving.addHashedMessageProfileOnTheSameThread(message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) with parameterized message profile.
	 * @param ctxTransfer
	 * @param message
	 * @param level
	 * @param elapsedMs
	 * @param params
	 */
	public static void addParameterizedMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message, ProfileLevel level, int elapsedMs, String... params) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return;
		}
		try {
			Weaving.addParameterizedMessageProfileByCtx(ctxTransfer.ctx, message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) with parameterized message profile.
	 * @param customTxid
	 * @param message
	 * @param level
	 * @param elapsedMs
	 * @param params
	 */
	public static void addParameterizedMessageProfileByCustomTxid(String customTxid, String message, ProfileLevel level, int elapsedMs, String... params) {
		try {
			Weaving.addParameterizedMessageProfileByCustomTxid(customTxid, message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add hashed(dictionary encoded) with parameterized message profile.
	 * @param message
	 * @param level
	 * @param elapsedMs
	 * @param params
	 */
	public static void addParameterizedMessageProfileOnTheSameThread(String message, ProfileLevel level, int elapsedMs, String... params) {
		try {
			Weaving.addParameterizedMessageProfileOnTheSameThread(message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get ScouterTxid
	 * @return
	 */
	public static ScouterTxid getTxidOnTheSameThread() {
		Object txid = Weaving.getTxidOnTheSameThread();
		return ScouterTxid.of(txid);
	}

	/**
	 * get ScouterTxid
	 * @param customTxid
	 * @return
	 */
	public static ScouterTxid getTxidByCustomTxid(String customTxid) {
		Object txid = Weaving.getTxidByCustomTxid(customTxid);
		return ScouterTxid.of(txid);
	}

	/**
	 * link custom txid onto the scouter trace context.
	 * @param customTxid
	 */
	public static void linkCustomTxidOnTheSameThread(String customTxid) {
		Weaving.linkCustomTxidOnTheSameThread(customTxid);
	}

	/**
	 * link custom txid onto the scouter trace context.
	 * @param customTxid
	 * @param ctxTransfer
	 */
	public static void linkCustomTxidByCtxTransfer(String customTxid, TransferCtx ctxTransfer) {
		if (ctxTransfer == null || ctxTransfer.isEmpty()) {
			return;
		}
		Weaving.linkCustomTxidByCtx(customTxid, ctxTransfer.ctx);
	}

	/**
	 * add value onto xlog service column
	 * @param txid
	 * @param value
	 */
	public static void setXlogServiceDictionaryValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogServiceValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog ip column
	 * @param txid
	 * @param value
	 */
	public static void setXlogIpValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogIpValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog user agent column
	 * @param txid
	 * @param value
	 */
	public static void setXlogUaDictionaryValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogUaValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog error column
	 * @param txid
	 * @param value
	 */
	public static void setXlogErrorDictionaryValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogErrorValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog login column
	 * @param txid
	 * @param value
	 */
	public static void setXlogLoginDictionaryValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogLoginValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog desc column
	 * @param txid
	 * @param value
	 */
	public static void setXlogDescDictionaryValue(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogDescValue(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog text1 column
	 * @param txid
	 * @param value
	 */
	public static void setXlogText1Value(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogText1Value(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog text2 column
	 * @param txid
	 * @param value
	 */
	public static void setXlogText2Value(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogText2Value(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog text3 column
	 * @param txid
	 * @param value
	 */
	public static void setXlogText3Value(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogText3Value(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog text4 column
	 * @param txid
	 * @param value
	 */
	public static void setXlogText4Value(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogText4Value(txid.getTxid(), value);
	}

	/**
	 * add value onto xlog text5 column
	 * @param txid
	 * @param value
	 */
	public static void setXlogText5Value(ScouterTxid txid, String value) {
		if (txid == null || txid.isEmpty() || value == null) {
			return;
		}
		Weaving.setXlogText5Value(txid.getTxid(), value);
	}





	public enum ProfileLevel {
		DEBUG((byte)0),
		INFO((byte)1),
		WARN((byte)2),
		ERROR((byte)3),
		FATAL((byte)4),
		;
		private final byte level;

		ProfileLevel(byte level) {
			this.level = level;
		}

		public byte getLevel() {
			return this.level;
		}
	}





	/**
	 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/21
	 */
	private static class Weaving {
		protected static Object isScouterJavaAgentActivated() {
			return false;
		}

		protected static Object getCtxOnTheSameThread() {
			return null;
		}

		protected static Object getCtxByCustomTxid(String customTxid) {
			return null;
		}

		protected static Object startServiceAndGetCtx(String serviceName) {
			return null;
		}

		protected static Object startServiceWithCustomTxidAndGetCtx(String serviceName, String customTxid) {
			return null;
		}

		protected static void endServiceByCtx(Object ctx, Throwable thr) {
		}

		protected static void endServiceByCustomTxid(String customTxid, Throwable thr) {
		}

		protected static void endServiceOnTheSameThread(Throwable thr) {
		}

		protected static Object startMethodByCtx(Object ctx, String name) {
			return null;
		}

		protected static Object startMethodByCustomTxid(String customTxid, String name) {
			return null;
		}

		protected static Object startMethodOnTheSameThread(String name) {
			return null;
		}

		protected static void endMethodByMethodTransfer(Object methodTransfer, Throwable thr) {
		}

		protected static void addMessageProfileByCtx(Object ctx, String message) {
		}

		protected static void addMessageProfileByCustomTxid(String customTxid, String message) {
		}

		protected static void addMessageProfileOnTheSameThread(String message) {
		}

		protected static void addHashedMessageProfileByCtx(Object ctx, String message, int elapsedMs, int anyValue) {
		}

		protected static void addHashedMessageProfileByCustomTxid(String customTxid, String message, int elapsedMs, int anyValue) {
		}

		protected static void addHashedMessageProfileOnTheSameThread(String message, int elapsedMs, int anyValue) {
		}

		protected static void addParameterizedMessageProfileByCtx(Object ctx, String message, byte level, int elapsedMs, String... params) {
		}

		protected static void addParameterizedMessageProfileByCustomTxid(String customTxid, String message, byte level, int elapsedMs, String... params) {
		}

		protected static void addParameterizedMessageProfileOnTheSameThread(String message, byte level, int elapsedMs, String... params) {
		}



		//txid getters
		protected static Object getTxidOnTheSameThread() {
			return null;
		}

		protected static Object getTxidByCtx(Object ctx) {
			return null;
		}

		protected static Object getTxidByCustomTxid(String customTxid) {
			return null;
		}

		//link custom txid
		protected static void linkCustomTxidOnTheSameThread(String customTxid) {
		}

		protected static void linkCustomTxidByCtx(String customTxid, Object ctx) {
		}

		//xlog info setters
		protected static void setXlogServiceValue(long txid, String value) {
		}
		protected static void setXlogIpValue(long txid, String value) {
		}
		protected static void setXlogUaValue(long txid, String value) {
		}
		protected static void setXlogErrorValue(long txid, String value) {
		}

		protected static void setXlogLoginValue(long txid, String value) {
		}
		protected static void setXlogDescValue(long txid, String value) {
		}
		protected static void setXlogText1Value(long txid, String value) {
		}
		protected static void setXlogText2Value(long txid, String value) {
		}
		protected static void setXlogText3Value(long txid, String value) {
		}
		protected static void setXlogText4Value(long txid, String value) {
		}
		protected static void setXlogText5Value(long txid, String value) {
		}
	}
}
