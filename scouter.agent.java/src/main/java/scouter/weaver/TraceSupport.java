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

	public static Object startServiceAndGetCtxTransfer(String serviceName) {
		return TraceMain.startService(serviceName, "_custom_", serviceName, "_none_", dummyObj, dummyArgs, XLogTypes.APP_SERVICE);
	}

	public static Object startServiceWithCustomTxidAndGetCtxTransfer(String serviceName, String customTxid) {
		Object o = TraceMain.startService(serviceName, "_custom_", serviceName, "_none_", dummyObj, dummyArgs, XLogTypes.APP_SERVICE);
		TraceContext ctx = ((LocalContext) o).context;
		TraceContextManager.linkCustomTxid(customTxid, ctx.txid);
		return o;
	}

	public static void endServiceByCtxTransfer(Object ctxTransfer, Throwable thr) {
		if (ctxTransfer == null) {
			return;
		}
		TraceMain.endService(ctxTransfer, null, thr);
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

	public static Object startMethodByCtxTransfer(Object ctxTransfer, String name) {
		TraceContext ctx = getCtxByCtxTransfer(ctxTransfer);
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

	public static void addMessageProfileByCtxTransfer(Object ctxTransfer, String message) {
		TraceContext ctx = getCtxByCtxTransfer(ctxTransfer);
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
		MessageStep messageStep = new MessageStep((int) (System.currentTimeMillis() - ctx.startTime), message);
		ctx.profile.add(messageStep);
	}


	public static void addHashedMessageProfileByCtxTransfer(Object ctxTransfer, String message, int elapsedMs, int anyValue) {
		TraceContext ctx = getCtxByCtxTransfer(ctxTransfer);
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
		HashedMessageStep step = new HashedMessageStep();
		step.hash = DataProxy.sendHashedMessage(message);
		step.value = anyValue;
		step.time = elapsedMs;
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(step);
	}



	public static void addParameterizedMessageProfileByCtxTransfer(Object ctxTransfer, String message, byte level, int elapsedMs, String... params) {
		TraceContext ctx = getCtxByCtxTransfer(ctxTransfer);
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

		ParameterizedMessageStep step = new ParameterizedMessageStep();
		step.setMessage(DataProxy.sendHashedMessage(message), params);
		step.setElapsed(elapsedMs);
		step.setLevelOfByte(level);
		step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
		ctx.profile.add(step);
	}








	private static TraceContext getCtxByCtxTransfer(Object ctxTransfer) {
		if (ctxTransfer instanceof LocalContext) {
			LocalContext localContext = (LocalContext) ctxTransfer;
			if (localContext != null) {
				return localContext.context;
			}
			return null;
		}
		return null;
	}

	private static TraceContext getCtxByCustomTxid(String customTxid) {
		TraceContext ctx = TraceContextManager.getContextByCustomTxid(customTxid);
		if (ctx == null) {
			return null;
		} else {
			return ctx;
		}
	}
}
