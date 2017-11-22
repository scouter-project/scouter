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

package scouterx.webapp.layer.consumer;

import lombok.Data;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouter.util.DateTimeHelper;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.dto.DateAndMapPack;
import scouterx.webapp.model.summary.AlertSummaryItem;
import scouterx.webapp.model.summary.ApiCallSummaryItem;
import scouterx.webapp.model.summary.ErrorSummaryItem;
import scouterx.webapp.model.summary.IpSummaryItem;
import scouterx.webapp.model.summary.ServiceSummaryItem;
import scouterx.webapp.model.summary.SqlSummaryItem;
import scouterx.webapp.model.summary.Summary;
import scouterx.webapp.model.summary.UserAgentSummaryItem;
import scouterx.webapp.request.SummaryRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * for statistics
 *
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 13.
 */
public class SummaryConsumer {

    @Data
    private static class SearchCondition {
        private String yyyymmdd;
        private long start;
        private long end;
    }

    /**
     * retrieve service summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<ServiceSummaryItem> retrieveServiceSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_SERVICE_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<ServiceSummaryItem> summary = Summary.of(ServiceSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve sql summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<SqlSummaryItem> retrieveSqlSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_SQL_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<SqlSummaryItem> summary = Summary.of(SqlSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve apicall summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<ApiCallSummaryItem> retrieveApiCallSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_APICALL_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<ApiCallSummaryItem> summary = Summary.of(ApiCallSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve ip summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<IpSummaryItem> retrieveIpSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_IP_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<IpSummaryItem> summary = Summary.of(IpSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve user agent summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<UserAgentSummaryItem> retrieveUserAgentSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_UA_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<UserAgentSummaryItem> summary = Summary.of(UserAgentSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve error by service summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<ErrorSummaryItem> retrieveErrorSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_SERVICE_ERROR_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<ErrorSummaryItem> summary = Summary.of(ErrorSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * retrieve alert summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<AlertSummaryItem> retrieveAlertSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_ALERT_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStartTimeMillis(), request.getEndTimeMillis(), request.getObjType(),
                request.getObjHash(), request.getServer());

        Summary<AlertSummaryItem> summary = Summary.of(AlertSummaryItem.class, resultPackList, request.getServer().getId());

        return summary;
    }

    /**
     * get summary result pack list
     *
     */
    private List<DateAndMapPack> retrieveSummary(String cmd, long start, long end, String objType, int objHash, Server server) {
        DateTimeHelper helper = DateTimeHelper.getDefault();

        long startDateUnit = helper.getDateUnit(start);
        long endDateUnit = helper.getDateUnit(end);

        List<DateAndMapPack> dateAndMapPackList = new ArrayList<>();

        for(long dateUnit = startDateUnit; dateUnit <= endDateUnit; dateUnit++) {
            SearchCondition condition = generateSearchCondition(helper, dateUnit, startDateUnit, endDateUnit, start, end);
            MapPack summaryResultMapPack = retrieveDailySummaryPack(cmd, condition.yyyymmdd, condition.start, condition.end, objType, objHash, server);
            dateAndMapPackList.add(new DateAndMapPack(condition.yyyymmdd, summaryResultMapPack));
        }
        return dateAndMapPackList;
    }

    /**
     * utility for generating search condition
     *
     */
    private SearchCondition generateSearchCondition(final DateTimeHelper helper, final long dateUnit,
                                                    final long startDateUnit, final long endDateUnit, final long start, final long end) {
        long _start;
        String _yyyymmdd;
        long _end;

        if (dateUnit < endDateUnit) {
            if (dateUnit == startDateUnit) {
                _start = start;
            } else {
                _start = helper.dateUnitToTimeMillis(dateUnit);
            }
            _end = helper.dateUnitToTimeMillis(dateUnit) + DateTimeHelper.MILLIS_PER_DAY - 1000;

        } else { // dateUnit == endDateUnit
            if (dateUnit == startDateUnit) {
                _start = start;
            } else {
                _start = helper.dateUnitToTimeMillis(dateUnit);
            }
            _end = end;
        }
        _yyyymmdd = helper.yyyymmdd(_start);

        SearchCondition searchCondition = new SearchCondition();
        searchCondition.yyyymmdd = _yyyymmdd;
        searchCondition.start = _start;
        searchCondition.end = _end;

        return searchCondition;
    }

    /**
     * retrieve daily summary pack from collector server
     *
     */
    private MapPack retrieveDailySummaryPack(final String cmd, final String yyyymmdd, long start, long end, String objType, int objHash, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.DATE, yyyymmdd);
        paramPack.put(ParamConstant.STIME, start);
        paramPack.put(ParamConstant.ETIME, end);
        if (objType != null) {
            paramPack.put(ParamConstant.OBJ_TYPE, objType);
        }
        paramPack.put(ParamConstant.OBJ_HASH, objHash);

        Pack resultPack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            resultPack = tcpProxy.getSingle(cmd, paramPack);
        }

        return (MapPack) resultPack;
    }
}
