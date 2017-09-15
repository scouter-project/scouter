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

package scouterx.webapp.api.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.client.server.Server;
import scouterx.client.server.ServerManager;

import javax.validation.constraints.Min;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
@Getter
@Setter
@ToString
public class SummaryRequest {
    private Server server;

    @Min(0)
    @QueryParam("start")
    private long start;

    @Min(0)
    @QueryParam("end")
    private long end;

    @PathParam("objType")
    private String objType;

    @PathParam("objHash")
    private int objHash;

    @QueryParam("serverId")
    public void setServerId(int serverId) {
        this.server = ServerManager.getInstance().getServerIfNullDefault(serverId);
    }
}
