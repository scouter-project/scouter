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

package scouter.server.netio.service.handle

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.pack.MapPack
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.plugin.alert.AlertRuleLoader

/**
  * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 24.
  *
  */
class AlertScriptingService {

    @ServiceHandler(RequestCmd.GET_ALERT_SCRIPTING_CONTETNS)
    def getAlertScriptingContents(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack]
        val contents = AlertRuleLoader.getInstance().getRuleContents(param.getText("counterName"))

        val result = new MapPack()
        result.put("contents", contents)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.GET_ALERT_SCRIPTING_CONFIG_CONTETNS)
    def getAlertScriptingConfigContents(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack]
        val contents = AlertRuleLoader.getInstance().getRuleConfigContents(param.getText("counterName"))

        val result = new MapPack()
        result.put("contents", contents)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.SAVE_ALERT_SCRIPTING_CONTETNS)
    def saveAlertScriptingContents(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack]
        val success = AlertRuleLoader.getInstance().saveRuleContents(param.getText("counterName"), param.getText("contents"))

        val result = new MapPack()
        result.put("success", success)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.SAVE_ALERT_SCRIPTING_CONFIG_CONTETNS)
    def saveAlertScriptingConfigContents(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack]
        val success = AlertRuleLoader.getInstance().saveConfigContents(param.getText("counterName"), param.getText("contents"))

        val result = new MapPack()
        result.put("success", success)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }
}
