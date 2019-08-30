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
import scouterx.webapp.layer.service.UserService;
import scouterx.webapp.layer.service.UserTokenService;
import scouterx.webapp.request.LoginRequest;
import scouterx.webapp.view.BearerTokenView;
import scouterx.webapp.view.CommonResultView;

import javax.crypto.KeyGenerator;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/user")
@Api("User")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class UserController {
    @Context
    HttpServletRequest servletRequest;

    final UserService userService = new UserService();
    final UserTokenService userTokenService = new UserTokenService();

    /**
     * traditional webapplication login for web client application ( success will response "set cookie JSESSIONID" )
     *
     * @param loginRequest @see {@link LoginRequest}
     */
    @NoAuth
    @POST @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> login(@Valid final LoginRequest loginRequest) {
        userService.login(ServerManager.getInstance().getServer(loginRequest.getServerId()), loginRequest.getUser());
        servletRequest.getSession(true).setAttribute("userId", loginRequest.getUser().getId());

        return CommonResultView.success();
    }

    /**
     * login for 3rd party application ( success will be responsed with Bearer Token which should be exist in the 'Authorization' header from next request.)
     *
     * @param loginRequest @see {@link LoginRequest}
     */
    @NoAuth
    @POST @Path("/loginGetToken")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<BearerTokenView> login3rdParty(@Valid final LoginRequest loginRequest) {
        Server server = ServerManager.getInstance().getServer(loginRequest.getServerId());
        userService.login(server, loginRequest.getUser());
        String bearerToken = userTokenService.publishToken(server, loginRequest.getUser());

        return CommonResultView.success(new BearerTokenView(true, bearerToken));
    }

    public static void main(String[] args) {
        Key key;
        SecureRandom rand = new SecureRandom();
        KeyGenerator generator = null;
        try {
            generator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        generator.init(256, rand);
        key = generator.generateKey();


    }
}
