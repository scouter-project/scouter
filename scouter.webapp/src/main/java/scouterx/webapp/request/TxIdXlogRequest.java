package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;

import javax.ws.rs.QueryParam;

/**
 * Created by jaco.ryu on 2017. 10. 13..
 */
@Getter
@Setter
public class TxIdXlogRequest {

    int serverId;

    @QueryParam("lastTxid")
    long lastTxid;

    @QueryParam("lastXLogTime")
    long lastXLogTime;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public void validate() {
        if((lastTxid != 0 && lastXLogTime == 0) || (lastTxid == 0 && lastXLogTime != 0)) {
            throw ErrorState.VALIDATE_ERROR.newBizException("lastTxid and lastXlogTime must coexist!");
        }
    }
}
