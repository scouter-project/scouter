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

import scouterx.webapp.model.summary.ServiceSummaryItem;
import scouterx.webapp.model.summary.Summary;
import scouterx.webapp.request.RealTimeAlertRequest;
import scouterx.webapp.request.SummaryRequest;
import scouterx.webapp.view.CommonResultView;
import scouterx.webapp.layer.service.SummaryService;

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
@Path("/v1/summary")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class SummaryController {

    private final SummaryService summaryService = new SummaryService();

    /**
     * retrieve service summary data within given date range.
     * uri pattern : /summary/ofType/{objType}?start={start}&end={end}&serverId={serverId}
     *
     * @param request @see {@link RealTimeAlertRequest}
     * @return
     */
    @GET
    @Path("/service/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ServiceSummaryItem>> retrieveServiceSummaryByType(@BeanParam @Valid SummaryRequest request) {
        return retrieveSummary(request);
    }

    @GET
    @Path("/service/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ServiceSummaryItem>> retrieveServiceSummaryByObj(@BeanParam @Valid SummaryRequest request) {
        return retrieveSummary(request);
    }

    private CommonResultView<Summary<ServiceSummaryItem>> retrieveSummary(SummaryRequest request) {
        return CommonResultView.success(summaryService.retrieveServiceSummary(request));
    }
}
