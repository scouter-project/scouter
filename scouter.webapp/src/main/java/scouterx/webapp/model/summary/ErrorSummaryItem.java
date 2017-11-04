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

package scouterx.webapp.model.summary;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import scouter.lang.value.ListValue;
import scouter.util.DateTimeHelper;
import scouterx.webapp.framework.client.model.TextProxy;
import scouterx.webapp.framework.dto.DateAndMapPack;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 14.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ErrorSummaryItem extends SummaryItem<ErrorSummaryItem> {
    private String error;
    private String service;
    private String errorMessage;
    private long txid;
    private String sql;
    private String apiCall;
    private String fullStack;

    @Builder
    public ErrorSummaryItem(int summaryKey, String summaryKeyName, int count, String error, String service, String errorMessage,
                            long txid, String sql, String apiCall, String fullStack) {
        this.summaryKey = summaryKey;
        this.summaryKeyName = summaryKeyName;
        this.count = count;
        this.error = error;
        this.service = service;
        this.errorMessage = errorMessage;
        this.txid = txid;
        this.sql = sql;
        this.apiCall = apiCall;
        this.fullStack = fullStack;

    }

    @Override
    public void merge(ErrorSummaryItem newItem) {
        this.setCount(this.getCount() + newItem.getCount());
        this.setError(newItem.getError());
        this.setService(newItem.getService());
        this.setErrorMessage(newItem.getErrorMessage());
        this.setTxid(newItem.getTxid());
        this.setSql(newItem.getSql());
        this.setApiCall(newItem.getApiCall());
        this.setFullStack(newItem.getFullStack());
    }

    @Override
    public Summary<ErrorSummaryItem> toSummary(List<DateAndMapPack> dnmPackList, int serverId) {
        Summary<ErrorSummaryItem> summary = new Summary<>();

        for (DateAndMapPack dnmPack : dnmPackList) {
            long date = DateTimeHelper.getDefault().yyyymmdd(dnmPack.getYyyymmdd());
            ListValue idList = dnmPack.getMapPack().getList("id");
            ListValue countList = dnmPack.getMapPack().getList("count");
            ListValue errorList = dnmPack.getMapPack().getList("error");
            ListValue serviceList = dnmPack.getMapPack().getList("service");
            ListValue errorMessageList = dnmPack.getMapPack().getList("message");
            ListValue txidList = dnmPack.getMapPack().getList("txid");
            ListValue sqlList = dnmPack.getMapPack().getList("sql");
            ListValue apiCallList = dnmPack.getMapPack().getList("apicall");
            ListValue fullStackList = dnmPack.getMapPack().getList("fullstack");

            for(int i = 0; i < idList.size(); i++) {
                String error = TextProxy.error.getTextIfNullDefault(date, errorList.getInt(i), serverId);
                String service = TextProxy.service.getTextIfNullDefault(date, serviceList.getInt(i), serverId);

                ErrorSummaryItem item = ErrorSummaryItem.builder()
                        .summaryKey(idList.getInt(i))
                        .summaryKeyName(error + " [:of:] " + service)
                        .count(countList.getInt(i))
                        .error(error)
                        .service(service)
                        .errorMessage(TextProxy.error.getTextIfNullDefault(date, errorMessageList.getInt(i), serverId))
                        .txid(txidList.getLong(i))
                        .sql(TextProxy.sql.getTextIfNullDefault(date, sqlList.getInt(i), serverId))
                        .apiCall(TextProxy.apicall.getTextIfNullDefault(date, apiCallList.getInt(i), serverId))
                        .fullStack(TextProxy.error.getTextIfNullDefault(date, fullStackList.getInt(i), serverId)).build();

                summary.merge(item);
            }
        }
        return summary;
    }
}
