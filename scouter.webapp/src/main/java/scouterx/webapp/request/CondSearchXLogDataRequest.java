package scouterx.webapp.request;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.framework.client.server.ServerManager;

/**
 * @author Hyanghee Jeon (gaiajeon@gmail.com) on 2017. 8. 27.
 */

@Getter
@Setter
@ToString
public class CondSearchXLogDataRequest {

	@NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;
	
	@QueryParam("serverId")
    int serverId;

    @NotNull
    @QueryParam("startHms")
    String startHms;

    @NotNull
    @QueryParam("endHms")
    String endHms;
    
    
    @QueryParam("objHash")
    long objHash;    
    
    @QueryParam("service")
    String service ;

    @QueryParam("ip")
    String ip;
    
    @QueryParam("login")
    String login;
    
    @QueryParam("desc")
    String desc;
    
    @QueryParam("text1")
    String text1;

    @QueryParam("text2")
    String text2;
    
    @QueryParam("text3")
    String text3;
    
    @QueryParam("text4")
    String text4;
    
    @QueryParam("text5")
    String text5;
    
    
    public CondSearchXLogDataRequest(){
    }
    
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    
}
