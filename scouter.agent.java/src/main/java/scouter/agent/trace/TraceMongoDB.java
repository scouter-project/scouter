/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.proxy.IMongoDbTracer;
import scouter.agent.proxy.MongoDbTraceFactory;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2020/08/16
 */
public class TraceMongoDB {

    public static final String V405 = "v405";
    public static final String V382 = "v382";
    public static final String V364 = "v364";

    static IMongoDbTracer tracer;
    static Configure conf = Configure.getInstance();

    public static Object startExecute(Object _this, Object connection, Object namespace, Object command,
                                    Object readPreference, Object payload, String version) {

        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return null;
        }

        try {
            if (tracer == null) {
                tracer = MongoDbTraceFactory.create(namespace.getClass().getClassLoader(), version);
            }
            StepTransferMap.ID id = tracer.generateAndTransferMongoQueryStep(ctx, _this, connection);
            if (id == null) {
                return null;
            }
            Object callback = tracer.genCallback(id, namespace, command, readPreference, payload);
            if (callback == null) {
                return null;
            }
            return callback;

        } catch (Throwable t) {
            Logger.println("MTC01", t.getMessage(), t);
            return null;
        }
    }

    public static void endExecute(Object callback, Throwable throwable) {
        if (callback == null) {
            return;
        }
        if (tracer == null) {
            return;
        }
        try {
            tracer.doCallback(callback, null, throwable);

        } catch (Throwable t) {
            Logger.println("MTC02", t.getMessage(), t);
        }
    }

    public static Object startExecuteAsync(Object _this, Object connection, Object namespace, Object command,
                                           Object readPreference, Object payload, Object callback, String version) {
        TraceContext ctx = TraceContextManager.getContext();
        if (ctx == null) {
            return callback;
        }
        try {
            if (tracer == null) {
                tracer = MongoDbTraceFactory.create(namespace.getClass().getClassLoader(), version);
            }
            StepTransferMap.ID id = tracer.generateAndTransferMongoQueryStep(ctx, _this, connection);
            if (id == null) {
                return callback;
            }
            return tracer.wrapCallback(id, namespace, command, readPreference, payload, callback);

        } catch (Throwable e) {
            Logger.println("MTC03", e.getMessage(), e);
            return callback;
        }
    }
}
