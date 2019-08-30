/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@Setter
@ToString
public class CounterRequest {
    private final long limitRangeSec = 6 * 60 * 60; //6 hour

    private int serverId;

    @NotNull
    @PathParam("counter")
    private String counter;

    //exclusive with startTimeMillis
    @QueryParam("startYmdHms")
    private String startYmdHms;

    //exclusive with endTimeMillis
    @QueryParam("endYmdHms")
    private String endYmdHms;

    @QueryParam("startTimeMillis")
    private long startTimeMillis;

    @QueryParam("endTimeMillis")
    private long endTimeMillis;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }

    public void validate() {
        if (StringUtils.isNotBlank(startYmdHms) || StringUtils.isNotBlank(endYmdHms)) {
            if (StringUtils.isBlank(startYmdHms) || StringUtils.isBlank(endYmdHms)) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms and endYmdHms should be not null !");
            }
            if (startTimeMillis > 0 || endTimeMillis > 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startYmdHms, endYmdHms and startTimeMillis, endTimeMills must not coexist!");
            }

            setTimeAsYmd();
        } else {
            if (startTimeMillis <= 0 || endTimeMillis <= 0) {
                throw ErrorState.VALIDATE_ERROR.newBizException("startTimeMillis and endTimeMillis must have value!");
            }
        }

        if (endTimeMillis - startTimeMillis > limitRangeSec * 1000L) {
            throw ErrorState.VALIDATE_ERROR.newBizException("query range should be lower than " + limitRangeSec + " seconds!");
        }
    }

    private void setTimeAsYmd() {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime startDateTime = LocalDateTime.parse(startYmdHms, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endYmdHms, formatter);

        startTimeMillis = startDateTime.atZone(zoneId).toEpochSecond() * 1000L;
        endTimeMillis = endDateTime.atZone(zoneId).toEpochSecond() * 1000L;
    }
}
