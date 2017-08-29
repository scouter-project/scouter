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

import scouter.lang.pack.*;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.client.ParamConstant;
import scouterx.client.net.TcpProxy;
import scouterx.client.server.Server;
import scouterx.webapp.api.model.SObject;
import scouterx.webapp.api.model.SXLog;
import scouterx.webapp.api.viewmodel.RealTimeXLogView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class XLogConsumer {

    /**
     * retrieve object(agent) list from collector server
     */
    public RealTimeXLogView retrieveRealTimeXLog(final Server server, List<Integer> objHashes, int xLogIndex, long xLogLoop) {
        boolean isFirst = false;
        int firstRetrieveLimit = 5000;

        if (xLogIndex == 0 && xLogLoop == 0) {
            isFirst = true;
        }
        String cmd = isFirst ? RequestCmd.TRANX_REAL_TIME_GROUP_LATEST : RequestCmd.TRANX_REAL_TIME_GROUP;

        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.XLOG_INDEX, xLogIndex);
        paramPack.put(ParamConstant.XLOG_LOOP, xLogLoop);
        paramPack.put(ParamConstant.XLOG_LIMIT, firstRetrieveLimit);

        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        for (Integer hash : objHashes) {
            objHashLv.add(hash);
        }

        RealTimeXLogView xLogView = new RealTimeXLogView();
        List<SXLog> xLogList = new ArrayList<>();
        xLogView.setXLogs(xLogList);

        TcpProxy.getTcpProxy(server).process(cmd, paramPack, in -> {
            Pack p = in.readPack();
            if (p.getPackType() == PackEnum.MAP) {
                MapPack metaPack = (MapPack) p;
                xLogView.setXLogIndex(metaPack.getInt(ParamConstant.XLOG_INDEX));
                xLogView.setXLogLoop(metaPack.getInt(ParamConstant.XLOG_LOOP));

            } else {
                XLogPack xLogPack = (XLogPack) p;
                xLogList.add(SXLog.of(xLogPack));
            }
        });

        return xLogView;
}