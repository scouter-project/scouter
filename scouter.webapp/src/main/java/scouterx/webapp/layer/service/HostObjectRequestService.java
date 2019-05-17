package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.HostObjectRequestConsumer;
import scouterx.webapp.model.HostDiskData;
import scouterx.webapp.model.ProcessObject;

import java.util.List;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 12.
 */
public class HostObjectRequestService {

    private final HostObjectRequestConsumer objectRequestConsumer;

    public HostObjectRequestService() {
        this.objectRequestConsumer = new HostObjectRequestConsumer();
    }

    public List<ProcessObject> retrieveRealTimeTopByObjType(final int objHash, final Server server) {
        return objectRequestConsumer.retrieveRealTimeTopByObjType(objHash, server);
    }

    // get disk usage information
    public List<HostDiskData> retrieveRealTimeDiskByObjType(int objHash, Server server) {
        return objectRequestConsumer.retrieveRealTimeDiskByObjType(objHash, server);
    }
}