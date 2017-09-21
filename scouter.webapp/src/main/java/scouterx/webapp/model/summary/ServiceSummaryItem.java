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
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ServiceSummaryItem extends SummaryItem<ServiceSummaryItem> {
    private int errorCount;
    private long elapsedSum;
    private long cpuSum;
    private long memorySum;

    @Builder
    public ServiceSummaryItem(int summaryKey, String summaryKeyName, int count, int errorCount, long elapsedSum, long cpuSum, long memorySum) {
        this.summaryKey = summaryKey;
        this.summaryKeyName = summaryKeyName;
        this.count = count;
        this.errorCount = errorCount;
        this.elapsedSum = elapsedSum;
        this.cpuSum = cpuSum;
        this.memorySum = memorySum;
    }

    @Override
    public void merge(ServiceSummaryItem newItem) {
        this.setCount(this.getCount() + newItem.getCount());
        this.setErrorCount(this.getErrorCount() + newItem.getErrorCount());
        this.setElapsedSum(this.getElapsedSum() + newItem.getElapsedSum());
        this.setCpuSum(this.getCpuSum() + newItem.getCpuSum());
        this.setMemorySum(this.getMemorySum() + newItem.getMemorySum());
    }

    @Override
    public Summary<ServiceSummaryItem> toSummary(List<DateAndMapPack> dnmPackList, int serverId) {
        Summary<ServiceSummaryItem> summary = new Summary<>();

        for (DateAndMapPack dnmPack : dnmPackList) {
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
                        .summaryKeyName(TextProxy.service.getTextIfNullDefault(date, idList.getInt(i), serverId))
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
}
