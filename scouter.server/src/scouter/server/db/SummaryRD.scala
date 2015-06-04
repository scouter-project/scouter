/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.server.db;

import scouter.server.db.summary.SummaryDataReader
import scouter.server.db.summary.SummaryIndex
import scouter.util.FileUtil
import java.io.File

object SummaryRD {

    def read(date: String, fromTime: Long, toTime: Long, _type: Byte, handler: (Long, Int, Byte, Long, SummaryDataReader) => Unit) {
        val path = SummaryWR.getDBPath(date);
        if (new File(path).canRead()) {
            val file = path + "/stat";
            var reader: SummaryDataReader = null;
            var index: SummaryIndex = null;
            try {
                reader = SummaryDataReader.open(file);
                index = SummaryIndex.open(file);
                index.read(fromTime, toTime, _type, handler, reader)
            } catch {
                case e: Throwable => e.printStackTrace();
            } finally {
                FileUtil.close(index);
                FileUtil.close(reader);
            }
        }
    }
}