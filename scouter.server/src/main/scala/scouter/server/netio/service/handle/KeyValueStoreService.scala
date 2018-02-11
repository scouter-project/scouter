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
import scouter.lang.pack.{MapPack}
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.db.{KeyValueStoreRW}
import scouter.server.netio.service.anotation.ServiceHandler

class KeyValueStoreService {
    val GLOBAL = "__SCOUTER_KV_GLOBAL__"

    /**
      * get value from kv store
      * @param din - key: Text
      * @param dout - value: Text
      */
    @ServiceHandler(RequestCmd.GET_GLOBAL_KV)
    def getText(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readText();
        if (param == null)
            return;

        val value = KeyValueStoreRW.get(GLOBAL, param)
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeText(value);
    }

    /**
      * set value from kv store
      * @param din - MapPack{ key: Text, value: Text }
      * @param dout - success: Boolean
      */
    @ServiceHandler(RequestCmd.SET_GLOBAL_KV)
    def setText(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        var key = param.getText("key");
        var value = param.getText("value");
        if (key == null)
            return;

        val result = new MapPack();
        val success = KeyValueStoreRW.set(GLOBAL, key, value)
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writeBoolean(success);
    }

}
