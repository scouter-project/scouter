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
import scouterx.webapp.framework.annotation.NoAuth;
import scouterx.webapp.framework.client.server.ServerManager;
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

import static scouter.lang.constants.ScouterConstants.SHORTENER_KEY_SPACE;

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
    @NoAuth
    @GET
    @Path("/s/{key}")
    public Response redirectByKey(@PathParam("key") String key, @QueryParam("serverId") int serverId) {
        URI targetURIForRedirection = null;
        String boxes = "{\"lg\":[{\"w\":6,\"h\":5,\"x\":0,\"y\":0,\"i\":\"4\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":5,\"x\":6,\"y\":0,\"i\":\"5\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":5,\"x\":0,\"y\":5,\"i\":\"3\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":5,\"x\":6,\"y\":5,\"i\":\"6\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":5,\"x\":0,\"y\":10,\"i\":\"7\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":5,\"x\":6,\"y\":10,\"i\":\"8\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false}],\"sm\":[{\"w\":6,\"h\":8,\"x\":0,\"y\":10,\"i\":\"4\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":6,\"h\":8,\"x\":0,\"y\":18,\"i\":\"5\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false}],\"xs\":[{\"w\":4,\"h\":8,\"x\":0,\"y\":15,\"i\":\"4\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":4,\"h\":8,\"x\":0,\"y\":23,\"i\":\"5\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false}],\"xxs\":[{\"w\":2,\"h\":8,\"x\":0,\"y\":15,\"i\":\"4\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":2,\"h\":8,\"x\":0,\"y\":23,\"i\":\"5\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false}],\"md\":[{\"w\":5,\"h\":8,\"x\":0,\"y\":5,\"i\":\"4\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false},{\"w\":5,\"h\":8,\"x\":5,\"y\":5,\"i\":\"5\",\"minW\":1,\"minH\":3,\"moved\":false,\"static\":false}]}";

        String result = kvStoreService.get(SHORTENER_KEY_SPACE, key, ServerManager.getInstance().getServerIfNullDefault(serverId));
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
