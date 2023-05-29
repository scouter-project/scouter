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
import scouterx.webapp.model.configure.ConfObjectState;
import scouterx.webapp.model.configure.ConfigureData;
import scouterx.webapp.request.SetConfigKvRequest;
import scouterx.webapp.request.SetConfigRequest;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

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
     * get server setting information of scouter collector server that is config from this webapp
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
    /**
     * get agent setting information of scouter collector server that is connected from this webapp
     *
     */
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
    /**
     * save server setting of scouter collector server that is connected from this webapp
     *
     */
//-  서버 개별 설정
    @POST
    @Path("/set/server")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ConfigureData> setServerConfig(
            @QueryParam("serverId") int serverId,
            @Valid final SetConfigRequest configRequest) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        ConfigureData configureData= configureService.saveServerConfig(configRequest,server);
        return CommonResultView.success(configureData);
    }
    /**
     * save agent setting of scouter collector server that is connected from this webapp
     *
     */
//-  에이전트 개별 설정
    @POST
    @Path("/set/object/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ConfigureData> setObjectConfig(
            @PathParam("objHash")   int objHash,
            @QueryParam("serverId") int serverId,
            @Valid final SetConfigRequest configRequest) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        ConfigureData configureData= configureService.saveObjectConfig(configRequest,objHash,server);
        return CommonResultView.success(configureData);
    }

    /**
     * save key value server setting of scouter collector server that is connected from this webapp
     *
     */
//-  Key Value 설정 지원 여부 | 개별 또는 전체 기준
    @PUT
    @Path("/set/kv/server")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> setKVServerConfig(
            @QueryParam("serverId") int serverId,
            @Valid final SetConfigKvRequest configRequest) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        final boolean settingState= configureService.saveKVServerConfig(configRequest,server);
        return CommonResultView.success(settingState);
    }
    /**
     * save key value agent setting of scouter collector server that is connected from this webapp
     *
     */
    @PUT
    @Path("/set/kv/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ConfObjectState>> setKVObjectConfig(
            @PathParam("objType")   String objType,
            @QueryParam("serverId") int serverId,
            @Valid final SetConfigKvRequest configRequest) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<ConfObjectState> resultList= configureService.saveObjTypConfig(objType,server,configRequest);
        return CommonResultView.success(resultList);
    }
}
