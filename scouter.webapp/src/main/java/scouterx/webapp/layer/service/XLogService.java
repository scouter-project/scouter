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

import java.util.List;

import scouterx.webapp.framework.client.net.INetReader;
import scouterx.webapp.layer.consumer.XLogConsumer;
import scouterx.webapp.model.XLogData;
import scouterx.webapp.model.scouter.SXlog;
import scouterx.webapp.request.CondSearchXLogRequest;
import scouterx.webapp.request.PageableXLogRequest;
import scouterx.webapp.request.RealTimeXLogRequest;
import scouterx.webapp.request.SingleXLogRequest;

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
    public void handlePageableXLog(final PageableXLogRequest xLogRequest, final INetReader reader) {

        xLogConsumer.handlePageableXLog(xLogRequest, reader);
    }

    
    
    /**
     * retrieve variable condition search
     */
    public List<XLogData>  handleCondtionSearchXLog(final CondSearchXLogRequest condSearchXlogRequest) {
        return xLogConsumer.handleConditionSearchXLog(condSearchXlogRequest);
    }
    
    /**
     * retrieve variable condition search
     */
    /*
    public void handleCondtionSearchXLog(final CondSearchXLogRequest condSearchXlogRequest, final INetReader reader) {
        xLogConsumer.handleConditionSearchXLog(condSearchXlogRequest,reader);
    }
    */
    
    /**
     * retrieve single xLog
     */
    public XLogData retrieveSingleXLogAsXLogData(final SingleXLogRequest singleXlogRequest) {
        return xLogConsumer.retrieveByTxidAsXLogData(singleXlogRequest);

    }

    /**
     * retrieve single xLog
     */
    public SXlog retrieveSingleXLogAsXLog(final SingleXLogRequest singleXlogRequest) {
        return xLogConsumer.retrieveByTxidAsXLog(singleXlogRequest);

    }
    


}
