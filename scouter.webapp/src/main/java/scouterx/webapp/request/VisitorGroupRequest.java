package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.framework.client.server.ServerManager;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Created by csk746(csk746@naver.com) on 2017. 10. 18..
 */
@Getter
@Setter
@ToString
public class VisitorGroupRequest {

    private int serverId;

    @NotNull
    @PathParam("sdate")
    private String sdate;

    @NotNull
    @PathParam("edate")
    private String edate;

    @NotNull
    @QueryParam("objHashes")
    String objHashes;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }
}
