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
import io.swagger.annotations.ApiOperation;
import scouterx.webapp.layer.service.SummaryService;
import scouterx.webapp.model.summary.AlertSummaryItem;
import scouterx.webapp.model.summary.ApiCallSummaryItem;
import scouterx.webapp.model.summary.ErrorSummaryItem;
import scouterx.webapp.model.summary.IpSummaryItem;
import scouterx.webapp.model.summary.ServiceSummaryItem;
import scouterx.webapp.model.summary.SqlSummaryItem;
import scouterx.webapp.model.summary.Summary;
import scouterx.webapp.model.summary.UserAgentSummaryItem;
import scouterx.webapp.request.SummaryOfObjHashRequest;
import scouterx.webapp.request.SummaryOfObjTypeRequest;
import scouterx.webapp.request.SummaryRequest;
import scouterx.webapp.view.CommonResultView;

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
@Api("Summary")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class SummaryController {

    private final SummaryService summaryService = new SummaryService();

    private final String NOTE_retrieveServiceSummaryByType =
            "* retrieve service summary data (5min precision) of specific object type within given duration.\n" +
            "  * uri pattern : /summary/service/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}\n" +
            "  * uri pattern : /summary/service/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}";

    /**
     * retrieve service summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/service/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/service/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/service/ofType/{objType}")
    @ApiOperation(value="retrieveServiceSummaryByType", notes = NOTE_retrieveServiceSummaryByType)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ServiceSummaryItem>> retrieveServiceSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveServiceSummary(request);
    }

    /**
     * retrieve service summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/service/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/service/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/service/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ServiceSummaryItem>> retrieveServiceSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveServiceSummary(request);
    }

    private CommonResultView<Summary<ServiceSummaryItem>> retrieveServiceSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveServiceSummary(request));
    }


    /**
     * retrieve sql summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/sql/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/sql/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/sql/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<SqlSummaryItem>> retrieveSqlSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveSqlSummary(request);
    }

    /**
     * retrieve sql summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/sql/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/sql/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/sql/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<SqlSummaryItem>> retrieveSqlSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveSqlSummary(request);
    }

    private CommonResultView<Summary<SqlSummaryItem>> retrieveSqlSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveSqlSummary(request));
    }

    /**
     * retrieve apiCall summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/sql/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/sql/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/apiCall/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ApiCallSummaryItem>> retrieveApiCallSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveApiCallSummary(request);
    }

    /**
     * retrieve apiCall summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/sql/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/sql/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/apiCall/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ApiCallSummaryItem>> retrieveApiCallSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveApiCallSummary(request);
    }


    private CommonResultView<Summary<ApiCallSummaryItem>> retrieveApiCallSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveApiCallSummary(request));
    }

    /**
     * retrieve ip summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/ip/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/ip/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/ip/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<IpSummaryItem>> retrieveIpSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveIpSummary(request);
    }

    /**
     * retrieve ip summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/ip/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/ip/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/ip/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<IpSummaryItem>> retrieveIpSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveIpSummary(request);
    }


    private CommonResultView<Summary<IpSummaryItem>> retrieveIpSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveIpSummary(request));
    }

    /**
     * retrieve userAgent summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/userAgent/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/userAgent/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/userAgent/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<UserAgentSummaryItem>> retrieveUserAgentSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveUserAgentSummary(request);
    }

    /**
     * retrieve userAgent summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/userAgent/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/userAgent/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/userAgent/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<UserAgentSummaryItem>> retrieveUserAgentSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveUserAgentSummary(request);
    }


    private CommonResultView<Summary<UserAgentSummaryItem>> retrieveUserAgentSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveUserAgentSummary(request));
    }

    /**
     * retrieve error summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/error/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/error/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/error/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ErrorSummaryItem>> retrieveErrorSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveErrorSummary(request);
    }

    /**
     * retrieve error summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/error/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/error/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/error/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<ErrorSummaryItem>> retrieveErrorSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveErrorSummary(request);
    }


    private CommonResultView<Summary<ErrorSummaryItem>> retrieveErrorSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveErrorSummary(request));
    }

    /**
     * retrieve alert summary data (5min precision) of specific object type within given duration.
     * uri pattern : /summary/alert/ofType/{objType}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/alert/ofType/{objType}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/alert/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<AlertSummaryItem>> retrieveAlertSummaryByType(@BeanParam @Valid SummaryOfObjTypeRequest request) {
        return retrieveAlertSummary(request);
    }

    /**
     * retrieve alert summary data (5min precision) of specific object within given date duration.
     * uri pattern : /summary/alert/ofObject/{objHash}?startTimeMillis={startTimeMillis}&endTimeMillis={endTimeMillis}&serverId={serverId}
     * uri pattern : /summary/alert/ofObject/{objHash}?startYmdHm={startYmdHm}&endYmdHm={endYmdHm}&serverId={serverId}
     *
     * @param request @see {@link SummaryRequest}
     * @return
     */
    @GET
    @Path("/alert/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Summary<AlertSummaryItem>> retrieveAlertSummaryByObj(@BeanParam @Valid SummaryOfObjHashRequest request) {
        return retrieveAlertSummary(request);
    }


    private CommonResultView<Summary<AlertSummaryItem>> retrieveAlertSummary(SummaryRequest request) {
        request.validate();
        return CommonResultView.success(summaryService.retrieveAlertSummary(request));
    }
}
