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

;

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.pack.MapPack
import scouter.lang.value.{BooleanValue, MapValue, TextValue}
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.db.KeyValueStoreRW
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.EnumerScala

class KeyValueStoreService {
    val GLOBAL = "__SCOUTER_KV_GLOBAL__"

    /**
      * get value from global kv store
      * @param din - TextValue:key
      * @param dout - TextValue
      */
    @ServiceHandler(RequestCmd.GET_GLOBAL_KV)
    def getText(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readValue();
        if (param == null)
            return;

        val value = KeyValueStoreRW.get(GLOBAL, param.toString())
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new TextValue(value))
    }

    /**
      * get value from global kv store
      * @param din - MapPack{ ListValue<TextValue>:key }
      * @param dout - MapPack<TextValue,TextValue>
      */
    @ServiceHandler(RequestCmd.GET_GLOBAL_KV_BULK)
    def getTextBulk(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val paramLv = param.getList("key")
        if (paramLv == null || paramLv.size() == 0)
            return

        val mapPack = new MapPack();
        for (i <- 0 until paramLv.size()) {
            val v = KeyValueStoreRW.get(GLOBAL, paramLv.getString(i))
            if(v != null) {
                mapPack.put(paramLv.getString(i), v)
            }
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mapPack)
    }

    /**
      * set value to global kv store
      * @param din - MapPack{ TextValue:key, TextValue:value }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_KV)
    def setText(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        var key = param.getText("key")
        var value = param.getText("value")
        if (key == null)
            return

        val success = KeyValueStoreRW.set(GLOBAL, key, value)
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new BooleanValue(success))
    }

    /**
      * set value to global kv store
      * @param din - MapPack<TextValue,TextValue>
      * @param dout - MapPack<TextValue,TextValue>
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_KV_BULK)
    def setTextBulk(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val paramMapPack = din.readMapPack();
        if(paramMapPack == null || paramMapPack.size() == 0)
            return

        val resultMapPack = new MapPack();
        EnumerScala.foreach(paramMapPack.keys(), (key: String) => {
            val success = KeyValueStoreRW.set(GLOBAL, key, paramMapPack.getText(key))
            resultMapPack.put(key, success);
        })

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(resultMapPack)
    }

    /**
      * get value from specific key space
      * @param din - MapPack{ TextValue:keySpace, TextValue:key }
      * @param dout - TextValue
      */
    @ServiceHandler(RequestCmd.GET_CUSTOM_KV)
    def getTextFromCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        var keySpace = param.getText("keySpace")
        var key = param.getText("key")
        if (key == null)
            return

        val value = KeyValueStoreRW.get(keySpace, key)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writeValue(new TextValue(value))
    }

    /**
      * set value from specific key space
      * @param din - MapPack{ TextValue:keySpace, TextValue:key, TextValue:value }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_CUSTOM_KV)
    def setTextToCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        var keySpace = param.getText("keySpace")
        var key = param.getText("key")
        var value = param.getText("value")
        if (key == null)
            return

        val success = KeyValueStoreRW.set(keySpace, key, value)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writeValue(new BooleanValue(success))
    }

    /**
      * get value from specific key space
      * @param din - MapPack{ TextValue:keySpace, ListValue<TextValue>:key }
      * @param dout - values: MapPack<TextValue,TextValue>
      */
    @ServiceHandler(RequestCmd.GET_CUSTOM_KV_BULK)
    def getTextBulkFromCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val keySpace = param.getText("keySpace")
        val paramLv = param.getList("key")
        if (paramLv == null || paramLv.size() == 0)
            return

        val mapPack = new MapPack()
        for (i <- 0 until paramLv.size()) {
            val v = KeyValueStoreRW.get(keySpace, paramLv.getString(i))
            if(v != null) {
                mapPack.put(paramLv.getString(i), v)
            }
        }
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(mapPack)
    }

    /**
      * set value to specific key space
      * @param din - MapPack{ TextValue:keySpace MapValue<TextValue,TextValue>:kv }
      * @param dout - success: MapPack{key, success}
      */
    @ServiceHandler(RequestCmd.SET_CUSTOM_KV_BULK)
    def setTextBulkToCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        if(param == null || param.size() == 0)
            return

        val keySpace = param.getText("keySpace")
        val kv = param.get("kv").asInstanceOf[MapValue]

        val resultMapPack = new MapPack()
        EnumerScala.foreach(kv.keys(), (key: String) => {
            val success = KeyValueStoreRW.set(keySpace, key, kv.getText(key))
            resultMapPack.put(key, success)
        })

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(resultMapPack)
    }
}
