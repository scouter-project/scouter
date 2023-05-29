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
import scouterx.webapp.layer.service.AlertService;
import scouterx.webapp.model.alertscript.ScriptingLoadData;
import scouterx.webapp.model.alertscript.ScriptingLogStateData;
import scouterx.webapp.model.alertscript.ScriptingSaveStateData;
import scouterx.webapp.request.RealTimeAlertRequest;
import scouterx.webapp.request.SetConfigRequest;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.view.RealTimeAlertView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/alert")
@Api("Alert")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class AlertController {

    private final AlertService alertService = new AlertService();

    /**
     * retrieve current alerts unread that is produced newly after the last offsets.
     * uri pattern : /alert/realTime/{offset1}/{offset2}?objType={objType}&serverId={serverId}
     *
     * @param request @see {@link RealTimeAlertRequest}
     * @return
     */
    @GET
    @Path("/realTime/{offset1}/{offset2}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<RealTimeAlertView> retrieveRealTimeAlert(@BeanParam @Valid RealTimeAlertRequest request) {

        return CommonResultView.success(alertService.retrieveRealTimeAlert(request));
    }

    /**
     * retrieve counter alert setting
     */
    @GET
    @Path("/scripting/{counterName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ScriptingLoadData> retrieveAlertScripting(@PathParam("counterName") String counterName,
                                                                      @QueryParam("serverId") int serverId
                                                                      ) {
        return CommonResultView.success(this.alertService.loadScripting(serverId,counterName));
    }

    /**
     * save counter alert config setting
     */
    @POST
    @Path("/set/config/scripting/{counterName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ScriptingSaveStateData> saveConfigAlertScripting(@PathParam("counterName") String counterName,
                                                                             @QueryParam("serverId") int serverId,
                                                                             @Valid SetConfigRequest setConfigRequest
    ) {

        return CommonResultView.success(this.alertService.setConfigScripting(serverId,counterName,setConfigRequest));
    }

    /**
     * save counter alert rule setting
     */
    @POST
    @Path("/set/rule/scripting/{counterName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ScriptingSaveStateData> setRuleAlertScripting(@PathParam("counterName") String counterName,
                                                                          @QueryParam("serverId") int serverId,
                                                                          @Valid SetConfigRequest setConfigRequest
    ) {
        return CommonResultView.success(this.alertService.setRuleScripting(serverId,counterName,setConfigRequest));
    }


    /**
     * retrieve current alert script compile log unread that is produced newly after the last offsets.
     */
    @GET
    @Path("/read/scripting/{loop}/{index}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<ScriptingLogStateData> readAlertScripting(@PathParam("loop") long loop,
                                                                      @PathParam("index") long index,
                                                                      @QueryParam("serverId") int serverId
    ) {
        return CommonResultView.success(this.alertService.readAlertScripting(serverId,loop,index));
    }
}
