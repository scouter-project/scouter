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
import scouter.lang.constants.ParamConstant
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
        val paramLv = param.getList(ParamConstant.KEY)
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
      * @param din - MapPack{ TextValue:key, TextValue:value, LongValue:ttl }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_KV)
    def setText(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val key = param.getText(ParamConstant.KEY)
        val value = param.getText(ParamConstant.VALUE)
        val ttl = param.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)
        if (key == null)
            return

        val success = KeyValueStoreRW.set(GLOBAL, key, value, ttl)
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new BooleanValue(success))
    }

    /**
      * set ttl to global kv store
      * @param din - MapPack{ TextValue:key, LongValue:ttl }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_TTL)
    def setTTL(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val key = param.getText(ParamConstant.KEY)
        val ttl = param.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)
        if (key == null)
            return

        val success = KeyValueStoreRW.setTTL(GLOBAL, key, ttl)
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeValue(new BooleanValue(success))
    }

    /**
      * set value to global kv store
      * @param din - MapPack{ LongValue:ttl, MapValue<TextValue:key,TextValue:value>:kv }
      * @param dout - MapPack<TextValue,TextValue>
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_KV_BULK)
    def setTextBulk(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val paramMapPack0 = din.readMapPack();
        val ttl = paramMapPack0.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)
        val paramMapPack = paramMapPack0.get(ParamConstant.KEY_VALUE).asInstanceOf[MapValue]
        if(paramMapPack == null || paramMapPack.size() == 0)
            return

        val resultMapPack = new MapPack();
        EnumerScala.foreach(paramMapPack.keys(), (key: String) => {
            val success = KeyValueStoreRW.set(GLOBAL, key, paramMapPack.getText(key), ttl)
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
        var keySpace = param.getText(ParamConstant.KEY_SPACE)
        var key = param.getText(ParamConstant.KEY)
        if (key == null)
            return

        val value = KeyValueStoreRW.get(keySpace, key)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writeValue(new TextValue(value))
    }

    /**
      * set value to specific key space
      * @param din - MapPack{ TextValue:keySpace, TextValue:key, TextValue:value }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_CUSTOM_KV)
    def setTextToCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        var keySpace = param.getText(ParamConstant.KEY_SPACE)
        var key = param.getText(ParamConstant.KEY)
        var value = param.getText(ParamConstant.VALUE)
        val ttl = param.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)
        if (key == null)
            return

        val success = KeyValueStoreRW.set(keySpace, key, value, ttl)
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writeValue(new BooleanValue(success))
    }

    /**
      * set ttl to specific key space
      * @param din - MapPack{ TextValue:keySpace, TextValue:key, TextValue:value }
      * @param dout - BooleanValue
      */
    @ServiceHandler(RequestCmd.SET_CUSTOM_TTL)
    def setTtlToCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        var keySpace = param.getText(ParamConstant.KEY_SPACE)
        var key = param.getText(ParamConstant.KEY)
        val ttl = param.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)
        if (key == null)
            return

        val success = KeyValueStoreRW.setTTL(keySpace, key, ttl)
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
        val keySpace = param.getText(ParamConstant.KEY_SPACE)
        val paramLv = param.getList(ParamConstant.KEY)
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
      * @param din - MapPack{ TextValue:keyspace, LongValue:ttl, MapValue<TextValue:key,TextValue:value>:kv }
      * @param dout - success: MapPack{key, success}
      */
    @ServiceHandler(RequestCmd.SET_CUSTOM_KV_BULK)
    def setTextBulkToCustomStore(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        if(param == null || param.size() == 0)
            return

        val keySpace = param.getText(ParamConstant.KEY_SPACE)
        val kv = param.get(ParamConstant.KEY_VALUE).asInstanceOf[MapValue]
        val ttl = param.getLongDefault(ParamConstant.TTL, ParamConstant.TTL_PERMANENT)

        val resultMapPack = new MapPack()
        EnumerScala.foreach(kv.keys(), (key: String) => {
            val success = KeyValueStoreRW.set(keySpace, key, kv.getText(key), ttl)
            resultMapPack.put(key, success)
        })

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(resultMapPack)
    }
}
