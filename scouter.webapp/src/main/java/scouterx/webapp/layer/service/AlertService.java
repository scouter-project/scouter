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

import scouterx.webapp.layer.consumer.AlertScriptingConsumer;
import scouterx.webapp.model.alertscript.ScriptingLoadData;
import scouterx.webapp.model.alertscript.ScriptingLogStateData;
import scouterx.webapp.model.alertscript.ScriptingSaveStateData;
import scouterx.webapp.request.RealTimeAlertRequest;
import scouterx.webapp.request.SetConfigRequest;
import scouterx.webapp.view.RealTimeAlertView;
import scouterx.webapp.layer.consumer.AlertConsumer;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AlertService {
    private final AlertConsumer alertConsumer;
    private final AlertScriptingConsumer alertScriptingConsumer;
    public AlertService() {
        this.alertConsumer = new AlertConsumer();
        this.alertScriptingConsumer = new AlertScriptingConsumer();
    }

    public RealTimeAlertView retrieveRealTimeAlert(final RealTimeAlertRequest request) {
        return alertConsumer.retrieveRealTimeAlert(request);
    }


    public ScriptingLoadData loadScripting(int serverId, String counterName) {
        return this.alertScriptingConsumer.loadAlertScripting(serverId,counterName);
    }

    public ScriptingSaveStateData setConfigScripting(int serverId, String counterName, SetConfigRequest setConfigRequest) {
        return this.alertScriptingConsumer.setConfigScripting(serverId,counterName,setConfigRequest);
    }

    public ScriptingSaveStateData setRuleScripting(int serverId, String counterName, SetConfigRequest setConfigRequest) {
        return this.alertScriptingConsumer.setRuleScripting(serverId,counterName,setConfigRequest);

    }

    public ScriptingLogStateData readAlertScripting(int serverId, long loop, long index) {
        return this.alertScriptingConsumer.readAlertScripting(serverId,loop,index);
    }
}
