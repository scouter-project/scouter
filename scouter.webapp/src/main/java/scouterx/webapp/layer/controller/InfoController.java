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
import scouterx.webapp.framework.annotation.NoAuth;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.countermodel.CounterModelData;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.view.ServerView;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 28.
 */
@Path("/v1/info")
@Api("Info")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class InfoController {

    /**
     * get server information of scouter collector server that is connected from this webapp
     *
     */
    @NoAuth
    @GET @Path("/server")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ServerView>> retrieveServers() {
        List<ServerView> serverList = ServerManager.getInstance().getAllServerList().stream()
                .map(s -> new ServerView(s.getId(),
                        s.getName(),
                        s.getSession() != 0,
                        System.currentTimeMillis()-s.getDelta(),
                        s.getVersion()))
                .collect(Collectors.toList());

        return CommonResultView.success(serverList);
    }

    /**
     * get counter information
     *
     */
    @GET @Path("/counter-model")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<CounterModelData> retrieveCounterModel(@QueryParam("serverId") int serverId) {
        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        CounterModelData counterModelData = CounterModelData.of(server.getCounterEngine());

        return CommonResultView.success(counterModelData);
    }
}
