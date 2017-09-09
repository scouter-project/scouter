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
import scouterx.webapp.api.view.CommonResultView;
import scouterx.model.scouter.SObject;
import scouterx.webapp.service.AgentService;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/agent")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class AgentController {
    @Context
    HttpServletRequest servletRequest;

    private final AgentService agentService;

    public AgentController() {
        this.agentService = new AgentService();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<SObject> retrieveAgentList(@QueryParam("serverId") int serverId) {
        List<SObject> agentList = agentService.retrieveAgentList(ServerManager.getInstance().getServer(serverId));

        return CommonResultView.success(agentList);
    }
}
