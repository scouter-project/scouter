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
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.GlobalKvStoreService;
import scouterx.webapp.model.KeyValueData;
import scouterx.webapp.request.SetKvBulkRequest;
import scouterx.webapp.request.SetKvRequest;
import scouterx.webapp.request.SetKvTTLRequest;
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
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/kv")
@Api("GlobalKeyValueStore")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class GlobalKvStoreController {

    private final GlobalKvStoreService kvStoreService = new GlobalKvStoreService();

    /**
     * get value by key from scouter key-value store
     */
    @GET
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<String> get(@PathParam("key") String key, @QueryParam("serverId") int serverId) {
        String result = kvStoreService.get(key, ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * store key & value onto scouter key-value store
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> set(SetKvRequest request) {
        kvStoreService.set(request.getKey(), request.getValue(), request.getTtl(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));
        return CommonResultView.success(true);
    }

    /**
     * set ttl onto scouter key-value store
     */
    @PUT
    @Path("/{key}/:ttl")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> set(@PathParam("key") String key, SetKvTTLRequest request) {

        kvStoreService.setTTL(key, request.getTtl(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));

        return CommonResultView.success(true);
    }

    /**
     * get values by keys from scouter key-value store
     */
    @GET
    @Path("/{keys}/:bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<KeyValueData>> getBulk(@PathParam("keys") final String keyBySeparator,
                                                        @QueryParam("serverId") final int serverId) {
        List<KeyValueData> resultList = kvStoreService.getBulk(ZZ.splitParam(keyBySeparator),
                                                               ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(resultList);
    }

    /**
     * store key-values on scouter key-value store
     */
    @PUT
    @Path("/:bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<KeyValueData>> setBulk(SetKvBulkRequest request) {
        List<KeyValueData> resultList = kvStoreService.setBulk(request.toMap(), request.getTtl(), ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));
        return CommonResultView.success(resultList);
    }
}
