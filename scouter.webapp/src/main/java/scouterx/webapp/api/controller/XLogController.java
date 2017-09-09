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

package scouterx.webapp.api.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.XLogPack;
import scouterx.client.net.INetReader;
import scouterx.webapp.api.view.CommonResultView;
import scouterx.model.scouter.SXlog;
import scouterx.webapp.api.request.PageableXLogRequest;
import scouterx.webapp.api.request.RealTimeXLogRequest;
import scouterx.webapp.service.XLogService;
import scouterx.webapp.api.view.PageableXLogView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Path("/v1/xlog")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class XLogController {
    private final XLogService xLogService;

    public XLogController() {
        this.xLogService = new XLogService();
    }

    /**
     * get values of several counters for given an object
     * uri : /xlog/realTime/0/100?objHashes=10001,10002 or ?objHashes=[10001,100002]
     *
     * @return
     */
    @GET
    @Path("/realTime/{xlogLoop}/{xlogIndex}")
    public Response streamRealTimeXLog(@BeanParam @Valid final RealTimeXLogRequest xLogRequest) {

        Consumer<JsonGenerator> realTimeXLogHandlerConsumer = jsonGenerator -> {
            try {
                int[] countable = {0};
                INetReader xLogReader = getRealTimeXLogReader(jsonGenerator, countable);

                xLogService.handleRealTimeXLog(xLogRequest, xLogReader);
                jsonGenerator.writeEndArray();
                jsonGenerator.writeNumberField("count", countable[0]);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        StreamingOutput streamingOutput = outputStream ->
                CommonResultView.jsonStream(outputStream, realTimeXLogHandlerConsumer);

        return Response.ok().entity(streamingOutput).type(MediaType.APPLICATION_JSON).build();
    }


    /**
     * request xlog token for range request.
     * uri : /xlog/{yyyymmdd}?startTime=... @see {@link PageableXLogRequest}
     *
     * @param xLogRequest
     * @return PageableXLogView @see {@link PageableXLogView}
     */
    @GET
    @Path("/{yyyymmdd}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response streamPageableXLog(@Valid @BeanParam PageableXLogRequest xLogRequest) {
        xLogRequest.validate();

        Consumer<JsonGenerator> pageableXLogHandlerConsumer = jsonGenerator -> {
            try {
                jsonGenerator.writeArrayFieldStart("xlogs");
                xLogService.handlePageableXLog(xLogRequest, getPageableXLogReader(jsonGenerator));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        StreamingOutput streamingOutput = outputStream ->
                CommonResultView.jsonStream(outputStream, pageableXLogHandlerConsumer);

        return Response.ok().entity(streamingOutput).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * get INetReader to make streaming output from realtime xlogs.
     *
     * @param jsonGenerator - low-level streaming json generator
     * @param countable - to keep xlog count
     * @return INetReader
     */
    private INetReader getRealTimeXLogReader(JsonGenerator jsonGenerator, int[] countable) {
        return in -> {
            Pack p = in.readPack();
            if (p.getPackType() == PackEnum.MAP) { //meta data arrive ahead of xlog pack
                MapPack metaPack = (MapPack) p;
                jsonGenerator.writeNumberField("xlogIndex", metaPack.getInt(ParamConstant.XLOG_INDEX));
                jsonGenerator.writeNumberField("xlogLoop", metaPack.getInt(ParamConstant.XLOG_LOOP));
                jsonGenerator.writeArrayFieldStart("xlogs");
            } else {
                XLogPack xLogPack = (XLogPack) p;
                jsonGenerator.writeObject(SXlog.of(xLogPack));
                countable[0]++;
            }
        };
    }

    /**
     * get INetReader to make streaming output from xlogs.
     *
     * @param jsonGenerator - low-level streaming json generator
     * @return INetReader
     */
    private INetReader getPageableXLogReader(JsonGenerator jsonGenerator) {
        int[] countable = {0};

        return in -> {
            Pack p = in.readPack();
            if (p.getPackType() != PackEnum.MAP) { // XLogPack case
                XLogPack xLogPack = (XLogPack) p;
                jsonGenerator.writeObject(SXlog.of(xLogPack));
                countable[0]++;
            } else { // MapPack case (//meta data arrive followed by xlog pack)
                jsonGenerator.writeEndArray();

                MapPack metaPack = (MapPack) p;
                jsonGenerator.writeBooleanField("hasMore", metaPack.getBoolean(ParamConstant.XLOG_RESULT_HAS_MORE));
                jsonGenerator.writeNumberField("lastTxid", metaPack.getLong(ParamConstant.XLOG_RESULT_LAST_TXID));
                jsonGenerator.writeNumberField("lastXLogTime", metaPack.getLong(ParamConstant.XLOG_RESULT_LAST_TIME));
                jsonGenerator.writeNumberField("count", countable[0]);
            }
        };
    }
}
