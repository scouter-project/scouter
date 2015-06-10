/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.server.db.io.zip;

import java.io.File;

import scouter.server.db.DBCtr;
import scouter.server.db.XLogWR;

object GZipCtr {

    val MAX_QUE_SIZE = 20000;

    val BLOCK_MAX_SIZE = 32 * 1024 * 1024;

    def getDataPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date);
        sb.append(XLogWR.dir);
        sb.append("/data");
        return sb.toString();
    }
    def createPath(date: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date);
        sb.append(XLogWR.dir);
        sb.append("/data");
        val path = sb.toString();
        new File(path).mkdirs();
        return path;
    }
}