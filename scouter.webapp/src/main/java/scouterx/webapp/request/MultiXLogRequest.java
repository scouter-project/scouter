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
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * Created by jaco.ryu on 2017. 10. 13..
 */
@Getter
@Setter
public class MultiXLogRequest {
    int serverId;

    List<Long> txidList;

    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;

    @PathParam("txidList")
    public void setTxidList(String txids) {
        this.txidList = ZZ.splitParamAsLong(txids);
    }

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
    }


}
