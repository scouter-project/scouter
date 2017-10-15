package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.VisitorConsumer;
import scouterx.webapp.model.VisitorGroup;

import java.util.List;

/**
 * Created by geonheelee on 2017. 10. 13..
 */
public class VisitorService {

    private final VisitorConsumer visitorConsumer;

    public VisitorService() {
        this.visitorConsumer = new VisitorConsumer();
    }

    public long retrieveVisitorRealTimeByObj(int objHash, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObj(objHash, server);
    }

    public long retrieveVisitorRealTimeByObjType(String objHash, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObjType(objHash, server);
    }

    public long retrieveVisitorRealTimeByObjHashes(List<Integer> objHashes, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObjHashes(objHashes, server);
    }

    public long retrieveVisitorLoaddateByObjAndDate(int objHash, String date, Server server){
        return visitorConsumer.retrieveVisitorLoaddateByObjAndDate(objHash,date,server);
    }

    public long retrieveVisitorLoaddateTotalByObjAndDate(String objType, String date, Server server){
        return visitorConsumer.retrieveVisitorLoaddateTotalByObjAndDate(objType, date, server);
    }

    public VisitorGroup retrieveVisitorLoaddateGroupByObjHashesAndDate(List<Integer> ohjHashes, String sdate, String edate, final Server server){
        return visitorConsumer.retrieveVisitorLoaddateGroupByObjHashesAndDate(ohjHashes, sdate, edate, server);
    }

    public List<VisitorGroup> retrieveVisitorLoadhourGroupByObjHashesAndDate(List<Integer> objHashes, String sdate, String edate, final Server server){
        return visitorConsumer.retrieveVisitorLoadhourGroupByObjHashesAndDate(objHashes, sdate, edate, server);
    }

}
