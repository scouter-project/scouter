package scouterx.webapp.layer.service;

import scouterx.webapp.framework.client.net.INetReader;
import scouterx.webapp.layer.consumer.XLogConsumer;
import scouterx.webapp.request.CondSearchXLogRequest;

public class XLogSearchService {
	
    private final XLogConsumer xLogConsumer;

    public XLogSearchService() {
        this.xLogConsumer = new XLogConsumer();
    }

    /**
     * retrieve XLog List for paging access
     */
    public void handleCondSearchXLog(final CondSearchXLogRequest condXLogRequest, final INetReader reader) {

        xLogConsumer.handleConditionSearchXLog(condXLogRequest, reader);
    }

}
