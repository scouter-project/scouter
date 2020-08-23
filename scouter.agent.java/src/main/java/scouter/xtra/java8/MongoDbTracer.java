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

package scouter.xtra.java8;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import org.bson.BsonDocument;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.StepTransferMap;
import scouter.agent.trace.TraceContext;
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.util.StringUtil;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2020/08/16
 */
public class MongoDbTracer {

    public static String COMMAND_QUERY_MSG = "[MongoDB] namespace: %s, query: %s, readPreference: %s";
    public static String COMMAND_COMMAND_MSG = "[MongoDB] namespace: %s, query: %s, payload: %s";
    public static String COMMAND_ERROR_MSG = "[MongoDB] namespace: %s, query: %s\n[Exception:%s] %s";
    public static String COMMAND_COMMAND_ERROR_MSG = "[MongoDB] namespace: %s, query: %s, payload: %s\n[Exception:%s] %s";

    static Configure conf = Configure.getInstance();

    public static StepTransferMap.ID generateAndTransferMongoQueryStep(TraceContext ctx, Object _this, String connectionDesc) {
        if (ctx == null) {
            return null;
        }
        try {
            ParameterizedMessageStep step = new ParameterizedMessageStep();
            step.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
            ctx.profile.push(step);

            if (connectionDesc != null) {
                step.putTempMessage("connectionDesc", connectionDesc);
            }
            return StepTransferMap.makeID(ctx, step);

        } catch (Throwable e) {
            Logger.println("MDp01", e.getMessage(), e);
            return null;
        }
    }

    public static class ScMongoSingleResultCallback<T> {
        public StepTransferMap.ID id;
        public Object namespace;
        public Object command;
        public Object readPreference;
        public List<BsonDocument> payload;

        public ScMongoSingleResultCallback(StepTransferMap.ID id, Object namespace, Object command, Object readPreference,
                                           List<BsonDocument> payload) {
            this.id = id;
            this.namespace = namespace;
            this.command = command;
            this.readPreference = readPreference;
            this.payload = payload;
        }

        protected void endMongoQueryStep(Throwable throwable) {
            try {
                if (id == null) {
                    return;
                }
                TraceContext ctx = id.ctx;
                ParameterizedMessageStep step = (ParameterizedMessageStep) id.step;
                if (ctx == null || step == null) {
                    return;
                }

                int elapsed = (int) (System.currentTimeMillis() - ctx.startTime) - step.start_time;
                step.setElapsed(elapsed);

                String namespaceDesc = "-";
                String bsonDesc = "-";
                String readPrefDesc = "-";
                String payloadDesc = "-";
                if (namespace instanceof MongoNamespace) {
                    namespaceDesc = ((MongoNamespace) namespace).getFullName();
                }
                if (command instanceof BsonDocument) {
                    bsonDesc = command.toString();
                }
                if (readPreference instanceof ReadPreference) {
                    readPrefDesc = ((ReadPreference) readPreference).getName();
                    }
                if (payload != null) {
                    payloadDesc = payload.toString();
                }

                if (throwable == null) {
                    step.setLevel(ParameterizedMessageLevel.INFO);
                    if (readPreference != null) {
                        step.setMessage(DataProxy.sendHashedMessage(COMMAND_QUERY_MSG), namespaceDesc, bsonDesc, readPrefDesc);
                    } else {
                        step.setMessage(DataProxy.sendHashedMessage(COMMAND_COMMAND_MSG), namespaceDesc, bsonDesc, payloadDesc);
                    }

                } else {
                    String msg = throwable.getMessage();
                    step.setLevel(ParameterizedMessageLevel.ERROR);

                    if (readPreference != null) {
                        step.setMessage(DataProxy.sendHashedMessage(COMMAND_ERROR_MSG), namespaceDesc, bsonDesc,
                                throwable.getClass().getName(), msg);
                    } else {
                        step.setMessage(DataProxy.sendHashedMessage(COMMAND_COMMAND_ERROR_MSG), namespaceDesc, bsonDesc, payloadDesc,
                                throwable.getClass().getName(), msg);
                    }

                    if (ctx.error == 0 && conf.xlog_error_on_mongodb_exception_enabled) {
                        ctx.error = DataProxy.sendError(msg);
                    }
                    //TODO not yet error summary processing for es : ctx.offerErrorEntity(ErrorEntity.of(throwable, ctx.error, 0, 0));
                }

                ctx.profile.pop(step);

                if (conf.counter_interaction_enabled) {
                    String connectionDesc = StringUtil.emptyToDefault(step.getTempMessage("connectionDesc"), "-");
                    int nodeHash = DataProxy.sendObjName(connectionDesc);
                    MeterInteraction meterInteraction = MeterInteractionManager.getInstance().getMongoDbCallMeter(conf.getObjHash(), nodeHash);
                    if (meterInteraction != null) {
                        meterInteraction.add(elapsed, throwable != null);
                    }
                }

            } catch (Throwable t) {
                Logger.println("MDp03", t.getMessage(), t);
            }
        }
    }
}
