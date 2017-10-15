package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;

import java.util.List;

/**
 * Created by geonheelee on 2017. 10. 13..
 */
public class VisitorConsumer {

    public long retrieveVisitorRealTimeByObj(int objType, final Server server){
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_HASH, objType);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorRealTimeByObjType(String objType, final Server server){
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_TYPE, objType);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME_TOTAL, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorRealTimeByObjHashes(List<Integer> objHashes, final Server server){
        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        for (Integer obj : objHashes){
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_REALTIME_GROUP, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorLoaddateByObjAndDate(int objType, String date, final Server server){
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_HASH, objType);
        param.put(ParamConstant.DATE, date);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_LOADDATE, param);
        }

        return ((DecimalValue) value).value;
    }

    public long retrieveVisitorLoaddateTotalByObjAndDate(int objType, String date, final Server server){
        MapPack param = new MapPack();
        param.put(ParamConstant.OBJ_HASH, objType);
        param.put(ParamConstant.DATE, date);

        Value value;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            value = tcpProxy.getSingleValue(RequestCmd.VISITOR_LOADDATE_TOTAL, param);
        }

        return ((DecimalValue) value).value;
    }

    public MapPack retrieveVisitorLoaddateGroupByObjHashesAndDate(List<Integer> objHashes, String sdate, String edate, final Server server){
        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        for (Integer obj : objHashes){
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);
        param.put(ParamConstant.SDATE, sdate);
        param.put(ParamConstant.EDATE, edate);

        Pack pack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            pack = tcpProxy.getSingle(RequestCmd.VISITOR_LOADDATE_GROUP, param);
        }

        return ((MapPack) pack);
    }

    public MapPack retrieveVisitorLoadhourGroupByObjHashesAndDate(List<Integer> objHashes, String sdate, String edate, final Server server){
        MapPack param = new MapPack();
        ListValue listValue = new ListValue();

        for (Integer obj : objHashes){
            listValue.add(obj);
        }

        param.put(ParamConstant.OBJ_HASH, listValue);
        param.put(ParamConstant.SDATE, sdate);
        param.put(ParamConstant.EDATE, edate);

        Pack pack;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            pack = tcpProxy.getSingle(RequestCmd.VISITOR_LOADHOUR_GROUP, param);
        }

        return ((MapPack) pack);
    }
}
