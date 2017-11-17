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

import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.INetReader;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.model.XLogData;
import scouterx.webapp.model.scouter.SXlog;
import scouterx.webapp.request.CondSearchXLogRequest;
import scouterx.webapp.request.PageableXLogRequest;
import scouterx.webapp.request.RealTimeXLogRequest;
import scouterx.webapp.request.SingleXLogRequest;
import scouterx.webapp.view.PageableXLogView;
import scouterx.webapp.view.RealTimeXLogView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Slf4j
public class XLogConsumer {

    /**
     * handle realtime xlog
     */
    public void handleRealTimeXLog(final RealTimeXLogRequest xLogRequest, final INetReader reader) {
        boolean isFirst = false;
        int firstRetrieveLimit = 5000;

        if (xLogRequest.getXLogLoop() == 0 && xLogRequest.getXLogIndex() == 0) {
            isFirst = true;
        }
        String cmd = isFirst ? RequestCmd.TRANX_REAL_TIME_GROUP_LATEST : RequestCmd.TRANX_REAL_TIME_GROUP;

        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OFFSET_INDEX, xLogRequest.getXLogIndex());
        paramPack.put(ParamConstant.OFFSET_LOOP, xLogRequest.getXLogLoop());
        paramPack.put(ParamConstant.XLOG_COUNT, firstRetrieveLimit);

        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        for (Integer hash : xLogRequest.getObjHashes()) {
            objHashLv.add(hash);
        }

        RealTimeXLogView xLogView = new RealTimeXLogView();
        List<SXlog> xLogList = new ArrayList<>();
        xLogView.setXLogs(xLogList);

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(xLogRequest.getServerId())) {
            tcpProxy.process(cmd, paramPack, reader);
        }
    }

    /**
     * retrieve XLog List for paging access
     *
     * @param pageableXLogRequest
     */
    public void handlePageableXLog(final PageableXLogRequest pageableXLogRequest, final INetReader reader) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.DATE, pageableXLogRequest.getYyyymmdd());
        paramPack.put(ParamConstant.XLOG_START_TIME, pageableXLogRequest.getStartTimeMillis());
        paramPack.put(ParamConstant.XLOG_TXID, pageableXLogRequest.getLastTxid());
        paramPack.put(ParamConstant.XLOG_END_TIME, pageableXLogRequest.getEndTimeMillis());
        paramPack.put(ParamConstant.XLOG_LAST_BUCKET_TIME, pageableXLogRequest.getLastXLogTime());
        paramPack.put(ParamConstant.XLOG_PAGE_COUNT, pageableXLogRequest.getPageCount());

        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        for (Integer hash : pageableXLogRequest.getObjHashes()) {
            objHashLv.add(hash);
        }

        PageableXLogView view = new PageableXLogView();
        List<SXlog> xLogList = new ArrayList<>();
        view.setXLogs(xLogList);

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(pageableXLogRequest.getServerId())) {
            tcpProxy.process(RequestCmd.TRANX_LOAD_TIME_GROUP_V2, paramPack, reader);
        }
    }
    
    /**
     * retrieve XLog List for searching with various condition
     * @param CondSearchXLogRequest
     */   
    public List<XLogData> retrieveConditionSearchXLog(final CondSearchXLogRequest condXLogRequest) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.DATE,condXLogRequest.getYyyymmdd());
        paramPack.put(ParamConstant.XLOG_START_TIME, condXLogRequest.getStartTimeMillis());
        paramPack.put(ParamConstant.XLOG_END_TIME, condXLogRequest.getEndTimeMillis());
        
        String service = condXLogRequest.getService();
        if ( service != null ){
            paramPack.put(ParamConstant.XLOG_SERVICE, service);
        }
        
        long objHash = condXLogRequest.getObjHash();
        if ( objHash != 0 ){
        	paramPack.put(ParamConstant.OBJ_HASH, objHash);
        }
        
        String ipAddr = condXLogRequest.getIp();
        if ( ipAddr != null ){
        	paramPack.put(ParamConstant.XLOG_IP, ipAddr);
        }
        
        String login = condXLogRequest.getLogin();
        if (login != null){
            paramPack.put(ParamConstant.XLOG_LOGIN, login);
        }
        
        String desc = condXLogRequest.getDesc();
        if ( desc != null ){
        	 paramPack.put(ParamConstant.XLOG_DESC, desc);
        }
       
        String textTemp = condXLogRequest.getText1();
        if ( textTemp != null){
        	paramPack.put(ParamConstant.XLOG_TEXT_1, textTemp);
        }
        textTemp = condXLogRequest.getText2();
        if ( textTemp != null){
        	paramPack.put(ParamConstant.XLOG_TEXT_2, textTemp);
        }
        textTemp = condXLogRequest.getText3();
        if ( textTemp != null){
        	paramPack.put(ParamConstant.XLOG_TEXT_3, textTemp);
        }        
        textTemp = condXLogRequest.getText4();
        if ( textTemp != null){
        	paramPack.put(ParamConstant.XLOG_TEXT_4, textTemp);
        }
        textTemp = condXLogRequest.getText5();
        if ( textTemp != null){
        	paramPack.put(ParamConstant.XLOG_TEXT_5, textTemp);
        }
        
        List<XLogPack> resp = null;
        
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(condXLogRequest.getServerId())) {
        	resp = (List)tcpProxy.process(RequestCmd.SEARCH_XLOG_LIST, paramPack);
        }
        
        List<XLogData> result = new ArrayList<XLogData>();
        for(XLogPack pack : resp){
        	result.add(XLogData.of(pack,condXLogRequest.getServerId()));
        }
        
        return result;
        
    }
    
    /**
     * retrieve XLog
     *
     * @param singleXLogRequest
     */
    public XLogData retrieveByTxidAsXLogData(final SingleXLogRequest singleXLogRequest) {
        XLogPack pack = retrieveByTxid(singleXLogRequest);
        return pack == null ? null : XLogData.of(pack, singleXLogRequest.getServerId());
    }

    /**
     * retrieve XLog
     *
     * @param singleXLogRequest
     */
    public SXlog retrieveByTxidAsXLog(final SingleXLogRequest singleXLogRequest) {
        XLogPack pack = retrieveByTxid(singleXLogRequest);
        return pack == null ? null : SXlog.of(pack);
    }

    /**
     * retrieve XLog
     *
     * @param singleXLogRequest
     */
    private XLogPack retrieveByTxid(final SingleXLogRequest singleXLogRequest) {

        MapPack param = new MapPack();
        param.put(ParamConstant.DATE, singleXLogRequest.getYyyymmdd());
        param.put(ParamConstant.XLOG_TXID, singleXLogRequest.getTxid());

        XLogPack pack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(singleXLogRequest.getServerId())) {
            pack = (XLogPack) tcpProxy.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
        }

        return pack;
    }

}
