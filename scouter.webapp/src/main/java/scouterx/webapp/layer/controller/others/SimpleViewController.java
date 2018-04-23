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

package scouterx.webapp.layer.controller.others;

import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.controller.ShortenController;
import scouterx.webapp.layer.service.CustomKvStoreService;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 4. 20.
 */
@Singleton
@Produces(MediaType.TEXT_HTML)
@Path("")
public class SimpleViewController {
    private final CustomKvStoreService kvStoreService = new CustomKvStoreService();

    /**
     * redirect to
     */
    @GET
    @Path("/s/{key}")
    public Response redirectByKey(@PathParam("key") String key, @QueryParam("serverId") int serverId) {
        URI targetURIForRedirection = null;

        String result = kvStoreService.get(ShortenController.SHORTENER_KEY_SPACE, key, ServerManager.getInstance().getServerIfNullDefault(serverId));
        if (StringUtils.isNotBlank(result)) {
            try {
                targetURIForRedirection = new URI(result);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (targetURIForRedirection != null) {
            return Response.seeOther(targetURIForRedirection).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
