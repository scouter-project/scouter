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

package scouterx.webapp.service;

import scouterx.webapp.api.request.RealTimeAlertRequest;
import scouterx.webapp.api.view.RealTimeAlertView;
import scouterx.webapp.consumer.AlertConsumer;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AlertService {
    private final AlertConsumer alertConsumer;

    public AlertService() {
        this.alertConsumer = new AlertConsumer();
    }

    public RealTimeAlertView retrieveRealTimeAlert(final RealTimeAlertRequest request) {
        return alertConsumer.retrieveRealTimeAlert(request);
    }

}
