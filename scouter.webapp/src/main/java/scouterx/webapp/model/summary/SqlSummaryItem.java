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
public class SqlSummaryItem extends SummaryItem<SqlSummaryItem> {
    private int errorCount;
    private long elapsedSum;

    @Builder
    public SqlSummaryItem(int summaryKey, String summaryKeyName, int count, int errorCount, long elapsedSum) {
        this.summaryKey = summaryKey;
        this.summaryKeyName = summaryKeyName;
        this.count = count;
        this.errorCount = errorCount;
        this.elapsedSum = elapsedSum;
    }

    @Override
    public void merge(SqlSummaryItem newItem) {
        this.setCount(this.getCount() + newItem.getCount());
        this.setErrorCount(this.getErrorCount() + newItem.getErrorCount());
        this.setElapsedSum(this.getElapsedSum() + newItem.getElapsedSum());
    }

    @Override
    public Summary<SqlSummaryItem> toSummary(List<DateAndMapPack> dnmPackList, int serverId) {
        Summary<SqlSummaryItem> summary = new Summary<>();

        for (DateAndMapPack dnmPack : dnmPackList) {
            long date = DateTimeHelper.getDefault().yyyymmdd(dnmPack.getYyyymmdd());
            ListValue idList = dnmPack.getMapPack().getList("id");
            ListValue countList = dnmPack.getMapPack().getList("count");
            ListValue errorCntList = dnmPack.getMapPack().getList("error");
            ListValue elapsedSumList = dnmPack.getMapPack().getList("elapsed");

            for(int i = 0; i < idList.size(); i++) {
                SqlSummaryItem item = SqlSummaryItem.builder()
                        .summaryKey(idList.getInt(i))
                        .summaryKeyName(TextProxy.sql.getTextIfNullDefault(date, idList.getInt(i), serverId))
                        .count(countList.getInt(i))
                        .errorCount(errorCntList.getInt(i))
                        .elapsedSum(elapsedSumList.getLong(i)).build();

                summary.merge(item);
            }
        }
        return summary;
    }
}
