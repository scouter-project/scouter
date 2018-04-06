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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Set;

/**
* DTO for pageable XLog request.
* - xlogLoop : (required) xlog loop offset given from previous response. use 0 at first time
* - xLogIndex : (required) xlog offset given from previous response. use 0 at first time
* - serverId : serverId if available (mandatory if it's multi-server connected scouter webapp)
* - objHashes : (required) object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
*
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 2.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class RealTimeXLogRequest {
    @Min(0)
    @PathParam("xlogLoop")
    long xLogLoop;

    @Min(0)
    @PathParam("xlogIndex")
    int xLogIndex;

    int serverId;

    @NotNull
    Set<Integer> objHashes;

    //with AllArgsConstructor
    public RealTimeXLogRequest() {}

    @QueryParam("objHashes")
    public void setObjHashes(String objHashes) {
        this.objHashes = ZZ.splitParamAsIntegerSet(objHashes);
    }

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }
}
