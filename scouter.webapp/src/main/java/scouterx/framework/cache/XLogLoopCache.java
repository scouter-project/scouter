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

package scouterx.framework.cache;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.pack.XLogPack;
import scouterx.client.server.Server;
import scouterx.model.XLogPackWrapper;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Cache of latest XLog data
 *
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 9.
 */
@Slf4j
public class XLogLoopCache {
    private static ConfigureAdaptor conf = ConfigureManager.getConfigure();
    private final int MAX_RETRIEVE_COUNT;
    private final Server server;
    private long loop = 0;
    private int index = 0;
    private XLogPackWrapper[] queue;

    public XLogLoopCache(Server server, int capacity) {
        this.MAX_RETRIEVE_COUNT = capacity / 2;
        this.server = server;
        if (server == null) {
            throw new RuntimeException("Not yet initialized!");
        }
        queue = new XLogPackWrapper[capacity];
    }

    public void add(XLogPack pack) {
        if (conf.isTrace() && index % 100 == 0) {
            log.info("XLog added - {} {} {}", loop, index, pack);
        }
        synchronized (queue) {
            queue[index] = new XLogPackWrapper(pack, this.loop, this.index);
            index += 1;
            if (index >= queue.length) {
                loop += 1;
                index = 0;
            }
        }
    }

    /**
     * get new xlog data revealed after the previous check point, and invoke the handler consumer
     *
     * @param aLoop
     * @param aIndex
     * @param bufferTime - buffer time(milliseconds) for to prevent fetching data before dictionary data generation.
     */
    public void getAndHandleRealTimeXLog(List<Integer> objHashList, long aLoop, int aIndex, int aMaxCount, int bufferTime,
                                         Consumer<XLogPackWrapper> handlerConsumer) {
        int maxCount = aMaxCount;

        long currentLoop;
        int currentIndex;
        synchronized (queue) {
            currentLoop = this.loop;
            currentIndex = this.index;
        }

        int loopStatus = (int) (currentLoop - aLoop);

        switch(loopStatus) {
            case 0:
                int countToGet = currentIndex - aIndex;
                if (aIndex < currentIndex) {
                    maxCount = Math.min(aMaxCount, countToGet);
                }
                break;
            case 1:
                maxCount = Math.min(aMaxCount, queue.length - aIndex + currentIndex);
                break;
            default:
        }

        if (maxCount == 0 || maxCount > MAX_RETRIEVE_COUNT) {
            maxCount = MAX_RETRIEVE_COUNT;
        }

        if(maxCount > currentIndex) {
            handleInternal(queue.length - (maxCount - currentIndex), queue.length, bufferTime, handlerConsumer);
            handleInternal(0, currentIndex, bufferTime, handlerConsumer);
        } else {
            handleInternal(currentIndex - maxCount, currentIndex, bufferTime, handlerConsumer);
        }
    }

    /**
     * get xlog data and invoke consumer within the range
     *
     * @param from
     * @param to
     * @param bufferTime
     * @param handlerConsumer
     */
    private void handleInternal(int from, int to, int bufferTime, Consumer<XLogPackWrapper> handlerConsumer) {
        long now = server.getCurrentTime();
        for(int i = from; i < from + to; i++) {
            XLogPackWrapper packWrapper = queue[i];

            if (now - bufferTime > packWrapper.getPack().endTime) {
                handlerConsumer.accept(packWrapper);
            } else {
                break;
            }
        }
    }
}
