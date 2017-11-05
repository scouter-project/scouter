package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.VisitorConsumer;
import scouterx.webapp.model.VisitorGroup;
import scouterx.webapp.request.VisitorGroupRequest;

import java.util.List;

/**
 * Created by csk746(csk746@naver.com) on 2017. 10. 13..
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

    public long retrieveVisitorByObj(int objHash, String date, Server server){
        return visitorConsumer.retrieveVisitorByObj(objHash,date,server);
    }

    public long retrieveVisitorTotalByObj(String objType, String date, Server server){
        return visitorConsumer.retrieveVisitorTotalByObj(objType, date, server);
    }

    public VisitorGroup retrieveVisitorGroupByObjHashes(VisitorGroupRequest visitorGroupRequest){

        return visitorConsumer.retrieveVisitorGroupByObjHashes(visitorGroupRequest);
    }

    public List<VisitorGroup> retrieveVisitorHourlyGroupByObjHashes(VisitorGroupRequest visitorGroupRequest){
        return visitorConsumer.retrieveVisitorHourlyGroupByObjHashes(visitorGroupRequest);
    }

}
