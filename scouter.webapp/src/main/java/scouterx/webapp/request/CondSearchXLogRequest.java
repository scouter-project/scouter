package scouterx.webapp.request;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.framework.exception.ErrorState;

@Getter
@Setter
@ToString
public class CondSearchXLogRequest {

    int serverId;
    
    @QueryParam("startYmdHm")
    String startYmdHm;

    @NotNull
    @QueryParam("endYmdHm")
    String endYmdHm;

    
    @QueryParam("startTime")
    long startTime;

    @NotNull
    @QueryParam("endTime")
    long endTime;
    
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
    
    public void validate() {
        if (StringUtils.isNotBlank(startYmdHm) || StringUtils.isNotBlank(endYmdHm)) {
            if (StringUtils.isBlank(startYmdHm) || StringUtils.isBlank(endYmdHm)) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms and endYmdHms should be not null !");
            }
            if (startTime > 0 || endTime > 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms, endYmdHms and startTime, endTime must not coexist!");
            }

            setTimeAsYmd();
        } else {
            throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms and endYmdHms should be not null !");
        }
    }
    
    private void setTimeAsYmd() {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime startDateTime = LocalDateTime.parse(startYmdHm, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endYmdHm, formatter);

        startTime = startDateTime.atZone(zoneId).toEpochSecond() * 1000L;
        endTime = endDateTime.atZone(zoneId).toEpochSecond() * 1000L;
    }
    
}
