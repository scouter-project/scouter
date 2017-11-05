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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Created by csk746(csk746@naver.com) on 2017. 10. 18..
 */
@Getter
@Setter
@ToString
public class VisitorGroupRequest {

    private int serverId;

    @QueryParam("startYmd")
    private String startYmd;

    @QueryParam("endYmd")
    private String endYmd;

    @QueryParam("startYmdH")
    private long startYmdH;

    @QueryParam("endYmdH")
    private long endYmdH;

    @PathParam("objHashes")
    String objHashes;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }
    public void validate() {
        if (StringUtils.isNotBlank(startYmd) || StringUtils.isNotBlank(endYmd)) {
            if (StringUtils.isBlank(startYmd) || StringUtils.isBlank(endYmd)) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmd and endYmd should be not null !");
            }
            if (startYmdH > 0 || endYmdH > 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmd, endYmd and startYmdH, endYmdH must not coexist!");
            }

            setTimeAsYmd();
        } else {
            if (startYmdH <= 0 || endYmdH <= 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdH and endYmdH must have value!");
            }
        }
    }
    private void setTimeAsYmd() {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        LocalDateTime startDateTime = LocalDateTime.parse(startYmd, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endYmd, formatter);

        startYmdH = startDateTime.atZone(zoneId).toEpochSecond() * 1000L;
        endYmdH = endDateTime.atZone(zoneId).toEpochSecond() * 1000L;
    }
}
