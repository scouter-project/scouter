package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Created by csk746(csk746@naver.com) on 2017. 11. 7..
 */
@Getter
@Setter
public class GxidXLogRequest {

    int serverId;

    @NotNull
    @PathParam("gxid")
    private long gxid;

    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;


    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public void validate() {
        if(0 == gxid || "".equals(yyyymmdd)) {
            throw ErrorState.VALIDATE_ERROR.newBizException("gxid and yyyymmdd must coexist!");
        }
    }

}
