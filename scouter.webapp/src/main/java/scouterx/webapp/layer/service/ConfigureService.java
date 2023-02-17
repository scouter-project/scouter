package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.layer.consumer.ConfigureConsumer;
import scouterx.webapp.model.configure.ConfigureData;

/**
 * @author yosong.heo (yosong.heo@gmail.com) on 2023. 2. 17.
 */
public class ConfigureService {
    private final ConfigureConsumer configureConsumer;

    public ConfigureService(){
        this.configureConsumer = new ConfigureConsumer();
    }
    public ConfigureData retrieveServerConfig(final Server server) {
        return this.configureConsumer.retrieveServerConfig(server,true);
    }

    public ConfigureData retrieveObjectConfig(int objHash, Server server) {
        return this.configureConsumer.retrieveObjectConfig(objHash,server);
    }
}
