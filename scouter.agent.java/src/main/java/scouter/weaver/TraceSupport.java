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

import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.LocalContext;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.pack.XLogTypes;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.ParameterizedMessageStep;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/21
 */
public class TraceSupport {

	private static final Object dummyObj = new Object();
	private static final Object[] dummyArgs = new Object[0];


	public static Object getCtxOnTheSameThread() {
		return TraceContextManager.getContext();
	}

	public static TraceContext getCtxByCustomTxid(String customTxid) {
		return TraceContextManager.getContextByCustomTxid(customTxid);
	}

	public static Object startServiceAndGetCtx(String serviceName) {
		return TraceMain.startService(serviceName, "_custom_", serviceName, "_none_", dummyObj, dummyArgs, XLogTypes.APP_SERVICE);
	}

	public static Object startServiceWithCustomTxidAndGetCtx(String serviceName, String customTxid) {
		return TraceMain.startServiceWithCustomTxid(serviceName, "_custom_", serviceName, "_none_", dummyObj, dummyArgs,
				XLogTypes.APP_SERVICE, customTxid);
	}

	public static void endServiceByCtx(Object anyCtx, Throwable thr) {
		if (anyCtx == null) {
			return;
		}
		Object ctx4EndService = anyCtx;
		if (anyCtx instanceof TraceContext) {
			ctx4EndService = new LocalContext((TraceContext) anyCtx, null);
		}
		TraceMain.endService(ctx4EndService, null, thr);
	}

