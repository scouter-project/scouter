package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author Hyanghee Jeon (gaiajeon@gmail.com) on 2017. 8. 27.
 */

@Getter
@Setter
@ToString
public class SearchXLogRequest {

    int serverId;
    
    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;

    @QueryParam("startTimeMillis")
    long startTimeMillis;

    @QueryParam("endTimeMillis")
    long endTimeMillis;

    @QueryParam("startHms")
    String startHms;

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

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public void validate() {
        if (StringUtils.isNotBlank(startHms) || StringUtils.isNotBlank(endHms)) {
            if (StringUtils.isBlank(startHms) || StringUtils.isBlank(endHms)) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startHms and endHms should be not null !");
            }
            if (startTimeMillis > 0 || endTimeMillis > 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms, endYmdHms and startTimeMillis, endTimeMills must not coexist!");
            }

            try {
                setTimeAsYmd();
            } catch (ParseException e) {
                throw ErrorState.VALIDATE_ERROR.newBizException("date is invalid!");
            }
        } else {
            if (startTimeMillis <= 0 || endTimeMillis <= 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startTimeMillis and endTimeMillis must have value!");
            }
        }
    }

    private void setTimeAsYmd() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        startTimeMillis = sdf.parse(yyyymmdd + startHms).getTime();
        endTimeMillis = sdf.parse(yyyymmdd + endHms).getTime();
    }

}
