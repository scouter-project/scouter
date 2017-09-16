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

package scouterx.webapp.layer.service;

import scouterx.webapp.layer.consumer.SummaryConsumer;
import scouterx.webapp.model.summary.AlertSummaryItem;
import scouterx.webapp.model.summary.ApiCallSummaryItem;
import scouterx.webapp.model.summary.ErrorSummaryItem;
import scouterx.webapp.model.summary.IpSummaryItem;
import scouterx.webapp.model.summary.ServiceSummaryItem;
import scouterx.webapp.model.summary.SqlSummaryItem;
import scouterx.webapp.model.summary.Summary;
import scouterx.webapp.model.summary.UserAgentSummaryItem;
import scouterx.webapp.request.SummaryRequest;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 14.
 */
public class SummaryService {
    private final SummaryConsumer summaryConsumer = new SummaryConsumer();

    public Summary<ServiceSummaryItem> retrieveServiceSummary(SummaryRequest request) {
        return summaryConsumer.retrieveServiceSummary(request);
    }

    public Summary<SqlSummaryItem> retrieveSqlSummary(SummaryRequest request) {
        return summaryConsumer.retrieveSqlSummary(request);
    }

    public Summary<ApiCallSummaryItem> retrieveApiCallSummary(SummaryRequest request) {
        return summaryConsumer.retrieveApiCallSummary(request);
    }

    public Summary<IpSummaryItem> retrieveIpSummary(SummaryRequest request) {
        return summaryConsumer.retrieveIpSummary(request);
    }

    public Summary<UserAgentSummaryItem> retrieveUserAgentSummary(SummaryRequest request) {
        return summaryConsumer.retrieveUserAgentSummary(request);
    }

    public Summary<ErrorSummaryItem> retrieveErrorSummary(SummaryRequest request) {
        return summaryConsumer.retrieveErrorSummary(request);
    }

    public Summary<AlertSummaryItem> retrieveAlertSummary(SummaryRequest request) {
        return summaryConsumer.retrieveAlertSummary(request);
    }
}
