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

package scouterx.webapp.layer.controller;

import io.swagger.annotations.Api;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.CounterService;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.request.CounterAvgRequestByObjHashes;
import scouterx.webapp.request.CounterAvgRequestByType;
import scouterx.webapp.request.CounterRequestByObjHashes;
import scouterx.webapp.request.CounterRequestByType;
import scouterx.webapp.request.LatestCounterRequestByObjHashes;
import scouterx.webapp.request.LatestCounterRequestByType;
import scouterx.webapp.view.AvgCounterView;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.view.CounterView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/counter")
@Api("Counter")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class CounterController {
    @Context
    HttpServletRequest servletRequest;

    private final CounterService counterService;

    public CounterController() {
        this.counterService = new CounterService();
    }

    /**
     * get current value of several counters about a type
     * uri : /counter/realTime/{counters}/ofType/{objType}?serverId=1001010
     *
     * @param objType
     * @param counterNameByCommaSeparator
     * @param serverId
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/{counters}/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SCounter>> retrieveRealTimeCountersByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @PathParam("counters") @Valid @NotNull final String counterNameByCommaSeparator,
            @QueryParam("serverId") final int serverId) {

        List<SCounter> counterList = counterService.retrieveRealTimeCountersByObjType(
                objType, ZZ.splitParamStringSet(counterNameByCommaSeparator), ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }

    /**
     * get current value of several counters of objects
     * uri : /counter/realTime/{counters}?serverId=1001010&objHashes=100,200,300
     *
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/{counters}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SCounter>> retrieveRealTimeCountersByObjHashes(
            @QueryParam("objHashes") @Valid @NotNull final String objHashes,
            @PathParam("counters") @Valid @NotNull final String counterNameByCommaSeparator,
            @QueryParam("serverId") final int serverId) {

        List<SCounter> counterList = counterService.retrieveRealTimeCountersByObjHashes(
                ZZ.splitParamAsIntegerSet(objHashes),
                ZZ.splitParamStringSet(counterNameByCommaSeparator),
                ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }

    /**
     * get counter values of specific time range by object type
     * uri pattern : /counter/{counter}/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /counter/{counter}/ofType/{objType}?startYmdHms={startYmdHms}&endYmdHms={endYmdHms}&serverId={serverId}
     *
     * @param request
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/{counter}/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<CounterView>> retrieveCounterByObjType(@BeanParam @Valid CounterRequestByType request) {
        request.validate();
        List<CounterView> counterView = counterService.retrieveCounterByObjType(request);
        return CommonResultView.success(counterView);
    }

    /**
     * get counter values of specific time range by object hashes
     * uri pattern : /counter/{counter}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&objHashes=100,200&serverId={serverId}
     * uri pattern : /counter/{counter}?startYmdHms={startYmdHms}&endYmdHms={endYmdHms}&objHashes=100,200&serverId={serverId}
     *
     * @param request
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/{counter}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<CounterView>> retrieveCounterByObjHashes(@BeanParam @Valid CounterRequestByObjHashes request) {
        request.validate();
        List<CounterView> counterView = counterService.retrieveCounterByObjHashes(request);
        return CommonResultView.success(counterView);
    }

    /**
     * get the specific counter's values about a type within given duration
     * uri : /counter/stat/{counter}/ofType/{objType}?serverId=1001010&fromYmd=20170809&toYmd=20170810
     *
     * @param request @see {@link CounterAvgRequestByType}
     */
    @GET
    @Path("/stat/{counter}/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<AvgCounterView>> retrieveAvgCounterByObjType(@BeanParam @Valid CounterAvgRequestByType request) {
        List<AvgCounterView> counterViewList = counterService.retrieveAvgCounterByObjType(request);
        return CommonResultView.success(counterViewList);

    }

    /**
     * get the specific counter's values about an object within given duration
     * uri : /counter/stat/{counter}?serverId=1001010&fromYmd=20170809&toYmd=20170810&objHashes=100,200
     *
     * @param request @see {@link CounterAvgRequestByObjHashes}
     */
    @GET
    @Path("/stat/{counter}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<AvgCounterView>> retrieveAvgCounterByObjHash(@BeanParam @Valid CounterAvgRequestByObjHashes request) {
        List<AvgCounterView> counterViewList = counterService.retrieveAvgCounterByObjHashes(request);
        return CommonResultView.success(counterViewList);
    }

    /**
     * get counter values in latest x seconds by object type
     * uri pattern : /counter/{counter}/latest/{latestSec}/ofType/{objType}?serverId={serverId}
     *
     * @param request
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/{counter}/latest/{latestSec}/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<CounterView>> retrieveLatestCounterByObjType(@BeanParam @Valid LatestCounterRequestByType request) {
        request.validate();
        List<CounterView> counterView = counterService.retrieveCounterByObjType(request.toCounterRequestByType());
        return CommonResultView.success(counterView);
    }

    /**
     * get counter values in latest x seconds by object hash
     * uri pattern : /counter/{counter}/latest/{latestSec}?serverId={serverId}&objHashes=100,200
     *
     * @param request
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/{counter}/latest/{latestSec}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<CounterView>> retrieveLatestCounterByObjHash(@BeanParam @Valid LatestCounterRequestByObjHashes request) {
        request.validate();
        List<CounterView> counterView = counterService.retrieveCounterByObjHashes(request.toCounterRequestByObjHashes());
        return CommonResultView.success(counterView);
    }
}
