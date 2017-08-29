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
import scouterx.webapp.api.fw.controller.ro.CommonResultView;
import scouterx.webapp.api.service.XLogService;
import scouterx.webapp.api.viewmodel.RealTimeXLogView;
import scouterx.webapp.util.ZZ;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Path("/v1/xlog")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class XLogController {
    private final XLogService xLogService;

    public XLogController() {
        this.xLogService = new XLogService();
    }

    /**
     * get values of several counters for given an object
     * uri : /xlog/realTime/0/100?objHashes=10001,10002 or ?objHashes=[10001,100002]
     * @param objHashByCommaSeparator
     * @param serverId
     */
    @GET @Path("/realTime/{xLogLoop}/{xLogIndex}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<RealTimeXLogView> retrieveRealTimeXLog(
            @QueryParam("objHashes") final String objHashByCommaSeparator,
            @PathParam("xLogLoop") final int xLogLoop,
            @PathParam("xLogIndex") final int xLogIndex,
            @QueryParam("serverId") final int serverId) {

        Object o = ZZ.<Integer>splitParam(objHashByCommaSeparator);

        RealTimeXLogView xLogView = xLogService.retrieveRealTimeXLog(
                ServerManager.getInstance().getServer(serverId),
                ZZ.splitParamAsInteger(objHashByCommaSeparator),
                xLogIndex,
                xLogLoop);

        return CommonResultView.success(xLogView);
    }
}
