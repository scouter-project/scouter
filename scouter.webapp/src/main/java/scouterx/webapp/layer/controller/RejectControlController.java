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
import scouterx.webapp.layer.service.RejectControlService;
import scouterx.webapp.model.RejectControlStatusData;
import scouterx.webapp.request.AddRejectBlockRequest;
import scouterx.webapp.request.SetRejectPolicyRequest;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Management API of the central reject(block) control.
 *
 * Blocks have no TTL by design - {@code DELETE /block} is the only way an entry
 * is released. The status response therefore also carries the detection
 * candidates so an operator can review a violation before making it permanent.
 *
 * Changes reach the agents on the collector's next sync cycle
 * (reject_control_sync_interval_ms, 2s by default), not instantly.
 */
@Path("/v1/reject-control")
@Api("RejectControl")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class RejectControlController {

    private final RejectControlService rejectControlService = new RejectControlService();

    /**
     * current block list + detection candidates
     * ex) GET /scouter/v1/reject-control/status
     */
    @GET
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<RejectControlStatusData> getStatus(@QueryParam("serverId") int serverId) {
        RejectControlStatusData result = rejectControlService.getStatus(
                ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * block an ip, or an (ip + url) pair
     * ex) POST /scouter/v1/reject-control/block
     *     {"ip":"1.2.3.4", "url":"/login", "reason":"MANUAL", "by":"gunlee"}
     */
    @POST
    @Path("/block")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> addBlock(@Valid AddRejectBlockRequest request) {
        boolean result = rejectControlService.addBlock(
                request.getIp(), request.getUrl(), request.getReason(), request.getBy(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));
        return CommonResultView.success(result);
    }

    /**
     * release a block. this is the only way an entry leaves the list.
     * ex) DELETE /scouter/v1/reject-control/block?ip=1.2.3.4&url=/login
     */
    @DELETE
    @Path("/block")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> removeBlock(@QueryParam("ip") String ip,
                                                 @QueryParam("url") String url,
                                                 @QueryParam("serverId") int serverId) {
        boolean result = rejectControlService.removeBlock(ip, url,
                ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * clear the whole block list, or only the detection candidates.
     * ex) DELETE /scouter/v1/reject-control/blocks?target=detection
     */
    @DELETE
    @Path("/blocks")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Integer> clear(@QueryParam("target") String target,
                                           @QueryParam("serverId") int serverId) {
        int removed = rejectControlService.clear(target,
                ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(removed);
    }

    /**
     * globally managed policy. keys omitted fall back to each agent's local config.
     * ex) GET /scouter/v1/reject-control/policy
     */
    @GET
    @Path("/policy")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Map<String, String>> getPolicy(@QueryParam("serverId") int serverId) {
        Map<String, String> result = rejectControlService.getPolicy(
                ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(result);
    }

    /**
     * replace the global policy.
     * ex) PUT /scouter/v1/reject-control/policy
     *     {"policy":{"enabled":"true","rate_enabled":"true","rate_window_ms":"10000","rate_max_count":"300"}}
     */
    @PUT
    @Path("/policy")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Boolean> setPolicy(@Valid SetRejectPolicyRequest request) {
        boolean result = rejectControlService.setPolicy(request.getPolicy(),
                ServerManager.getInstance().getServerIfNullDefault(request.getServerId()));
        return CommonResultView.success(result);
    }
}
