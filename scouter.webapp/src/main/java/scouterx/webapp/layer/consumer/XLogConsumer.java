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
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.INetReader;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.model.XLogData;
import scouterx.webapp.model.scouter.SXLog;
import scouterx.webapp.request.GxidXLogRequest;
import scouterx.webapp.request.MultiXLogRequest;
import scouterx.webapp.request.PageableXLogRequest;
import scouterx.webapp.request.RealTimeXLogRequest;
import scouterx.webapp.request.SearchXLogRequest;
import scouterx.webapp.request.SingleXLogRequest;
import scouterx.webapp.view.PageableXLogView;
import scouterx.webapp.view.RealTimeXLogView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static scouter.lang.constants.ParamConstant.XLOG_TXID;

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
        int firstRetrieveLimit = 10000;

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
        List<SXLog> xLogList = new ArrayList<>();
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
        paramPack.put(XLOG_TXID, pageableXLogRequest.getLastTxid());
        paramPack.put(ParamConstant.XLOG_END_TIME, pageableXLogRequest.getEndTimeMillis());
        paramPack.put(ParamConstant.XLOG_LAST_BUCKET_TIME, pageableXLogRequest.getLastXLogTime());
        paramPack.put(ParamConstant.XLOG_PAGE_COUNT, pageableXLogRequest.getPageCount());

        ListValue objHashLv = paramPack.newList(ParamConstant.OBJ_HASH);
        for (Integer hash : pageableXLogRequest.getObjHashes()) {
            objHashLv.add(hash);
        }

        PageableXLogView view = new PageableXLogView();
        List<SXLog> xLogList = new ArrayList<>();
        view.setXLogs(xLogList);

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(pageableXLogRequest.getServerId())) {
            tcpProxy.process(RequestCmd.TRANX_LOAD_TIME_GROUP_V2, paramPack, reader);
        }
    }

    /**
     * retrieve XLog List for searching with various condition
     *
     */
    public List<SXLog> searchXLogList(final SearchXLogRequest searchXLogRequest) {
        List<Pack> searchXLogPackList = searchXLogPackList(searchXLogRequest);

        List<SXLog> result = new ArrayList<>();
        for(Pack pack : searchXLogPackList){
            result.add(SXLog.of((XLogPack) pack));
        }

        return result;
    }


    /**
     * retrieve XLog List for searching with various condition
     *
     */
    public List<XLogData> searchXLogDataList(final SearchXLogRequest searchXLogRequest) {
        List<Pack> searchXLogPackList = searchXLogPackList(searchXLogRequest);

        List<XLogData> result = new ArrayList<>();
        for(Pack pack : searchXLogPackList){
            result.add(XLogData.of((XLogPack) pack, searchXLogRequest.getServerId()));
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
    public SXLog retrieveByTxidAsXLog(final SingleXLogRequest singleXLogRequest) {
        XLogPack pack = retrieveByTxid(singleXLogRequest);
        return pack == null ? null : SXLog.of(pack);
    }

    /**
     * retrieve XLogList by gxid
     *
     * @param xlogRequest
     */
    public List<SXLog> retrieveXLogListByGxid(final GxidXLogRequest xlogRequest) {
        return retrieveXLogPacksByGxid(xlogRequest).stream()
                .map(pack -> (XLogPack) pack)
                .map(SXLog::of)
                .collect(Collectors.toList());
    }


    /**
     * retrieve XLog Data List by gxid
     *
     * @param xLogRequest
     */
    public List<XLogData> retrieveXLogDataListByGxid(final GxidXLogRequest xLogRequest) {
        return retrieveXLogPacksByGxid(xLogRequest).stream()
                .map(pack -> (XLogPack) pack)
                .map(pack -> XLogData.of(pack, xLogRequest.getServerId()))
                .collect(Collectors.toList());
    }

    /**
     * retrieve XLog Data List by txids
     *
     * @param multiXLogRequest
     */
    public List<XLogData> retrieveXLogDataListByTxids(final MultiXLogRequest multiXLogRequest) {
        return retrieveXLogPacksByTxids(multiXLogRequest).stream()
                .map(pack -> (XLogPack) pack)
                .map(pack -> XLogData.of(pack, multiXLogRequest.getServerId()))
                .collect(Collectors.toList());
    }


    private XLogPack retrieveByTxid(final SingleXLogRequest singleXLogRequest) {
        MapPack param = new MapPack();
        param.put(ParamConstant.DATE, singleXLogRequest.getYyyymmdd());
        param.put(XLOG_TXID, singleXLogRequest.getTxid());

        XLogPack pack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(singleXLogRequest.getServerId())) {
            pack = (XLogPack) tcpProxy.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
        }

        return pack;
    }

    private List<Pack> retrieveXLogPacksByGxid(GxidXLogRequest xlogRequest) {
        MapPack param = new MapPack();
        param.put(ParamConstant.DATE, xlogRequest.getYyyymmdd());
        param.put(ParamConstant.XLOG_GXID, xlogRequest.getGxid());

        List<Pack> results;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(xlogRequest.getServerId())) {
            results = tcpProxy.process(RequestCmd.XLOG_READ_BY_GXID, param);
        }
        return results;
    }

    private List<Pack> retrieveXLogPacksByTxids(MultiXLogRequest xlogRequest) {
        MapPack param = new MapPack();
        param.put(ParamConstant.DATE, xlogRequest.getYyyymmdd());
        ListValue xlogLv = param.newList(XLOG_TXID);
        xlogRequest.getTxidList().forEach(xlogLv::add);

        List<Pack> results;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(xlogRequest.getServerId())) {
            results = tcpProxy.process(RequestCmd.XLOG_LOAD_BY_TXIDS, param);
        }
        return results;
    }

    private List<Pack> searchXLogPackList(final SearchXLogRequest searchXLogRequest) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.DATE,searchXLogRequest.getYyyymmdd());
        paramPack.put(ParamConstant.XLOG_START_TIME, searchXLogRequest.getStartTimeMillis());
        paramPack.put(ParamConstant.XLOG_END_TIME, searchXLogRequest.getEndTimeMillis());

        String service = searchXLogRequest.getService();
        if ( service != null ){
            paramPack.put(ParamConstant.XLOG_SERVICE, service);
        }

        long objHash = searchXLogRequest.getObjHash();
        if ( objHash != 0 ){
            paramPack.put(ParamConstant.OBJ_HASH, objHash);
        }

        String ipAddr = searchXLogRequest.getIp();
        if ( ipAddr != null ){
            paramPack.put(ParamConstant.XLOG_IP, ipAddr);
        }

        String login = searchXLogRequest.getLogin();
        if (login != null){
            paramPack.put(ParamConstant.XLOG_LOGIN, login);
        }

        String desc = searchXLogRequest.getDesc();
        if ( desc != null ){
            paramPack.put(ParamConstant.XLOG_DESC, desc);
        }

        String textTemp = searchXLogRequest.getText1();
        if ( textTemp != null){
            paramPack.put(ParamConstant.XLOG_TEXT_1, textTemp);
        }
        textTemp = searchXLogRequest.getText2();
        if ( textTemp != null){
            paramPack.put(ParamConstant.XLOG_TEXT_2, textTemp);
        }
        textTemp = searchXLogRequest.getText3();
        if ( textTemp != null){
            paramPack.put(ParamConstant.XLOG_TEXT_3, textTemp);
        }
        textTemp = searchXLogRequest.getText4();
        if ( textTemp != null){
            paramPack.put(ParamConstant.XLOG_TEXT_4, textTemp);
        }
        textTemp = searchXLogRequest.getText5();
        if ( textTemp != null){
            paramPack.put(ParamConstant.XLOG_TEXT_5, textTemp);
        }

        List<Pack> resp = null;

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(searchXLogRequest.getServerId())) {
            resp = tcpProxy.process(RequestCmd.SEARCH_XLOG_LIST, paramPack);
        }

        return resp;
    }
}
