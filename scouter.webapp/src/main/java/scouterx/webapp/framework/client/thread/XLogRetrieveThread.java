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

package scouterx.webapp.framework.client.thread;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.XLogPack;
import scouter.util.ThreadUtil;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.cache.XLogLoopCache;
import scouterx.webapp.request.RealTimeXLogRequest;
import scouterx.webapp.layer.consumer.XLogConsumer;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
@Slf4j
public class XLogRetrieveThread extends Thread {
    private static AtomicInteger threadNumber = new AtomicInteger();

    private static final long CHECK_INTERVAL = 1000;
    private final Server server;
    private String lastError = "";
    private long lastTime = 0;
    private static final long EXEPTION_IGNORE_TIME = 60_000L;
    private long loop = 0;
    private int index = 0;
    private final XLogConsumer xLogConsumer;
    private XLogLoopCache xLogLoopCache;

    public XLogRetrieveThread(Server server) {
        this.server = server;
        xLogLoopCache = new XLogLoopCache(server, 40000);
        xLogConsumer = new XLogConsumer();
        this.setDaemon(true);
        this.setName("XLogThread-" + server.getName() + "-" + threadNumber.getAndIncrement());
    }

    @Override
    public void run() {
        while (true) {
            ThreadUtil.sleep(CHECK_INTERVAL);
            process();
        }
    }

    private void process() {
        if (server.isOpen() == false || server.getSession() == 0) {
            return;
        }

        try {
            RealTimeXLogRequest realTimeXLogRequest = new RealTimeXLogRequest(loop, index, server.getId(), Collections.emptySet());
            xLogConsumer.handleRealTimeXLog(realTimeXLogRequest, in -> {
                Pack p = in.readPack();
                if (p.getPackType() == PackEnum.MAP) { //meta data arrive ahead of xlog pack
                    MapPack metaPack = (MapPack) p;
                    index = metaPack.getInt(ParamConstant.OFFSET_INDEX);
                    loop = metaPack.getInt(ParamConstant.OFFSET_LOOP);
                } else {
                    xLogLoopCache.add((XLogPack) p);
                }
            });

        } catch (Throwable t) {
            if (t.getMessage() != null && t.getMessage().equals(lastError) && System.currentTimeMillis() < (lastTime + EXEPTION_IGNORE_TIME)) {
                //ignore
            } else {
                lastError = t.getMessage();
                lastTime = System.currentTimeMillis();
                log.error("[XLogThread] at {}, error:{}", server, t.getMessage());
            }
            return;
        }
    }
}
