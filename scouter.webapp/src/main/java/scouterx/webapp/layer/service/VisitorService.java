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

    public long retrieveVisitorRealTimeByObj(int objHash, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObj(objHash, server);
    }

    public long retrieveVisitorRealTimeByObjType(String objHash, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObjType(objHash, server);
    }

    public long retrieveVisitorRealTimeByObjHashes(List<Integer> objHashes, Server server){
        return visitorConsumer.retrieveVisitorRealTimeByObjHashes(objHashes, server);
    }
}
