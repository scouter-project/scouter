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

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CipherUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.scouter.SUser;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AccountConsumer {

    /**
     * id & password check from scouter collector server
     */
    public boolean login(final Server server, final SUser user) {
        MapPack param = new MapPack();
        param.put(ParamConstant.USER_ID, user.getId());
        param.put(ParamConstant.USER_PASSWROD, CipherUtil.sha256(user.getPassword()));

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.CHECK_LOGIN, param);
        }

        return ((BooleanValue) value).value;
    }
}
