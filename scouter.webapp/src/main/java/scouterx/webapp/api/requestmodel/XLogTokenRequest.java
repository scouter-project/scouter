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

package scouterx.webapp.api.requestmodel;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import scouterx.webapp.util.ZZ;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 30.
 */
@Getter
@Setter
@ToString
public class XLogTokenRequest {
    public static final int MAX_COUNT = 1_000_000;

    @NotNull
    @PathParam("date")
    String date;

    @QueryParam("startTime")
    int serverId;

    @NotNull
    @QueryParam("startTime")
    long startTime;

    @NotNull
    @QueryParam("endTime")
    long endTime;

    List<Integer> objHashes;

    @NotNull
    @QueryParam("objHashes")
    public void setObjHashes(String objHashes) {
        ZZ.splitParamAsInteger(objHashes);
    }

    @Max(MAX_COUNT)
    @QueryParam("maxCount")
    int maxCount = MAX_COUNT;

}
