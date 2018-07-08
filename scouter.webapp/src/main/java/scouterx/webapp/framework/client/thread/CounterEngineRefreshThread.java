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
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.Value;
import scouter.util.ThreadUtil;
import scouterx.webapp.framework.client.net.LoginMgr;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
@Slf4j
public class CounterEngineRefreshThread extends Thread {
    public static final ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private static CounterEngineRefreshThread thread;

    public synchronized static void load() {
        if (thread == null) {
            thread = new CounterEngineRefreshThread();
            thread.setDaemon(true);
            thread.setName("CounterEngineRefreshThread");
            thread.start();
        }
    }

    private static final long CHECK_INTERVAL = 15000;

    @Override
    public void run() {
        ThreadUtil.sleep(CHECK_INTERVAL);
        while (true) {
            try {
                refreshCounterEngine();
            } catch (Throwable t) {
                log.error("[Error][CounterEngineRefreshThread] error:{}", t.getMessage());
            }
            ThreadUtil.sleep(CHECK_INTERVAL);
        }
    }

    private void refreshCounterEngine() {
        Set<Integer> serverIdSet = ServerManager.getInstance().getOpenServerIdList();
        if (serverIdSet.size() > 0) {
            for (int serverId : serverIdSet) {
                Server server = ServerManager.getInstance().getServer(serverId);
                CounterEngine counterEngine = server.getCounterEngine();
                MapPack m = LoginMgr.getCounterXmlServer(server);
                if (m != null) {
                    Value v1 = m.get("default");
                    Value v2 = m.get("custom");

                    counterEngine.clear();
                    counterEngine.parse(((BlobValue)v1).value);
                    if (v2 != null) {
                        counterEngine.parse(((BlobValue)v1).value);
                    }
                }
            }
        }
    }
}
