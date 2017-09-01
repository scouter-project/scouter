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

package scouterx.webapp.api.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouterx.client.net.TcpProxy;
import scouterx.client.server.Server;
import scouterx.webapp.api.model.counter.SCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class CounterConsumer {

    public List<SCounter> retrieveCountersByObjType(String objType, List<String> counterNames, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        ListValue counterNameLv = paramPack.newList(ParamConstant.COUNTER);
        for (String name : counterNames) {
            counterNameLv.add(name);
        }

        MapPack outMapPack = (MapPack)TcpProxy.getTcpProxy(server).getSingle(RequestCmd.COUNTER_REAL_TIME_ALL_MULTI, paramPack);
        ListValue rObjHashLv = (ListValue) outMapPack.get(ParamConstant.OBJ_HASH);
        ListValue rCounterNameLv = (ListValue) outMapPack.get(ParamConstant.COUNTER);
        ListValue rCounterValueLv = (ListValue) outMapPack.get(ParamConstant.VALUE);

        List<SCounter> resultList = new ArrayList<>();
        for(int i = 0; i < rObjHashLv.size(); i++) {
            int objHash = CastUtil.cint(rObjHashLv.get(i));
            String counterName = rCounterNameLv.getString(i);
            Object counterValue = rCounterValueLv.get(i).toJavaObject();

            resultList.add(new SCounter(objHash, counterName, counterValue));
        }

        return resultList;
    }
}
