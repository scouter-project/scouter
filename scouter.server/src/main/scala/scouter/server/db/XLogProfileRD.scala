/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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
 *
 */

package scouter.server.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import scouter.lang.step.StepControl;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.ShutdownManager;
import scouter.server.db.xlog.XLogProfileIndex;
import scouter.server.db.xlog.XLogProfileDataReader;
import scouter.server.db.xlog.XLogProfileDataWriter;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.RequestQueue;
import scouter.util.IClose;
import scouter.util.IShutdown;
import scouter.util.KeyGen;
import scouter.util.Queue;
import scouter.util.ThreadUtil;

object XLogProfileRD {
    val prefix = "xlog";

    class ResultSet(_keys: java.util.List[Long], _reader: XLogProfileDataReader) {
        def this() = this(null, null)
        var x = 0
        var keys = _keys
        var reader = _reader
        var max = if (keys == null) 0 else keys.size()

        def hasNext() = x < max

        def readNext(): Array[Byte] = {
            if (x >= max || reader == null)
                return null;
            x += 1
            return reader.read(keys.get(x - 1));
        }

        def close() = {
            if (reader != null) {
                reader.close();
                reader = null
            }
        }
    }

    def getProfile(date: String, txid: Long, max: Int): Array[Byte] = {
        val out = new ByteArrayOutputStream();

        val path = XLogProfileWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return out.toByteArray();
        }
        val file = path + "/" + XLogProfileWR.prefix;
        var result: List[Long] = null;
        val idx = XLogProfileIndex.open(file);
        try {
            result = idx.getByTxid(txid);
        } finally {
            idx.close();
        }

        if (result == null) {
            return null;
        }

        val reader = XLogProfileDataReader.open(date, file);
        try {
            var blockCnt = 0
            for (i <- 0 to result.size() - 1) {
                if (max > 0 && blockCnt >= max) {
                    val mStep = new StepControl();
                    mStep.code = 0;
                    mStep.message = " ** Profile Truncated ** ";
                    out.write(new DataOutputX().writeStep(mStep).toByteArray());
                    return out.toByteArray();
                }
                val buff = reader.read(result.get(i));
                if (buff != null) {
                    out.write(buff);
                    blockCnt += 1
                }
            }
        } finally {
            reader.close();
        }
        return out.toByteArray();
    }

    def getFullProfile(date: String, txid: Long, max: Int, handler: (Array[Byte]) => Any) {
        try {
            val path = XLogProfileWR.getDBPath(date);
            if (new File(path).canRead() == false) {
                return ;
            }
            val file = path + "/" + XLogProfileWR.prefix;
            var result: List[Long] = null;
            val idx = XLogProfileIndex.open(file);
            try {
                result = idx.getByTxid(txid);
            } finally {
                idx.close();
            }

            if (result == null) {
                return ;
            }

            val reader = XLogProfileDataReader.open(date, file);
            try {
                for (i <- 0 to result.size() - 1) {
                    val buff = reader.read(result.get(i).longValue());
                    if (buff != null) {
                        if (handler(buff) == false)
                            return ;
                    }
                }
            } finally {
                reader.close();
            }
        } catch {
            case ex: Exception => {
                ex.printStackTrace();
            }
        }
    }

    def executeQuery(date: String, txid: Long): ResultSet = {
        val path = XLogProfileWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return new ResultSet();
        }
        val file = path + "/" + prefix;
        var result: List[Long] = null;
        val idx = XLogProfileIndex.open(file);
        try {
            result = idx.getByTxid(txid);
        } finally {
            idx.close();
        }
        val reader = XLogProfileDataReader.open(date, file);
        return new ResultSet(result, reader);
    }
}