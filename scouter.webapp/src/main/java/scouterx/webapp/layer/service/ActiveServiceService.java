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

package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.ActiveThread;
import scouterx.webapp.model.ThreadContents;
import scouterx.webapp.model.scouter.SActiveService;
import scouterx.webapp.model.scouter.SActiveServiceStepCount;
import scouterx.webapp.layer.consumer.ActiveServiceConsumer;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class ActiveServiceService {
    private final ActiveServiceConsumer activeServiceConsumer;

    public ActiveServiceService() {
        this.activeServiceConsumer = new ActiveServiceConsumer();
    }

    public List<SActiveServiceStepCount> retrieveRealTimeActiveServiceByObjType(final String objType, final Server server) {
        return activeServiceConsumer.retrieveRealTimeActiveServiceByObjType(objType, server);
    }

    public List<SActiveService> retrieveActiveServiceListByType(final String objType, final Server server) {
        return activeServiceConsumer.retrieveActiveServiceListByType(objType, server);
    }

    public List<SActiveService> retrieveActiveServiceListByObjHash(final int objHash, final Server server) {
        return activeServiceConsumer.retrieveActiveServiceListByObjHash(objHash, server);
    }

    public ActiveThread retrieveActiveThread(final int objHash, final long threadId, final long txid, final Server server) {
        return activeServiceConsumer.retrieveActiveThread(objHash, threadId, txid, server);
    }
    public ThreadContents controlThread(final int objHash, long threadId, String action, final Server server){
        return activeServiceConsumer.controlThread(objHash,threadId,action,server);
    }
}
