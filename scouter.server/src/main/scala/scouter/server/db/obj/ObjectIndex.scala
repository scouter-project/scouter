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

package scouter.server.db.obj;

import java.io.IOException;
import java.util.Hashtable;

import scouter.server.db.io.IndexKeyFile;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.FileUtil;
import scouter.util.IClose;
object ObjectIndex {
    val table = new Hashtable[String, ObjectIndex]();

    def open(file: String): ObjectIndex = {
        table.synchronized {
            var index = table.get(file);
            if (index != null) {
                index.refrence += 1;
                return index;
            } else {
                index = new ObjectIndex(file);
                table.put(file, index);
                return index;
            }
        }
    }
}
class ObjectIndex(_file: String) extends IClose {

    val file = _file
    var refrence = 0;
    var index: IndexKeyFile = null

    def set(key: Int, pos: Long) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        this.index.put(DataOutputX.toBytes(key), DataOutputX.toBytes5(pos));
    }

    def get(key: Int): Long = {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        val buf = this.index.get(DataOutputX.toBytes(key));
        if (buf == null) -1 else DataInputX.toLong5(buf, 0)
    }
    def delete(key: Int) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        this.index.delete(DataOutputX.toBytes(key));
    }
    def read(handler: (Array[Byte], Array[Byte]) => Any, reader: (Long)=>Array[Byte]) {
        if (this.index == null) {
            this.index = new IndexKeyFile(file);
        }
        this.index.read(handler, reader);
    }

    override  def close() {
        ObjectIndex.table.synchronized {
            if (this.refrence == 0) {
                ObjectIndex.table.remove(this.file);
                FileUtil.close(this.index);
                this.index = null;
            } else {
                this.refrence -= 1;
            }
        }
    }

}