package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.HostObjectRequestConsumer;
import scouterx.webapp.model.ProcessObject;

import java.util.List;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 */
public class HostObjectRequestService {

    private final HostObjectRequestConsumer objectRequestConsumer;

    public HostObjectRequestService() {
        this.objectRequestConsumer = new HostObjectRequestConsumer();
    }

    public List<ProcessObject> retrieveRealTimeTopByObjType(final int objHash, final Server server) {
        return objectRequestConsumer.retrieveRealTimeTopByObjType(objHash, server);
    }
}
