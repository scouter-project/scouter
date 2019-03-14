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
import scouterx.webapp.layer.service.InteractionCounterService;
import scouterx.webapp.model.InteractionCounterData;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 24.
 */
@Path("/v1/interactionCounter")
@Api("InteractionCounter")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class InteractionCounterController {
    @Context
    HttpServletRequest servletRequest;

    private final InteractionCounterService service;

    public InteractionCounterController() {
        this.service = new InteractionCounterService();
    }

    /**
     * get current value of interaction counters about a type
     * uri : /interactionCounter/realTime/ofType/{objType}?serverId=1001010
     *
     * @param objType
     * @param serverId
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<InteractionCounterData>> retrieveRealTimeCountersByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @QueryParam("serverId") final int serverId) {

        List<InteractionCounterData> counterList = service.retrieveRealTimeByObjType(objType,
                ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }

    /**
     * get current value of interaction counters of objects
     * uri : /interactionCounter/realTime?serverId=1001010&objHashes=100,200,300
     *
     * @see scouter.lang.counters.CounterConstants
     */
    @GET
    @Path("/realTime")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<InteractionCounterData>> retrieveRealTimeCountersByObjHashes(
            @QueryParam("objHashes") @Valid @NotNull final String objHashes,
            @QueryParam("serverId") final int serverId) {

        List<InteractionCounterData> counterList = service.retrieveRealTimeByObjHashes(ZZ.splitParamAsIntegerSet(objHashes),
                ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(counterList);
    }
}
