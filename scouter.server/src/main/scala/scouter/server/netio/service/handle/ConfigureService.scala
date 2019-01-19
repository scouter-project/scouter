/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */

package scouter.server.netio.service.handle

;

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.conf.{ValueType, ValueTypeDesc}
import scouter.lang.counters.CounterEngine
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.value.BlobValue
import scouter.lang.value.BooleanValue
import scouter.lang.value.MapValue
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.CounterManager
import scouter.server.core.AgentManager
import scouter.server.http.handler.RegisterHandler
import scouter.server.netio.AgentCall
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.StringKeyLinkedMap.StringKeyLinkedEntry
import scouter.server.util.EnumerScala

class ConfigureService {

    @ServiceHandler(RequestCmd.GET_CONFIGURE_SERVER)
    def getConfigureServer(din: DataInputX, dout: DataOutputX, login: Boolean) {
        var result = new MapPack();

        result.put("configKey", Configure.getInstance().getKeyValueInfo().getList("key"));

        var config = Configure.getInstance().loadText();
        if (config == null) {
            config = "";
        }
        result.put("serverConfig", config);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(result);
    }

    @ServiceHandler(RequestCmd.SET_CONFIGURE_SERVER)
    def setConfigureServer(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val setConfig = param.getText("setConfig");
        val success = Configure.getInstance().saveText(setConfig);
        if (success) {
            Configure.getInstance().reload(true);
        }
        val result = new MapPack();
        result.put("result", String.valueOf(success));
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(result);
    }

    @ServiceHandler(RequestCmd.GET_CONFIGURE_WAS)
    def getConfigureAgent(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.GET_CONFIGURE_WAS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.SET_CONFIGURE_WAS)
    def setConfigureAgent(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.SET_CONFIGURE_WAS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.LIST_CONFIGURE_WAS)
    def listConfigureWas(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.LIST_CONFIGURE_WAS, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.LIST_CONFIGURE_SERVER)
    def listConfigureServer(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val m = Configure.getInstance().getKeyValueInfo();
        val pack = new MapPack();
        pack.put("key", m.getList("key"));
        pack.put("value", m.getList("value"));
        pack.put("default", m.getList("default"));
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(pack);
    }

    @ServiceHandler(RequestCmd.REDEFINE_CLASSES)
    def applyConfigureAgent(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");

        val o = AgentManager.getAgent(objHash);
        val p = AgentCall.call(o, RequestCmd.REDEFINE_CLASSES, param);
        if (p != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        }
    }

    @ServiceHandler(RequestCmd.GET_XML_COUNTER)
    def getCounterXml(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val defaultXml = CounterManager.getInstance().getXmlContent();
        val customXml = CounterManager.getInstance().getXmlCustomContent();
        val p = new MapPack();
        p.put("default", new BlobValue(defaultXml));
        if (customXml != null && customXml.length > 0) {
            p.put("custom", new BlobValue(customXml));
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
    }

    @ServiceHandler(RequestCmd.DEFINE_OBJECT_TYPE)
    def defineObjectType(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val success = CounterManager.getInstance().addObjectType(param);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new BooleanValue(success));
    }

    @ServiceHandler(RequestCmd.EDIT_OBJECT_TYPE)
    def editObjectType(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val success = CounterManager.getInstance().editObjectType(param);
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new BooleanValue(success));
    }

    @ServiceHandler(RequestCmd.CONFIGURE_DESC)
    def getConfigureDesc(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        if (objHash == 0) {
            val p = new MapPack()
            val descMap = Configure.getInstance.getConfigureDesc();
            EnumerScala.foreach(descMap.entries(), (entry: StringKeyLinkedEntry[String]) => {
                p.put(entry.getKey(), entry.getValue())
            })
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        } else {
            val o = AgentManager.getAgent(objHash);
            val p = AgentCall.call(o, RequestCmd.CONFIGURE_DESC, param);
            if (p != null) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(p);
            }
        }
    }

    @ServiceHandler(RequestCmd.CONFIGURE_VALUE_TYPE)
    def getConfigureValueType(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objHash = param.getInt("objHash");
        if (objHash == 0) {
            val p = new MapPack()
            val valueTypeMap = Configure.getInstance.getConfigureValueType();
            EnumerScala.foreach(valueTypeMap.entries(), (entry: StringKeyLinkedEntry[ValueType]) => {
                p.put(entry.getKey(), entry.getValue().getType())
            })
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(p);
        } else {
            val o = AgentManager.getAgent(objHash);
            val p = AgentCall.call(o, RequestCmd.CONFIGURE_VALUE_TYPE, param);
            if (p != null) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(p);
            }
        }
    }

    @ServiceHandler(RequestCmd.CONFIGURE_VALUE_TYPE_DESC)
    def getConfigureValueTypeDesc(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack]
        val objHash = param.getInt("objHash")
        val p = new MapPack()
        if (objHash == 0) {
            val valueTypeDescMap = Configure.getInstance.getConfigureValueTypeDesc

            EnumerScala.foreach(valueTypeDescMap.entries(), (entry: StringKeyLinkedEntry[ValueTypeDesc]) => {
                p.put(entry.getKey, entry.getValue.toMapValue)
            })
        }
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(p)
    }

    @ServiceHandler(RequestCmd.GET_CONFIGURE_TELEGRAF)
    def getConfigureTelegraf(din: DataInputX, dout: DataOutputX, login: Boolean) {
        var result = new MapPack()
        result.put("tgConfigContents", Configure.getInstance().getTgConfigContents())

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.SET_CONFIGURE_TELEGRAF)
    def setConfigureTelegraf(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val setConfig = param.getText("tgConfigContents")
        val success = Configure.getInstance().saveTgConfigContents(setConfig)
        if (success) {
            Configure.getInstance().reload(true)
        }
        val result = new MapPack()
        result.put("result", String.valueOf(success))
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.GET_CONFIGURE_COUNTERS_SITE)
    def getConfigureCountersSite(din: DataInputX, dout: DataOutputX, login: Boolean) {
        var result = new MapPack()
        result.put("contents", CounterManager.getInstance().readCountersSiteXml())

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }

    @ServiceHandler(RequestCmd.SET_CONFIGURE_COUNTERS_SITE)
    def setConfigureCountersSite(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val contents = param.getText("contents")
        val success = new CounterEngine().parse(contents.getBytes("utf-8")) &&
                CounterManager.getInstance().saveAndReloadCountersSiteXml(contents)

        if (success) RegisterHandler.notifyAllClients()
        val result = new MapPack()
        result.put("result", String.valueOf(success))
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(result)
    }
}
