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

import lombok.Data;
import scouterx.webapp.framework.dto.DateAndMapPack;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 14.
 */
@Data
public abstract class SummaryItem<T extends SummaryItem<T>> {
    int summaryKey;
    String summaryKeyName;
    int count;

    /**
     * accumulate item values
     *
     * @param newItem
     */
    abstract void merge(T newItem);

    /**
     * utility method
     * to make summary data from results that was retrieved from scouter collector server.
     *
     * @param dnmPackList
     * @param serverId
     * @return
     */
    abstract Summary<T> toSummary(List<DateAndMapPack> dnmPackList, int serverId);

}
