package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.VisitorConsumer;

import java.util.List;

/**
 * Created by geonheelee on 2017. 10. 13..
 */
public class VisitorService {

    private final VisitorConsumer visitorConsumer;

    public VisitorService() {
        this.visitorConsumer = new VisitorConsumer();
    }

    public long retrieveVisitorRealTimeCountersByObjId(int objHash, Server server){
        return visitorConsumer.getVisitorRealTime(objHash,server);
    }

    public long retrieveVisitorTotalRealTimeCounterByObjType(String objHash, Server server){
        return visitorConsumer.getVisitorRealTimeTotal(objHash,server);
    }

    public long retrieveVisitorGroupRealTimeCounterByObjId(List<Integer> objHashes, Server server){
        return visitorConsumer.getVisitorRealTimeGroup(objHashes,server);
    }
}
