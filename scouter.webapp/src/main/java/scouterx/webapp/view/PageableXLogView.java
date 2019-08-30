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

package scouterx.webapp.view;

import lombok.Getter;
import lombok.Setter;
import scouterx.webapp.model.scouter.SXLog;

import java.util.List;

/**
 * response object about pageable XLog request.
 * - serverId : serverId if available (int)
 * - lastXLogTime : It's for next paging request (long)
 * - lastTxid : It's for next paging request (long)
 * - hasMore : more data to retrieve (boolean)
 * - xLogs : SXlog object array
 *
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 30.
 */
@Getter
@Setter
public class PageableXLogView {
    long lastXLogTime;
    long lastTxid;
    boolean hasMore;
    List<SXLog> xLogs;
}
