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

import scouterx.webapp.api.request.RealTimeAlertRequest;
import scouterx.webapp.api.view.CommonResultView;
import scouterx.webapp.api.view.RealTimeAlertView;
import scouterx.webapp.service.AlertService;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Path("/v1/alert")
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
}
