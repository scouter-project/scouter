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

package scouterx.webapp.api.service;

import scouterx.client.net.INetReader;
import scouterx.webapp.api.consumer.XLogConsumer;
import scouterx.webapp.api.requestmodel.PageableXLogRequest;
import scouterx.webapp.api.requestmodel.RealTimeXLogRequest;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class XLogService {
    private final XLogConsumer xLogConsumer;

    public XLogService() {
        this.xLogConsumer = new XLogConsumer();
    }

    /**
     * retrieve realtime xlog
     */
    public void handleRealTimeXLog(final RealTimeXLogRequest xLogRequest, final INetReader reader) {
        xLogConsumer.handleRealTimeXLog(xLogRequest, reader);
    }

    /**
     * retrieve XLog List for paging access
     */
    public void handlePageableXLog(PageableXLogRequest xLogRequest, INetReader reader) {

        xLogConsumer.handlePageableXLog(xLogRequest, reader);
    }

}
