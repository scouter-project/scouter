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

import scouterx.client.server.ServerManager;
import scouterx.framework.exception.ErrorState;
import scouterx.webapp.api.view.CommonResultView;
import scouterx.model.scouter.SActiveService;
import scouterx.model.scouter.SCounter;
import scouterx.webapp.api.request.CounterRequestByType;
import scouterx.webapp.api.service.CounterService;
import scouterx.webapp.api.view.CounterView;
import scouterx.framework.util.ZZ;

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
     * get current values of several counters for objects given types
     * uri : /counter/realTime/each/counters/{objType}/byType?serverId=1001010&counters=GcCount,GcTime or ?counters=[GcCount,GcTime]
     *
     * @param objType
     * @param counterNameByCommaSeparator
     * @param serverId
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/each/counters/{objType}/byType")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SCounter>> retrieveRealTimeCountersByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @QueryParam("counters") @Valid @NotNull final String counterNameByCommaSeparator,
            @QueryParam("serverId") final int serverId) {

        List<SCounter> counterList = counterService.retrieveRealTimeCountersByObjType(
                objType, ZZ.splitParam(counterNameByCommaSeparator), ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }

    @GET
    @Path("/realTime/activeService/{objType}/byType")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SActiveService>> retrieveRealTimeActiveServiceByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @QueryParam("serverId") final int serverId) {

        List<SActiveService> activeServiceList = counterService.retrieveRealTimeActiveServiceByObjType(
                objType, ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(activeServiceList);
    }

    /**
     * get current values of several counters for given an object
     * uri : /counter/realTime/each/counters/{objHash}?counters=GcCount,GcTime or ?counters=[GcCount,GcTime]
     *
     * @param objHash
     * @param counterNameByCommaSeparator
     * @param serverId
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/each/counters/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> retrieveRealTimeCountersByObjId(
            @PathParam("objHash") final int objHash,
            @QueryParam("counters") final String counterNameByCommaSeparator,
            @QueryParam("serverId") final int serverId) {

        //TODO
        ErrorState.throwNotImplementedException();
        return null;
    }

    @GET
    @Path("/realTime/each/{counter}/{objType}/byType")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> retrieveRealTimeCounterByObjType(
            @PathParam("counter") final String counterName,
            @PathParam("objType") final String objType,
            @QueryParam("serverId") final int serverId) {

        //TODO
        ErrorState.throwNotImplementedException();
        return null;
    }

    @GET
    @Path("/realTime/each/{counter}/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> retrieveRealTimeCounterByObjId(
            @PathParam("counter") final String counterName,
            @PathParam("objHash") final int objHash,
            @QueryParam("serverId") final int serverId) {

        //TODO
        ErrorState.throwNotImplementedException();
        return null;
    }

    /**
     * get values of several counters for objects given types
     * uri : /counter/each/{counter}/{objType}/byType?serverId=1001010&fromYmd=20170809&toYmd=20170810
     *
     * @param request @see {@link CounterRequestByType}
     */
    @GET
    @Path("/each/{counter}/{objType}/byType")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SCounter>> retrieveRealTimeCountersByObjType(@BeanParam @Valid CounterRequestByType request) {
        List<CounterView> counterViewList = counterService.retrieveCounterByObjType(request);
        return CommonResultView.success(counterViewList);

    }
}
