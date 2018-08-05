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
import scouter.util.ThreadUtil;
import scouterx.webapp.framework.client.net.LoginMgr;
import scouterx.webapp.framework.client.net.LoginRequest;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 26.
 */
@Slf4j
public class ServerSessionObserver extends Thread {
    public static final ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private static ServerSessionObserver observer;

    public synchronized static void load() {
        if (observer == null) {
            observer = new ServerSessionObserver();
            observer.setDaemon(true);
            observer.setName("ServerSessionObserverThread");
            observer.start();
        }
    }

    private static final long CHECK_INTERVAL = 5000;

    @Override
    public void run() {
        ThreadUtil.sleep(CHECK_INTERVAL);
        while (true) {
            process();
            ThreadUtil.sleep(CHECK_INTERVAL);
        }
    }

    private void process() {
        try {
            Set<Integer> idSet = ServerManager.getInstance().getOpenServerIdList();
            for (int serverId : idSet) {
                Server server = ServerManager.getInstance().getServer(serverId);
                if (server == null) {
                    continue;
                }
                if (server.getSession() == 0) {
                    LoginRequest result = LoginMgr.login(server);
                    if (result.success) {
                        log.info("Success re-login to {}", server.getName());
                    } else {
                        log.error("Failed re-login to {} : {}", server.getName(), result.getErrorMessage());
                    }
                }
            }

            Set<Integer> closedSet = ServerManager.getInstance().getClosedServerIdList();
            for (int serverId : closedSet) {
                Server server = ServerManager.getInstance().getServer(serverId);
                if (server == null) {
                    continue;
                }
                LoginRequest result = LoginMgr.login(server);
                if (result.success) {
                    log.info("Success re-login to {}", server.getName());
                } else {
                    log.error("Failed re-login to {} : {}", server.getName(), result.getErrorMessage());
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
