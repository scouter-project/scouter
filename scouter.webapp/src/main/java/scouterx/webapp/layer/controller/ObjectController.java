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

import io.swagger.annotations.*;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.ObjectService;
import scouterx.webapp.model.HeapHistogramData;
import scouterx.webapp.model.SocketObjectData;
import scouterx.webapp.model.ThreadObjectData;
import scouterx.webapp.model.VariableData;
import scouterx.webapp.model.scouter.SObject;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 26.
 */
@Path("/v1/object")
@Api("Object")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ObjectController {
    @Context
    HttpServletRequest servletRequest;

    private final ObjectService agentService;

    public ObjectController() {
        this.agentService = new ObjectService();
    }

    @GET
    @ApiOperation(value = "/", notes = "get agent list that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SObject>> retrieveObjectList(@QueryParam("serverId") int serverId) {

        List<SObject> agentList = agentService.retrieveObjectList(ServerManager.getInstance().getServer(serverId));

        return CommonResultView.success(agentList);
    }

    @GET
    @ApiOperation(value = "/remove/inactive", notes = "remove inactive object. target all connected servers")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/remove/inactive")
    public CommonResultView removeInactiveByAll() {
        AgentModelThread.removeInactiveByAll();
        return CommonResultView.success();
    }
    @GET
    @ApiOperation(value = "/remove/inactive/server", notes = "remove inactive object. target by serverId ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @Path("/remove/inactive/server")
    public CommonResultView removeInactiveByServerId(@QueryParam("serverId") int serverId) {
        AgentModelThread.removeInactiveByServerId(serverId);
        return CommonResultView.success();
    }


    @GET
    @ApiOperation(value = "/threadList/{objHash}", notes = "get agent thread list that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "objHash", value = "object type", required = true, dataType = "int", paramType = "path"),
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/threadList/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ThreadObjectData>> retrieveThreadList(@PathParam("objHash") @Valid @NotNull int objHash,
                                                                       @QueryParam("serverId") int serverId) {

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<ThreadObjectData> list = agentService.retrieveThreadList(objHash, server);
        return CommonResultView.success(list);

    }

    @GET
    @ApiOperation(value = "/threadDump/{objHash}", notes = "get agent thread dump that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "objHash", value = "object type", required = true, dataType = "int", paramType = "path"),
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/threadDump/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<String> retrieveThreadDump(@PathParam("objHash") @Valid @NotNull int objHash,
                                                                   @QueryParam("serverId") int serverId) {

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        String dump = agentService.retrieveThreadDump(objHash, server);
        return CommonResultView.success(dump);

    }

    @GET
    @ApiOperation(value = "/heapHistogram/{objHash}", notes = "get agent Heap histogram that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "objHash", value = "object type", required = true, dataType = "int", paramType = "path"),
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/heapHistogram/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<HeapHistogramData>> retrieveHeapHistogram(@PathParam("objHash") @Valid @NotNull final int objHash,
                                                                           @QueryParam("serverId") int serverId) {

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<HeapHistogramData> list = agentService.retrieveHeapHistogram(objHash, server);
        return CommonResultView.success(list);

    }

    @GET
    @ApiOperation(value = "/env/{objHash}", notes = "get agent environment info that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "objHash", value = "object type", required = true, dataType = "int", paramType = "path"),
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/env/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<VariableData>> retrieveEnv(@PathParam("objHash") @Valid @NotNull int objHash,
                                                                           @QueryParam("serverId") int serverId) {

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<VariableData> list = agentService.retrieveEnv(objHash, server);

        return CommonResultView.success(list);

    }

    @GET
    @ApiOperation(value = "/socket/{objHash}", notes = "get agent socket info that is monitored by scouter")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "objHash", value = "object type", required = true, dataType = "int", paramType = "path"),
            @ApiImplicitParam(name = "serverId", value = "server id", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/socket/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SocketObjectData>> retrieveSocket(@PathParam("objHash") @Valid @NotNull int objHash,
                                                            @QueryParam("serverId") int serverId) {

        List<SocketObjectData> list = agentService.retrieveSocket(objHash, serverId);
        return CommonResultView.success(list);

    }

}