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
import scouterx.webapp.annotation.NoAuth;
import scouterx.webapp.api.view.ServerView;
import scouterx.webapp.api.view.CommonResultView;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 28.
 */
@Path("/v1/info")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class InfoController {
    @NoAuth
    @GET @Path("/server")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ServerView> retrieveServers() {
        List<ServerView> serverList = ServerManager.getInstance().getAllServerList().stream()
                .map(s -> new ServerView(s.getId(), s.getName(), s.isConnected(), System.currentTimeMillis()-s.getDelta()))
                .collect(Collectors.toList());

        return CommonResultView.success(serverList);
    }
}
