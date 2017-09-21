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
import scouter.lang.pack.AlertLevelEnum;
import scouter.lang.value.ListValue;
import scouter.util.DateTimeHelper;
import scouterx.webapp.framework.dto.DateAndMapPack;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 14.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AlertSummaryItem extends SummaryItem<AlertSummaryItem> {
    private AlertLevelEnum level;

    @Builder
    public AlertSummaryItem(int summaryKey, String summaryKeyName, int count, AlertLevelEnum level) {
        this.summaryKey = summaryKey;
        this.summaryKeyName = summaryKeyName;
        this.count = count;
        this.level = level;
    }

    @Override
    public void merge(AlertSummaryItem newItem) {
        this.setCount(this.getCount() + newItem.getCount());
    }

    @Override
    public Summary<AlertSummaryItem> toSummary(List<DateAndMapPack> dnmPackList, int serverId) {
        Summary<AlertSummaryItem> summary = new Summary<>();

        for (DateAndMapPack dnmPack : dnmPackList) {
            long date = DateTimeHelper.getDefault().yyyymmdd(dnmPack.getYyyymmdd());
            ListValue idList = dnmPack.getMapPack().getList("id");
            ListValue titleList = dnmPack.getMapPack().getList("title");
            ListValue countList = dnmPack.getMapPack().getList("count");
            ListValue levelList = dnmPack.getMapPack().getList("level");

            for(int i = 0; i < idList.size(); i++) {
                AlertSummaryItem item = AlertSummaryItem.builder()
                        .summaryKey(idList.getInt(i))
                        .summaryKeyName(titleList.getString(i))
                        .count(countList.getInt(i))
                        .level(AlertLevelEnum.of(levelList.getInt(i))).build();

                summary.merge(item);
            }
        }
        return summary;
    }
}
