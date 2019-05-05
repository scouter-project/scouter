package scouterx.webapp.layer.consumer;

import org.apache.commons.collections.CollectionUtils;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.model.VisitorGroup;
import scouterx.webapp.request.VisitorGroupRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by csk746(csk746@naver.com) on 2017. 10. 13..
 */
public class VisitorConsumer {

    public long retrieveVisitorRealTimeByObj(int objType, final Server server) {
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_HASH, objType);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorRealTimeByObjType(String objType, final Server server) {
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_TYPE, objType);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME_TOTAL, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorRealTimeByObjHashes(List<Integer> objHashes, final Server server) {
        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        for (Integer obj : objHashes) {
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME_GROUP, param);
        }

        if (value != null) {
            return ((DecimalValue) value).value;
        } else {
            return 0;
        }
    }

    public long retrieveVisitorByObj(int objHash, String date, final Server server) {
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_HASH, objHash);
        param.put(ParamConstant.DATE, date);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_LOADDATE, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorTotalByObj(String objType, String date, final Server server) {

        MapPack param = new MapPack();

        param.put(ParamConstant.OBJ_TYPE, objType);
        param.put(ParamConstant.DATE, date);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_LOADDATE_TOTAL, param);
        }

        return ((DecimalValue) value).value;
    }

    public VisitorGroup retrieveVisitorGroupByObjHashes(VisitorGroupRequest visitorGroupRequest) {

        MapPack param = getVisitorGroupPack(visitorGroupRequest);
        Server server = ServerManager.getInstance().getServerIfNullDefault(visitorGroupRequest.getServerId());

        Pack pack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            pack = tcpProxy.getSingle(RequestCmd.VISITOR_LOADDATE_GROUP, param);
        }

        return VisitorGroup.of((MapPack) pack);
    }

    public List<VisitorGroup> retrieveVisitorHourlyGroupByObjHashes(VisitorGroupRequest visitorGroupRequest) {

        MapPack param = getVisitorHourlyGroupPack(visitorGroupRequest);
        Server server = ServerManager.getInstance().getServerIfNullDefault(visitorGroupRequest.getServerId());

        List<Pack> results;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            results = tcpProxy.process(RequestCmd.VISITOR_LOADHOUR_GROUP, param);
        }

        return results.stream()
                .map(pack -> (MapPack) pack)
                .map(VisitorGroup::of)
                .collect(Collectors.toList());
    }

    private MapPack getVisitorGroupPack(VisitorGroupRequest visitorGroupRequest) {

        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        String objHashes = visitorGroupRequest.getObjHashes();
        String sdate = visitorGroupRequest.getStartYmd();
        String edate = visitorGroupRequest.getEndYmd();

        List<Integer> objList = ZZ.splitParamAsInteger(objHashes);

        if (CollectionUtils.isEmpty(objList)) {
            throw ErrorState.VALIDATE_ERROR.newBizException("Query parameter 'objHashes' is required!");
        }

        for (Integer obj : objList) {
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);
        param.put(ParamConstant.SDATE, sdate);
        param.put(ParamConstant.EDATE, edate);

        return param;
    }

    private MapPack getVisitorHourlyGroupPack(VisitorGroupRequest visitorGroupRequest) {

        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        String objHashes = visitorGroupRequest.getObjHashes();
        long stime = visitorGroupRequest.getStartYmdH();
        long etime = visitorGroupRequest.getEndYmdH();

        List<Integer> objList = ZZ.splitParamAsInteger(objHashes);

        if (CollectionUtils.isEmpty(objList)) {
            throw ErrorState.VALIDATE_ERROR.newBizException("Query parameter 'objHashes' is required!");
        }

        for (Integer obj : objList) {
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);
        param.put(ParamConstant.STIME, stime);
        param.put(ParamConstant.ETIME, etime);

        return param;
    }
}
