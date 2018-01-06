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
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;

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
public class SummaryRequest {
    private Server server;

    @QueryParam("startTimeMillis")
    private long startTimeMillis;

    @QueryParam("endTimeMillis")
    private long endTimeMillis;

    @QueryParam("startYmdHm")
    private String startYmdHm;

    @QueryParam("endYmdHm")
    private String endYmdHm;

    //@PathParam("objType")
    private String objType;

    //@PathParam("objHash")
    private int objHash;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.server = ServerManager.getInstance().getServerIfNullDefault(serverId);
    }

    public void validate() {
        if (StringUtils.isNotBlank(startYmdHm) || StringUtils.isNotBlank(endYmdHm)) {
            if (StringUtils.isBlank(startYmdHm) || StringUtils.isBlank(endYmdHm)) {
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
    }

    private void setTimeAsYmd() {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime startDateTime = LocalDateTime.parse(startYmdHm, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endYmdHm, formatter);

        startTimeMillis = startDateTime.atZone(zoneId).toEpochSecond() * 1000L;
        endTimeMillis = endDateTime.atZone(zoneId).toEpochSecond() * 1000L;
    }

}
