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
import java.io.IOException;
import java.io.RandomAccessFile;

import scouter.util.FileUtil;
import scouter.util.IClose;

class CountBoard(date: String) extends IClose {

    val filename = (GZipCtr.createPath(date) + "/count.dat");
    val file = new File(filename);
    var raf = new RandomAccessFile(file, "rw");
    var counts = 0L;

    if (this.raf.length() >= 8) {
        try {
            this.raf.seek(0);
            this.counts = raf.readLong();
        } catch {
            case e: IOException =>
                this.counts = 0;
                e.printStackTrace();
        }
    } else {
        counts = 0;
    }

    def add(cnt: Long) = set(this.counts + cnt);

    def set(cnt: Long): Long = {
        this.counts = cnt;
        try {
            raf.seek(0);
            raf.writeLong(counts);
        } catch {
            case e: Exception => e.printStackTrace();
        }
        return this.counts;
    }

    def getCount() = this.counts;

    override def close() {
        FileUtil.close(this.raf);
        this.raf = null;
    }
}