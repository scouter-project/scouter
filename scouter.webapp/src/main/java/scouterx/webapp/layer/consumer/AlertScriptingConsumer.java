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

import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.HashUtil;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.model.alertscript.ScriptingLoadData;
import scouterx.webapp.model.alertscript.ScriptingLogStateData;
import scouterx.webapp.model.alertscript.ScriptingSaveStateData;
import scouterx.webapp.model.alertscript.ApiDesc;
import scouterx.webapp.request.SetConfigRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Yosong.heo (yosong.heo@gmail.com) on 2023. 2. 19.
 */
public class AlertScriptingConsumer {


    private final int STATUS_DUP_CODE = 301;
    private final int STATUS_SAVE_NORMAL_CODE = 200;
    private final int STATUS_SAVE_FAIL_CODE = 404;
    private final String DEFAULT_RULE_CONTENTS =
            "// void process(RealCounter $counter, PluginHelper $$) {\n" +
                    "// create your java code below..\n" +
                    "\n" +
                    "\n" +
                    "// }";
    private final String DEFAULT_CONFIG_CONTENTS =
            "#history_size=150\n" +
                    "#silent_time=300\n" +
                    "#check_term=20";



    public ScriptingLogStateData readAlertScripting(int serverId, long loop, long index) {
        ScriptingLogStateData alertScriptingReadStateData = new ScriptingLogStateData();
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId) ){
            MapPack param = new MapPack();
            param.put("loop", loop);
            param.put("index", index);

            MapPack resultMapPack = (MapPack)tcpProxy.getSingle(RequestCmd.GET_ALERT_SCRIPT_LOAD_MESSAGE, param);

            long consoleLoop = resultMapPack.getLong("loop");
            int consoleIndex = resultMapPack.getInt("index");
            alertScriptingReadStateData.setLoop(consoleLoop);
            alertScriptingReadStateData.setIndex(consoleIndex);
            ListValue messageLv = resultMapPack.getList("messages");
            for (String message : messageLv.toStringArray()) {
                alertScriptingReadStateData.getMessage().add(message);
            }
        }
        return alertScriptingReadStateData;
    }
    public ScriptingLoadData loadAlertScripting(int serverId, String counterName) {
        ScriptingLoadData alertScriptingLoadData= new ScriptingLoadData();

        MapPack param = new MapPack();
        param.put("counterName", counterName);
        loadAlertScriptingContents(alertScriptingLoadData,serverId,param);
        loadAlertScriptingConfigContents(alertScriptingLoadData,serverId,param);
        loadApiDesc(alertScriptingLoadData.getRealCounterDescMap(),serverId,RequestCmd.GET_ALERT_REAL_COUNTER_DESC, "$counter");
        loadApiDesc(alertScriptingLoadData.getPluginHelperDescMap(),serverId,RequestCmd.GET_PLUGIN_HELPER_DESC, "$$");

        return alertScriptingLoadData;
    }

    private void loadApiDesc(Map<String,ApiDesc> apiDescMap, int serverId, String requestCmd, String label) {

        try(TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId)) {
            tcpProxy.process(requestCmd, new MapPack(), in -> {
                MapPack mapPack = (MapPack) in.readPack();

                String desc = mapPack.getText("desc");
                String methodName = mapPack.getText("methodName");
                String returnTypeName = mapPack.getText("returnTypeName");

                ApiDesc apiDesc = new ApiDesc();
                apiDesc.setDesc(desc);
                apiDesc.setMethodName(methodName);
                apiDesc.setReturnTypeName(returnTypeName);

                ListValue parameterTypeNames = mapPack.getList("parameterTypeNames");
                String paramSig = Arrays.stream(parameterTypeNames.toStringArray()).collect(Collectors.joining(", "));

                apiDesc.setFullSignature(new StringBuilder()
                        .append(label)
                        .append(".")
                        .append(methodName)
                        .append("(")
                        .append(paramSig)
                        .append(")").toString());

                apiDescMap.put(apiDesc.getFullSignature(), apiDesc);
            });
        }
    }

    private void loadAlertScriptingConfigContents(ScriptingLoadData alertScriptingLoadData, int serverId, MapPack param) {
        try(TcpProxy tcpProxy=TcpProxy.getTcpProxy(serverId)) {
            MapPack resultMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.GET_ALERT_SCRIPTING_CONFIG_CONTETNS, param);
            alertScriptingLoadData.setConfigText(StringUtil.emptyToDefault(resultMapPack.getText("contents"), DEFAULT_CONFIG_CONTENTS));
        }
    }

    private void loadAlertScriptingContents(ScriptingLoadData alertScriptingLoadData, int serverId, MapPack param) {
        try(TcpProxy tcpProxy=TcpProxy.getTcpProxy(serverId)) {
            MapPack resultMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.GET_ALERT_SCRIPTING_CONTETNS, param);
            alertScriptingLoadData.setRuleText(StringUtil.emptyToDefault(resultMapPack.getText("contents"), DEFAULT_RULE_CONTENTS));
        }

    }

    public ScriptingSaveStateData setConfigScripting(int serverId, String counterName, SetConfigRequest setConfigRequest) {
        ScriptingLoadData alertScriptingLoadData= new ScriptingLoadData();
        ScriptingSaveStateData saveState = new ScriptingSaveStateData();

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId) ){
            MapPack param = new MapPack();
            param.put("counterName", counterName);
            this.loadAlertScriptingConfigContents(alertScriptingLoadData,serverId,param);

            int setConfigHash= HashUtil.hash(setConfigRequest.getValues());

            if(alertScriptingLoadData.getConfigHash() != setConfigHash){
                param.put("contents", setConfigRequest.getValues());
                MapPack resultMapPack = (MapPack)tcpProxy.getSingle(RequestCmd.SAVE_ALERT_SCRIPTING_CONFIG_CONTETNS, param);
                if(Objects.nonNull(resultMapPack)){
                    if(resultMapPack.getBoolean("success")){
                        saveState.setStatus(STATUS_SAVE_NORMAL_CODE);
                        saveState.setMessage("Settings save success");
                    }else{
                        saveState.setStatus(STATUS_SAVE_FAIL_CODE);
                        saveState.setMessage("Failed to save settings");
                    }
                }else{
                    saveState.setStatus(STATUS_SAVE_FAIL_CODE);
                    saveState.setMessage("Failed to save settings");

                }
            }
        }
        return saveState;
    }

    public ScriptingSaveStateData setRuleScripting(int serverId, String counterName, SetConfigRequest setConfigRequest) {
        ScriptingLoadData alertScriptingLoadData= new ScriptingLoadData();
        ScriptingSaveStateData saveState = new ScriptingSaveStateData();
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId) ){
            MapPack param = new MapPack();
            param.put("counterName", counterName);
            this.loadAlertScriptingContents(alertScriptingLoadData,serverId,param);
            int setConfigHash= HashUtil.hash(setConfigRequest.getValues());

            if(alertScriptingLoadData.getRuleTextHash() != setConfigHash){
                param.put("contents", setConfigRequest.getValues());
                MapPack resultMapPack = (MapPack)tcpProxy.getSingle(RequestCmd.SAVE_ALERT_SCRIPTING_CONTETNS, param);
                if(Objects.nonNull(resultMapPack)){
                    if(resultMapPack.getBoolean("success")){
                        saveState.setStatus(STATUS_SAVE_NORMAL_CODE);
                        saveState.setMessage("Settings save success");
                    }else{
                        saveState.setStatus(STATUS_SAVE_FAIL_CODE);
                        saveState.setMessage("Failed to save settings");
                    }
                }else{
                    saveState.setStatus(STATUS_SAVE_FAIL_CODE);
                    saveState.setMessage("Failed to save settings");

                }
            }
        }
        return saveState;
    }


}
