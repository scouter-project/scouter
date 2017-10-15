package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Created by jaco.ryu on 2017. 10. 13..
 */
@Getter
@Setter
public class SingleXLogRequest {
    int serverId;

    @NotNull
    @PathParam("txid")
    long txid;

    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public void validate() {
        if(0 == txid || "".equals(yyyymmdd)) {
            throw ErrorState.VALIDATE_ERROR.newBizException("txid and yyyymmdd must coexist!");
        }
    }
}
