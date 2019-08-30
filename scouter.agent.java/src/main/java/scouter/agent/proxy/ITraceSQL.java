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

package scouter.agent.proxy;

import scouter.agent.trace.SqlParameter;

public interface ITraceSQL {
    Exception getSlowSqlException();
    Exception getTooManyRecordException();
    Exception getConnectionOpenFailException();
    Object start(Object o, String sql, byte methodType);
    Object start(Object o, SqlParameter args, byte methodType);
    Object driverConnect(Object conn, String url);
    Object getConnection(Object conn);
    Object dbcOpenEnd(Object conn, Object stat);
    void ctxLookup(Object this1, Object ctx);
}
