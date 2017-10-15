package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.ObjectConsumer;
import scouterx.webapp.model.ProcessObject;

import java.util.List;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 */
public class ObjectService {

    private final ObjectConsumer objectConsumer;

    public ObjectService() {
        this.objectConsumer = new ObjectConsumer();
    }

    public List<ProcessObject> retrieveRealTimeTopByObjType(final int objHash, final Server server) {
        return objectConsumer.retrieveRealTimeTopByObjType(objHash, server);
    }
}
