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

import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.model.scouter.SObject;
import scouterx.webapp.layer.service.ObjectService;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/object")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ObjectController {
    @Context
    HttpServletRequest servletRequest;

    private final ObjectService agentService;

    public ObjectController() {
        this.agentService = new ObjectService();
    }

    /**
     * get agent list that is monitored by scouter
     *
     * @param serverId optional if web instance just connected one collector server.
     * @return
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<SObject>> retrieveObjectList(@QueryParam("serverId") int serverId) {
        List<SObject> agentList = agentService.retrieveObjectList(ServerManager.getInstance().getServer(serverId));

        return CommonResultView.success(agentList);
    }
}
