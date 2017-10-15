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

    public long retrieveRealTimeVisitorByObj(int objHash, Server server){
        return visitorConsumer.retrieveRealTimeVisitorByObj(objHash, server);
    }

    public long retrieveRealTimeVisitorByObjType(String objHash, Server server){
        return visitorConsumer.retrieveRealTimeVisitorByObjType(objHash, server);
    }

    public long retrieveRealTimeVisitorByObjHashes(List<Integer> objHashes, Server server){
        return visitorConsumer.retrieveRealTimeVisitorByObjHashes(objHashes, server);
    }
}
