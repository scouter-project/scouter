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

import scouterx.webapp.api.fw.controller.ro.CommonResultView;
import scouterx.webapp.api.model.counter.SCounter;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Path("/v1/xlog")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class XLogController {
    @Context
    HttpServletRequest servletRequest;

//    private final CounterService counterService;
//
//    public CounterController() {
//        this.counterService = new CounterService();
//    }

    @GET
    @Path("/realTime/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SCounter>> retrieveRealTimeXLog(
            @PathParam("objType")  @Valid @NotNull final String objType,
            @QueryParam("counters") @Valid @NotNull final String counterNameByCommaSeparator,
            @QueryParam("serverId") final int serverId) {

        return null;
    }
}
