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
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.ActiveThread;
import scouterx.webapp.model.ThreadContents;
import scouterx.webapp.model.scouter.SActiveService;
import scouterx.webapp.model.scouter.SActiveServiceStepCount;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class ActiveServiceConsumer {

    /**
     * get current active service step count by type
     * @param objType
     * @param server
     */
    public List<SActiveServiceStepCount> retrieveRealTimeActiveServiceByObjType(final String objType, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);

        List<Pack> results;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            results = tcpProxy.process(RequestCmd.ACTIVESPEED_REAL_TIME, paramPack);
        }

        return results.stream()
                .map(pack -> (MapPack) pack)
                .map(SActiveServiceStepCount::of)
                .collect(Collectors.toList());
    }

    /**
     * set current active service list by obj type
     *
     * @param objType
     * @param server
     */
    public List<SActiveService> retrieveActiveServiceListByType(final String objType, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        List<Pack> packList = retrieveActiveService(paramPack, server);

        return SActiveService.ofPackList(packList);
    }

    /**
     * set current active service list by obj hashes
     *
     * @param objHash
     * @param server
     */
    public List<SActiveService> retrieveActiveServiceListByObjHash(final int objHash, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_HASH, objHash);
        List<Pack> packList = retrieveActiveService(paramPack, server);

        return SActiveService.ofPackList(packList);
    }

    public List<Pack> retrieveActiveService(final MapPack paramPack, final Server server) {
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            return tcpProxy.process(RequestCmd.OBJECT_ACTIVE_SERVICE_LIST, paramPack);
        }
    }

    public ActiveThread retrieveActiveThread(final int objHash, final long threadId, final long txid, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put("objHash", objHash);
        paramPack.put("id", threadId);
        paramPack.put("txid", txid);

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            MapPack resultPack = (MapPack) tcpProxy.getSingle(RequestCmd.OBJECT_THREAD_DETAIL, paramPack);
            return ActiveThread.of(resultPack);
        }
    }

    public ThreadContents controlThread(int objHash, long threadId, String action, Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put("objHash", objHash);
        paramPack.put("id", threadId);
        paramPack.put("action", action);

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            MapPack resultPack = (MapPack) tcpProxy.getSingle(RequestCmd.OBJECT_THREAD_CONTROL, paramPack);
            return ThreadContents.of(resultPack);

        }
    }
}
