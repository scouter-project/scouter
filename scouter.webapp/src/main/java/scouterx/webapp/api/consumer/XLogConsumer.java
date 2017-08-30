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

import lombok.extern.slf4j.Slf4j;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.client.ParamConstant;
import scouterx.client.net.TcpProxy;
import scouterx.client.server.Server;
import scouterx.webapp.api.file.UnSynchronizedPackFileWriter;
import scouterx.webapp.api.model.SXLog;
import scouterx.webapp.api.requestmodel.XLogTokenRequest;
import scouterx.webapp.api.viewmodel.RealTimeXLogView;
import scouterx.webapp.configure.ConfigureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Slf4j
public class XLogConsumer {
    private static ExecutorService es = new ThreadPoolExecutor(3, 3, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
        private int threadNum = 1;
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "XLogConsumerThread" + (threadNum++));
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * retrieve realtime xlog
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
        paramPack.put(ParamConstant.XLOG_MAX_COUN, firstRetrieveLimit);

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

    /**
     * retrieve xlog by time
     * TODO XLog Temp file delete job
     * @param xLogRequest
     * @return generated request id for retrieveXLog
     */
    public String requestXLogToken(final XLogTokenRequest xLogRequest) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.DATE, xLogRequest.getDate());
        paramPack.put(ParamConstant.XLOG_START_TIME, xLogRequest.getStartTime());
        paramPack.put(ParamConstant.XLOG_END_TIME, xLogRequest.getEndTime());
        paramPack.put(ParamConstant.XLOG_MAX_COUNT, xLogRequest.getMaxCount());

        String requestToken = UUID.randomUUID().toString();

        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        for (Integer hash : xLogRequest.getObjHashes()) {
            objHashLv.add(hash);
        }

        RealTimeXLogView xLogView = new RealTimeXLogView();
        List<SXLog> xLogList = new ArrayList<>();
        xLogView.setXLogs(xLogList);

        es.execute(() -> {
            String tempFileName = ConfigureManager.getConfigure().getLogDir() + File.separator + requestToken;
            try (UnSynchronizedPackFileWriter ufile = new UnSynchronizedPackFileWriter(tempFileName)) {
                TcpProxy.getTcpProxy(xLogRequest.getServerId()).process(RequestCmd.TRANX_LOAD_TIME_GROUP, paramPack, in -> {
                    Pack p = in.readPack();
                    ufile.writePack(in.readPack());
                });

            } catch (Exception e) {
                log.error("exception while retrieve xlog.", e);
                try {
                    Files.delete(Paths.get(tempFileName));
                } catch (IOException e1) {}
            }
        });

        return requestToken;
    }
}