	public static void endServiceByCustomTxid(String customTxid, Throwable thr) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		endService0(ctx, thr);
	}

	public static void endServiceOnTheSameThread(Throwable thr) {
		TraceContext ctx = TraceContextManager.getContext();
		endService0(ctx, thr);
	}

	private static void endService0(TraceContext ctx, Throwable thr) {
		if (ctx == null) {
			return;
		}
		TraceMain.endService(new LocalContext(ctx, null), null, thr);
	}

	public static Object startMethodByCtx(Object anyCtx, String name) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		if (ctx == null) {
			return null;
		}
		int hash = DataProxy.sendMethodName(name);
		return TraceMain.startMethod(hash, name, ctx);
	}

	public static Object startMethodByCustomTxid(String customTxid, String name) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		if (ctx == null) {
			return null;
		}
		int hash = DataProxy.sendMethodName(name);
		return TraceMain.startMethod(hash, name, ctx);
	}

	public static Object startMethodOnTheSameThread(String name) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx == null) {
			return null;
		}
		int hash = DataProxy.sendMethodName(name);
		return TraceMain.startMethod(hash, name, ctx);
	}

	public static void endMethodByMethodTransfer(Object methodTransfer, Throwable thr) {
		TraceMain.endMethod(methodTransfer, thr);
	}

	public static void addMessageProfileByCtx(Object anyCtx, String message) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		addMessageProfile0(ctx, message);
	}

	public static void addMessageProfileByCustomTxid(String customTxid, String message) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		addMessageProfile0(ctx, message);
	}

	public static void addMessageProfileOnTheSameThread(String message) {
		TraceContext ctx = TraceContextManager.getContext();
		addMessageProfile0(ctx, message);
	}

	private static void addMessageProfile0(TraceContext ctx, String message) {
		if (ctx == null) {
			return;
		}
		MessageStep messageStep = new MessageStep((int) (System.currentTimeMillis() - ctx.startTime), message);
		ctx.profile.add(messageStep);
	}


	public static void addHashedMessageProfileByCtx(Object anyCtx, String message, int elapsedMs, int anyValue) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		addHashedMessageProfile0(ctx, message, elapsedMs, anyValue);
	}

	public static void addHashedMessageProfileByCustomTxid(String customTxid, String message, int elapsedMs, int anyValue) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		addHashedMessageProfile0(ctx, message, elapsedMs, anyValue);
	}

	public static void addHashedMessageProfileOnTheSameThread(String message, int elapsedMs, int anyValue) {
		TraceContext ctx = TraceContextManager.getContext();
		addHashedMessageProfile0(ctx, message, elapsedMs, anyValue);
	}

	private static void addHashedMessageProfile0(TraceContext ctx, String message, int elapsedMs, int anyValue) {
		if (ctx == null) {
			return;
		}
		HashedMessageStep step = new HashedMessageStep();
		step.hash = DataProxy.sendHashedMessage(message);
		step.value = anyValue;
		step.time = elapsedMs;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(step);
	}



	public static void addParameterizedMessageProfileByCtx(Object anyCtx, String message, byte level, int elapsedMs, String... params) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		addParameterizedMessageProfile0(ctx, message, level, elapsedMs, params);
	}

	public static void addParameterizedMessageProfileByCustomTxid(String customTxid, String message, byte level, int elapsedMs, String... params) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		addParameterizedMessageProfile0(ctx, message, level, elapsedMs, params);
	}

	public static void addParameterizedMessageProfileOnTheSameThread(String message, byte level, int elapsedMs, String... params) {
		TraceContext ctx = TraceContextManager.getContext();
		addParameterizedMessageProfile0(ctx, message, level, elapsedMs, params);
	}

	private static void addParameterizedMessageProfile0(TraceContext ctx, String message, byte level, int elapsedMs, String... params) {
		if (ctx == null) {
			return;
		}
		ParameterizedMessageStep step = new ParameterizedMessageStep();
		step.setMessage(DataProxy.sendHashedMessage(message), params);
		step.setElapsed(elapsedMs);
		step.setLevelOfByte(level);
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(step);
	}




	//txid getters
	public static Object getTxidOnTheSameThread() {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			return ctx.txid;
		}
		return 0;
	}

	public static Object getTxidByCtx(Object anyCtx) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		if (ctx != null) {
			return ctx.txid;
		}
		return 0;
	}

	public static Object getTxidByCustomTxid(String customTxid) {
		TraceContext ctx = getCtxByCustomTxid(customTxid);
		if (ctx != null) {
			return ctx.txid;
		}
		return 0;
	}

	//link custom txid
	public static void linkCustomTxidOnTheSameThread(String customTxid) {
		TraceContext ctx = TraceContextManager.getContext();
		if (ctx != null) {
			TraceContextManager.linkCustomTxid(customTxid, ctx.txid);
		}
	}

	public static void linkCustomTxidByCtx(String customTxid, Object anyCtx) {
		TraceContext ctx = getCtxByAnyCtx(anyCtx);
		if (ctx != null) {
			TraceContextManager.linkCustomTxid(customTxid, ctx.txid);
		}
	}

	//xlog info setters
	public static void setXlogServiceValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null && value != null) {
			ctx.serviceName = value;
			ctx.serviceHash = DataProxy.sendServiceName(ctx.serviceName);
		}
	}
	public static void setXlogIpValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.remoteIp = value;
		}
	}
	public static void setXlogUaValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null && value != null) {
			ctx.userAgent = DataProxy.sendUserAgent(value);
			ctx.userAgentString = value;
		}
	}
	public static void setXlogErrorValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null && value != null) {
			if (ctx.error == 0) {
				ctx.error = DataProxy.sendError(value);
			}
		}
		addParameterizedMessageProfile0(ctx, value, ParameterizedMessageLevel.ERROR.getLevel(), 0);
	}

	public static void setXlogLoginValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.login = value;
		}
	}
	public static void setXlogDescValue(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.desc = value;
		}
	}
	public static void setXlogText1Value(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.text1 = value;
		}
	}
	public static void setXlogText2Value(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.text2 = value;
		}
	}
	public static void setXlogText3Value(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.text3 = value;
		}
	}
	public static void setXlogText4Value(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.text4 = value;
		}
	}
	public static void setXlogText5Value(long txid, String value) {
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx != null) {
			ctx.text5 = value;
		}
	}




	private static TraceContext getCtxByAnyCtx(Object anyCtx) {
		if (anyCtx instanceof LocalContext) {
			LocalContext localContext = (LocalContext) anyCtx;
			return localContext.context;

		} else if (anyCtx instanceof TraceContext) {
			return (TraceContext) anyCtx;
		}
		return null;
	}
}
