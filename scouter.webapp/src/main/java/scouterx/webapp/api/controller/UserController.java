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
import scouterx.webapp.api.fw.controller.ro.CommonResultView;
import scouterx.webapp.api.model.SUser;
import scouterx.webapp.api.requestmodel.LoginRequest;
import scouterx.webapp.api.service.UserService;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/user")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class UserController {
    @Context
    HttpServletRequest servletRequest;

    final UserService userService = new UserService();

    @NoAuth
    @POST @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> login(@Valid final LoginRequest loginRequest) {
        userService.login(ServerManager.getInstance().getServer(loginRequest.getServerId()), loginRequest.getUser());
        servletRequest.getSession(true).setAttribute("user", new SUser(loginRequest.getUser().getId()));

        return CommonResultView.success();
    }
}
