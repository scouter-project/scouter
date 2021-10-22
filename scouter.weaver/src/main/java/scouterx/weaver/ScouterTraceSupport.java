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
public class ScouterTraceSupport {

	public static TransferCtx startServiceAndGetCtxTransfer(String serviceName) {
		try {
			return new TransferCtx(ScouterTraceSupport0.startServiceAndGetCtxTransfer(serviceName));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static TransferCtx startServiceWithCustomTxidAndGetCtxTransfer(String serviceName, String customTxid) {
		try {
			return new TransferCtx(ScouterTraceSupport0.startServiceWithCustomTxidAndGetCtxTransfer(serviceName, customTxid));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void endServiceByCtxTransfer(TransferCtx ctxTransfer, Throwable thr) {
		try {
			ScouterTraceSupport0.endServiceByCtxTransfer(ctxTransfer.lctx, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void endServiceByCustomTxid(String customTxid, Throwable thr) {
		try {
			ScouterTraceSupport0.endServiceByCustomTxid(customTxid, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void endServiceOnTheSameThread(Throwable thr) {
		try {
			ScouterTraceSupport0.endServiceOnTheSameThread(thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MethodCtx startMethodByCtxTransfer(TransferCtx ctxTransfer, String name) {
		try {
			return new MethodCtx(ScouterTraceSupport0.startMethodByCtxTransfer(ctxTransfer.lctx, name));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static MethodCtx startMethodOnTheSameThread(String name) {
		try {
			return new MethodCtx(ScouterTraceSupport0.startMethodOnTheSameThread(name));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static MethodCtx startMethodByCustomTxid(String customTxid, String name) {
		try {
			return new MethodCtx(ScouterTraceSupport0.startMethodByCustomTxid(customTxid, name));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void endMethodByMethodTransfer(MethodCtx methodTransfer, Throwable thr) {
		try {
			ScouterTraceSupport0.endMethodByMethodTransfer(methodTransfer.lctx, thr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message) {
		try {
			ScouterTraceSupport0.addMessageProfileByCtxTransfer(ctxTransfer.lctx, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addMessageProfileByCustomTxid(String customTxid, String message) {
		try {
			ScouterTraceSupport0.addMessageProfileByCustomTxid(customTxid, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addMessageProfileOnTheSameThread(String message) {
		try {
			ScouterTraceSupport0.addMessageProfileOnTheSameThread(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addHashedMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message, int elapsedMs, int anyValue) {
		try {
			ScouterTraceSupport0.addHashedMessageProfileByCtxTransfer(ctxTransfer.lctx, message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addHashedMessageProfileByCustomTxid(String customTxid, String message, int elapsedMs, int anyValue) {
		try {
			ScouterTraceSupport0.addHashedMessageProfileByCustomTxid(customTxid, message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addHashedMessageProfileOnTheSameThread(String message, int elapsedMs, int anyValue) {
		try {
			ScouterTraceSupport0.addHashedMessageProfileOnTheSameThread(message, elapsedMs, anyValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addParameterizedMessageProfileByCtxTransfer(TransferCtx ctxTransfer, String message, ProfileLevel level, int elapsedMs, String... params) {
		try {
			ScouterTraceSupport0.addParameterizedMessageProfileByCtxTransfer(ctxTransfer.lctx, message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addParameterizedMessageProfileByCustomTxid(String customTxid, String message, ProfileLevel level, int elapsedMs, String... params) {
		try {
			ScouterTraceSupport0.addParameterizedMessageProfileByCustomTxid(customTxid, message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addParameterizedMessageProfileOnTheSameThread(String message, ProfileLevel level, int elapsedMs, String... params) {
		try {
			ScouterTraceSupport0.addParameterizedMessageProfileOnTheSameThread(message, level.getLevel(), elapsedMs, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	private static class ScouterTraceSupport0 {

		protected static Object startServiceAndGetCtxTransfer(String serviceName) {
			return null;
		}

		protected static Object startServiceWithCustomTxidAndGetCtxTransfer(String serviceName, String customTxid) {
			return null;
		}

		protected static void endServiceByCtxTransfer(Object ctxTransfer, Throwable thr) {
		}

		protected static void endServiceByCustomTxid(String customTxid, Throwable thr) {
		}

		protected static void endServiceOnTheSameThread(Throwable thr) {
		}

		protected static Object startMethodByCtxTransfer(Object ctxTransfer, String name) {
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

		protected static void addMessageProfileByCtxTransfer(Object ctxTransfer, String message) {
		}

		protected static void addMessageProfileByCustomTxid(String customTxid, String message) {
		}

		protected static void addMessageProfileOnTheSameThread(String message) {
		}

		protected static void addHashedMessageProfileByCtxTransfer(Object ctxTransfer, String message, int elapsedMs, int anyValue) {
		}

		protected static void addHashedMessageProfileByCustomTxid(String customTxid, String message, int elapsedMs, int anyValue) {
		}

		protected static void addHashedMessageProfileOnTheSameThread(String message, int elapsedMs, int anyValue) {
		}

		protected static void addParameterizedMessageProfileByCtxTransfer(Object ctxTransfer, String message, byte level, int elapsedMs, String... params) {
		}

		protected static void addParameterizedMessageProfileByCustomTxid(String customTxid, String message, byte level, int elapsedMs, String... params) {
		}

		protected static void addParameterizedMessageProfileOnTheSameThread(String message, byte level, int elapsedMs, String... params) {
		}
	}
}
