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
import scouterx.webapp.framework.util.ZZ;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Set;

/**
* DTO for pageable XLog request.
* - offset1 : (required) xlog offset1 given from previous response. use 0 at first time
* - offset2 : (required) xlog offset2 given from previous response. use 0 at first time
* - serverId : serverId if available (mandatory if it's multi-server connected scouter webapp)
* - objHashes : (required) object hashes by comma separator also allowed with bracket. eg) 10011,10012 or [10011,10012]
*
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 2.
 */
@Getter
@Setter
@ToString
public class RealTimeXLogDataRequest {
    int serverId;

    long xLogLoop;

    int xLogIndex;

    //due to swagger bug, define setter.
    @PathParam("offset1")
    public void setXLogLoop(long xLogLoop) {
        this.xLogLoop = xLogLoop;
    }

    //due to swagger bug, define setter.
    @PathParam("offset2")
    public void setXLogIndex(int xLogIndex) {
        this.xLogIndex = xLogIndex;
    }

    @NotNull
    Set<Integer> objHashes;

    @QueryParam("objHashes")
    public void setObjHashes(String objHashes) {
        this.objHashes = ZZ.splitParamAsIntegerSet(objHashes);
    }

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }
}
