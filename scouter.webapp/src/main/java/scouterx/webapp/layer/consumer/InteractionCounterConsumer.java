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
import scouter.lang.pack.InteractionPerfCounterPack;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.InteractionCounterData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 24.
 */
public class InteractionCounterConsumer {

    /**
     * get realtime interaction counters's values by type
     * @param objType
     * @param server
     * @return
     */
    public List<InteractionCounterData> retrieveRealTimeByObjType(final String objType, final Server server) {
        return retrieveRealTimeByObjTypeOrObjHashes(objType, null, server);
    }

    /**
     * get realtime interaction counters's values by obj hashes
     * @param objHashSet
     * @param server
     * @return
     */
    public List<InteractionCounterData> retrieveRealTimeByObjHashes(final Set<Integer> objHashSet, final Server server) {
        return retrieveRealTimeByObjTypeOrObjHashes(null, objHashSet, server);
    }

    /**
     * retrieve realtime interaction counter value by objtype of objhash
     *
     */
    private List<InteractionCounterData> retrieveRealTimeByObjTypeOrObjHashes(final String objType, final Set<Integer> objHashSet, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        if (objHashSet != null) {
            for (Integer objHash : objHashSet) {
                objHashLv.add(objHash);
            }
        }

        List<InteractionCounterData> resultList = new ArrayList<>();
        try(TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            tcpProxy.process(RequestCmd.INTR_COUNTER_REAL_TIME_BY_OBJ, paramPack, in -> {
                resultList.add(InteractionCounterData.of((InteractionPerfCounterPack) in.readPack(), server.getId()));
            });
        }

        return resultList;
    }
}
