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

package scouterx.webapp.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateTimeHelper;
import scouterx.client.model.TextProxy;
import scouterx.client.net.TcpProxy;
import scouterx.client.server.Server;
import scouterx.model.summary.ServiceSummaryItem;
import scouterx.model.summary.Summary;
import scouterx.webapp.api.request.SummaryRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * for statistics
 *
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 13.
 */
public class SummaryConsumer {

    @Data
    private class SearchCondition {
        private String yyyymmdd;
        private long start;
        private long end;
    }

    @Data
    @AllArgsConstructor
    private class DateAndMapPack {
        private String yyyymmdd;
        private MapPack mapPack;
    }

    /**
     * retrieve service summary
     *
     * @param request {@link SummaryRequest}
     * @return
     */
    public Summary<ServiceSummaryItem> retrieveServiceSummary(SummaryRequest request) {
        String cmd = RequestCmd.LOAD_SERVICE_SUMMARY;
        List<DateAndMapPack> resultPackList = retrieveSummary(cmd, request.getStart(), request.getEnd(), request.getObjType(), request.getObjHash(), request.getServer());

        Summary<ServiceSummaryItem> summary = new Summary<>();

        for (DateAndMapPack dnmPack : resultPackList) {
            long date = DateTimeHelper.getDefault().yyyymmdd(dnmPack.getYyyymmdd());
            ListValue idList = dnmPack.getMapPack().getList("id");
            ListValue countList = dnmPack.getMapPack().getList("count");
            ListValue errorCntList = dnmPack.getMapPack().getList("error");
            ListValue elapsedSumList = dnmPack.getMapPack().getList("elapsed");
            ListValue cpuSumList = dnmPack.getMapPack().getList("cpu");
            ListValue memSumList = dnmPack.getMapPack().getList("mem");

            for(int i = 0; i < idList.size(); i++) {
                ServiceSummaryItem item = ServiceSummaryItem.builder()
                        .summaryKey(idList.getInt(i))
                        .summaryKeyName(TextProxy.service.getTextIfNullDefault(date, idList.getInt(i), request.getServer().getId()))
                        .count(countList.getInt(i))
                        .errorCount(errorCntList.getInt(i))
                        .elapsedSum(elapsedSumList.getLong(i))
                        .cpuSum(cpuSumList.getLong(i))
                        .memorySum(memSumList.getLong(i)).build();

                summary.merge(item);
            }
        }

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
            _end = helper.dateUnitToTimeMillis(dateUnit) + helper.MILLIS_PER_DAY - 1000;

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
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        paramPack.put(ParamConstant.OBJ_HASH, objHash);

        Pack resultPack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            resultPack = tcpProxy.getSingle(cmd, paramPack);
        }

        return (MapPack) resultPack;
    }
}
