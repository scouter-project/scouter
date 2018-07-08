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
import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.session.UserToken;
import scouterx.webapp.framework.session.WebRequestContext;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.CustomKvStoreService;
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
import java.util.stream.Collectors;

@Path("/v1/kv-private")
@Api("PrivateKeyValueStore - login user private space")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class PrivateKvStoreController {
    private static final String KEYSPACE_WEB_SESSION_PRIVATE = "___WEB_SESSION_PRIVATE___";

    private final CustomKvStoreService kvStoreService = new CustomKvStoreService();

    /**
     * get value by key from the login user's key-value store
     */
    @GET
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<String> get(@PathParam("key") String key, @QueryParam("serverId") int serverId) {
        String result = kvStoreService.get(KEYSPACE_WEB_SESSION_PRIVATE,
                toPrivateKey(key), ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(result);
    }

    /**
     * store key & value onto the login user's key-value store
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> set(SetKvRequest request) {
        kvStoreService.set(KEYSPACE_WEB_SESSION_PRIVATE, toPrivateKey(request.getKey()), request.getValue(),
                request.getTtl(), ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));

        return CommonResultView.success(true);
    }

    /**
     * set ttl onto the login user's key-value store
     */
    @PUT
    @Path("/{key}/:ttl")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> set(@PathParam("key") String key, SetKvTTLRequest request) {
        kvStoreService.setTTL(KEYSPACE_WEB_SESSION_PRIVATE, toPrivateKey(key), request.getTtl(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));

        return CommonResultView.success(true);
    }

    /**
     * get values by keys from the login user's key-value store
     */
    @GET
    @Path("/{keys}/:bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<KeyValueData>> getBulk(@PathParam("keys") final String keyBySeparator,
                                                        @QueryParam("serverId") final int serverId) {
        List<KeyValueData> resultList = kvStoreService.getBulk(KEYSPACE_WEB_SESSION_PRIVATE,
                ZZ.splitParam(keyBySeparator).stream().map(this::toPrivateKey).collect(Collectors.toList()),
                ServerManager.getInstance().getServerIfNullDefault(serverId));

        List<KeyValueData> keyGeneralizedList = resultList.stream()
                .peek(d -> d.setKey(toGeneralKey(d.getKey())))
                .collect(Collectors.toList());

        return CommonResultView.success(keyGeneralizedList);
    }

    /**
     * store key-values on the login user's key-value store
     */
    @PUT
    @Path("/:bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<KeyValueData>> setBulk(SetKvBulkRequest request) {
        List<KeyValueData> resultList = kvStoreService.setBulk(KEYSPACE_WEB_SESSION_PRIVATE,
                request.toMapPadKeyPrefix(getPrivateKeyPrefix()), request.getTtl(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));

        List<KeyValueData> keyGeneralizedList = resultList.stream()
                .peek(d -> d.setKey(toGeneralKey(d.getKey())))
                .collect(Collectors.toList());

        return CommonResultView.success(keyGeneralizedList);
    }

    private String getPrivateKeyPrefix() {
        UserToken userToken = WebRequestContext.getUserToken();
        if (userToken == null) {
            ErrorState.LOGIN_REQUIRED.newBizException();
        }
        return "]" + userToken.getUserId() + ":";
    }

    private String toPrivateKey(String key) {
        return getPrivateKeyPrefix() + key;
    }

    private String toGeneralKey(String key) {
        String prefix = getPrivateKeyPrefix();
        if (!StringUtils.startsWith(key, prefix)) {
            ErrorState.ILLEGAL_KEY_ACCESS.newBizException();
        }

        return StringUtils.substring(key, prefix.length());
    }
}
