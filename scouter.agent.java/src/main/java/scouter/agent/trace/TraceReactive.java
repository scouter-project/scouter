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
package scouter.agent.trace;

import java.lang.reflect.Method;

public class TraceReactive {
    public static void threadSetName(Thread thread, String name) {
        //System.out.println(">> Thread setname " + Thread.currentThread().getId() + " : " + Thread.currentThread().getName() + " -> " + name);
    }

    public static void startCoroutineIdUpdateThreadContext(long coroutineId) {
        TraceContext context = TraceContextManager.getContext(true);
        if (context == null) {
            context = TraceContextManager.getCoroutineContext(coroutineId);
            if (context == null) {
                return;
            }
        }

        CoroutineDebuggingLocal.setCoroutineDebuggingId(coroutineId);
        if (context.isReactiveStarted && !context.isCoroutineStarted) {
            context.isCoroutineStarted = true;
            context.isOnCoroutineIdUpdating = true;
            TraceContextManager.asCoroutineDebuggingMode(coroutineId, context);
        }
    }

    public static void endCoroutineIdUpdateThreadContext() {
        TraceContext context = TraceContextManager.getCoroutineContext();
        if (context == null) {
            return;
        }
        context.isOnCoroutineIdUpdating = false;
    }

    public static Object startMonoKtMono(Object coroutineContext) {
        TraceContext context = TraceContextManager.getContext(true);
        if (context == null) {
            return coroutineContext;
        }
        return TraceMain.reactiveSupport.monoCoroutineContextHook(coroutineContext, context);
    }









    private static Object[] coroutineJobParams = null;
    private static Method coroutineJobGetMethod = null;
    private static Method jobIsActiveMethod = null;

    public static void startCoroutineIdRestoreThreadContext(Object coroutineContext) {
        CoroutineDebuggingLocal.releaseCoroutineId();
    }
//    public static void startCoroutineIdRestoreThreadContext(Object coroutineContext) {
//        try {
//            if (coroutineJobParams == null) {
//                Class jobClass = Class.forName("kotlinx.coroutines.Job", false, Thread.currentThread().getContextClassLoader());
//                Field keyField = jobClass.getField("Key");
//                Object key = keyField.get(null);
//                Object[] params = new Object[1];
//                params[0] = key;
//                coroutineJobParams = params;
//            }
//
//            if (coroutineJobGetMethod == null) {
//                Class[] typeParams = new Class[1];
//                Class arg = Class.forName("kotlin.coroutines.CoroutineContext$Key", false, Thread.currentThread().getContextClassLoader());
//                typeParams[0] = arg;
//                Method method = coroutineContext.getClass().getMethod("get", typeParams[0]);
//                coroutineJobGetMethod = method;
//            }
//
//            Object job = coroutineJobGetMethod.invoke(coroutineContext, coroutineJobParams);
//
//            if (jobIsActiveMethod == null) {
//                jobIsActiveMethod = job.getClass().getMethod("isActive");
//            }
//
//            Object isActive = jobIsActiveMethod.invoke(job);
//            if (isActive instanceof Boolean && !((Boolean) isActive)) {
//                TraceContext context = TraceContextManager.getContext();
//                TraceMain.endCanceledHttpService(context);
//            }
//            //TODO
//            /*
//            Active Service 코루틴에 맞추기
//            child 코루틴과 연결 가능한지 테스트하기.
//             */
//
//        } catch (Exception e) {
//            Logger.println("A342", "reflection errors.", e);
//        }
//
//        CoroutineLocal.releaseCoroutineId();
//    }
}
