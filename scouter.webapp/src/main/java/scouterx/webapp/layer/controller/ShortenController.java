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
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.CustomKvStoreService;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static scouter.lang.constants.ScouterConstants.SHORTENER_KEY_SPACE;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/shortener")
@Api("ShortenURL")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ShortenController {
    @Context
    HttpServletRequest servletRequest;

    private final CustomKvStoreService kvStoreService = new CustomKvStoreService();

    /**
     * get stored url
     */
    @GET
    @Path("/{key}")
    public CommonResultView<String> getShortenUrl(@PathParam("key") String key, @QueryParam("serverId") int serverId) {

        String result = kvStoreService.get(SHORTENER_KEY_SPACE, key, ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * get shorten url
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<String> makeShortenUrl(@QueryParam("url") String url, @QueryParam("serverId") int serverId) {
        String hashed = Hexa32.toString32(HashUtil.hash(url));
        kvStoreService.set(SHORTENER_KEY_SPACE, hashed, url, ServerManager.getInstance().getServerIfNullDefault(serverId));

        String reqUrl = servletRequest.getRequestURL().toString();
        String shortenerServiceUrl = reqUrl.substring(0, reqUrl.indexOf(servletRequest.getPathInfo()));
        return CommonResultView.success(shortenerServiceUrl + "/s/" + hashed);
    }
}
