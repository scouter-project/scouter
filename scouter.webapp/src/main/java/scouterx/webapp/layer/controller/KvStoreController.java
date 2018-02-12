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
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.KvStoreService;
import scouterx.webapp.request.SetKvRequest;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/kv")
@Api("KeyValueStore")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class KvStoreController {

    private final KvStoreService kvStoreService = new KvStoreService();

    /**
     * get value by key from scouter key-value store
     *
     */
    @GET
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<String> get(@PathParam("key") String key, @QueryParam("serverId") int serverId) {
        String result = kvStoreService.get(key, ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * set key & value on scouter key-value store
     *
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> set(SetKvRequest request) {
        kvStoreService.set(request.getKey(), request.getValue(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));
        return CommonResultView.success(true);
    }
}
