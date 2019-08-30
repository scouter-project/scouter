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

package scouter.server.db.text;

import java.util.Hashtable

import scouter.io.DataOutputX
import scouter.server.Configure
import scouter.server.db.io.IndexKeyFile
import scouter.util.{FileUtil, HashUtil, ICloseDB}

object TextTable {
    val conf = Configure.getInstance()
    val INDEX_KEY_SIZE_MB = conf._mgr_text_db_daily_index_mb
    val table = new Hashtable[String, TextTable]();

    def open(filePath: String): TextTable = {
        table.synchronized {
            var textTable = table.get(filePath);
            if (textTable != null) {
                textTable.refrence += 1;
                return textTable;
            } else {
                textTable = new TextTable(filePath);
                table.put(filePath, textTable);
                return textTable;
            }
        }
    }
}

class TextTable(_file: String) extends ICloseDB {
    val file = _file
    var refrence = 0
    var index: IndexKeyFile = null

    var lastActive = 0L
    def getLastActive(): Long = {
        return lastActive
    }
    def setActive(time: Long) {
        lastActive = time
    }

    def set(div: String, key: Int, value: Array[Byte]) {
        if (this.index == null) {
            this.index = newIndexKeyFile()
        }
        this.index.put(new DataOutputX().writeInt(HashUtil.hash(div)).writeInt(key).toByteArray(), value)
    }

    def get(div: String, key: Int): Array[Byte] = {
        if (this.index == null) {
            this.index = newIndexKeyFile()
        }
        return this.index.get(new DataOutputX().writeInt(HashUtil.hash(div)).writeInt(key).toByteArray())
    }

    def hasKey(div: String, key: Int): Boolean = {
        if (this.index == null) {
            this.index = newIndexKeyFile()
        }
        return this.index.hasKey(new DataOutputX().writeInt(HashUtil.hash(div)).writeInt(key).toByteArray())
    }

    def read(handler: (Array[Byte], Array[Byte]) => Any) {
        if (this.index == null) {
            this.index = newIndexKeyFile()
        }
        this.index.read(handler)
    }

    def newIndexKeyFile(): IndexKeyFile = {
        return new IndexKeyFile(file, TextTable.INDEX_KEY_SIZE_MB);
    }

    override def close() {
        TextTable.table.synchronized {
            if (this.refrence == 0) {
                TextTable.table.remove(this.file);
                FileUtil.close(this.index);
                this.index = null;
            } else {
                this.refrence -= 1;
            }
        }
    }
}