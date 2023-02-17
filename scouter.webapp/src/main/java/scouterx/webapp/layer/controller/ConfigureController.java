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
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.ConfigureService;
import scouterx.webapp.model.configure.ConfigureData;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Yosong Heo (yosong.heo@gmail.com) on 2023. 2. 17.
 */
@Path("/v1/configure")
@Api("Configure")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ConfigureController {

    private final ConfigureService configureService = new ConfigureService();
    /**
     * get server information of scouter collector server that is connected from this webapp
     *
     */
    @GET
    @Path("/server")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ConfigureData> retrieveConfig(@QueryParam("serverId") int serverId) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        ConfigureData configureData= configureService.retrieveServerConfig(server);
        return CommonResultView.success(configureData);
    }
    @GET
    @Path("/object/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ConfigureData> retrieveObjectConfig(
            @PathParam("objHash")   int objHash,
            @QueryParam("serverId") int serverId) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        ConfigureData configureData= configureService.retrieveObjectConfig(objHash,server);
        return CommonResultView.success(configureData);
    }

}
