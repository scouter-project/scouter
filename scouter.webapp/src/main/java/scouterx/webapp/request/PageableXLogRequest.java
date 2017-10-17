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
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.exception.ErrorState;
import scouterx.webapp.framework.util.ZZ;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Set;

/**
* DTO for pageable XLog request.
* - date : (required) date to retrieve as String format YYYYMMDD
* - serverId : serverId if available (mandatory if it's multi-server connected scouter webapp)
* - startTimeMillis : (required) start time as milliseconds(long)
* - endTimeMillis : (required) end time as milliseconds(long)
* - objHashes : (required) object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
* - pageCount : count to retrieve in one time. (max limit is 30,000, default 10,000)
* - lastTxid : available from previous response for paging support. (long)
* - lastXLogTime : available from previous response for paging support. (long)
*
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 30.
 */
@Getter
@Setter
@ToString
public class PageableXLogRequest {
    public static final int MAX_PAGE_COUNT = 30_000;
    public static final int DEFAULT_PAGE_COUNT = 10_000;

    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;

    int serverId;

    @NotNull
    @Min(1)
    @QueryParam("startTimeMillis")
    long startTimeMillis;

    @NotNull
    @Min(1)
    @QueryParam("endTimeMillis")
    long endTimeMillis;

    @NotNull
    Set<Integer> objHashes;

    @QueryParam("objHashes")
    public void setObjHashes(String objHashes) {
        this.objHashes = ZZ.splitParamAsIntegerSet(objHashes);
    }

    @Max(MAX_PAGE_COUNT)
    int pageCount;

    @QueryParam("pageCount")
    public void setPageCount(int pageCount) {
        if(pageCount > 0) {
            this.pageCount = pageCount;
        } else {
            this.pageCount = DEFAULT_PAGE_COUNT;
        }
    }

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
