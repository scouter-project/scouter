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

import scouter.server.db.io.IndexKeyFile
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.TextTypes
import scouter.server.Configure
import scouter.util.FileUtil
import scouter.util.IClose
import scouter.util.StringKeyLinkedMap

object TextPermIndex {
  private val conf = Configure.getInstance()
  private val DEFAULT_MB = conf._mgr_text_db_index_default_mb
  private val SERVICE_MB = conf._mgr_text_db_index_service_mb
  private val APICALL_MB = conf._mgr_text_db_index_api_mb
  private val USERAGENT_MB = conf._mgr_text_db_index_ua_mb
  private val LOGIN_MB = conf._mgr_text_db_index_login_mb
  private val DESC_MB = conf._mgr_text_db_index_desc_mb
  private val HMSG_MB = conf._mgr_text_db_index_hmsg_mb

  val table = new StringKeyLinkedMap[TextPermIndex]();

  def get(div: String): TextPermIndex = {
    table.synchronized {
      var index = table.get(div);
      if (index != null) {
        index.refrence += 1;
        return index;
      } else return null;
    }
  }
  def open(div: String, file: String): TextPermIndex = {
    table.synchronized {
      var index = table.get(div);
      if (index != null) {
        index.refrence += 1;
        return index;
      } else {
        index = new TextPermIndex(div, file);
        table.put(div, index);
        return index;
      }
    }
  }
  def closeAll() {
    while (table.size() > 0) {
      table.removeFirst().close();
    }
  }
}
class TextPermIndex(div: String, file: String) extends IClose {

  var refrence = 0;
  var index: IndexKeyFile = null

  def set(key: Int, dataPos: Long) {
    if (this.index == null) {
      this.index = newIndexKeyFileByType()
    }
    this.index.put(DataOutputX.toBytes(key), DataOutputX.toBytes5(dataPos))
  }

  def get(key: Int): Long = {
    if (this.index == null) {
      this.index = newIndexKeyFileByType()
    }
    val buf = this.index.get(DataOutputX.toBytes(key))
    if (buf == null) -1 else DataInputX.toLong5(buf, 0)
  }
  def hasKey(key: Int): Boolean = {
    if (this.index == null) {
      this.index = newIndexKeyFileByType()
    }
    return this.index.hasKey(DataOutputX.toBytes(key))
  }
  def read(handler: (Array[Byte], Array[Byte]) => Any, reader: (Long) => Array[Byte]) {
    if (this.index == null) {
      this.index = newIndexKeyFileByType()
    }
    this.index.read(handler, reader)
  }

  def newIndexKeyFileByType(): IndexKeyFile = {
    div match {
      case TextTypes.SERVICE => new IndexKeyFile(file, TextPermIndex.SERVICE_MB)
      case TextTypes.APICALL => new IndexKeyFile(file, TextPermIndex.APICALL_MB)
      case TextTypes.USER_AGENT => new IndexKeyFile(file, TextPermIndex.USERAGENT_MB)
      case TextTypes.LOGIN => new IndexKeyFile(file, TextPermIndex.LOGIN_MB)
      case TextTypes.DESC => new IndexKeyFile(file, TextPermIndex.DESC_MB)
      case TextTypes.HASH_MSG => new IndexKeyFile(file, TextPermIndex.HMSG_MB)
      case _ => new IndexKeyFile(file, TextPermIndex.DEFAULT_MB)
    }
  }

  override def close() {
    TextPermIndex.table.synchronized {
      if (this.refrence == 0) {
        TextPermIndex.table.remove(this.div);
        FileUtil.close(this.index);
        this.index = null;
      } else {
        this.refrence -= 1;
      }
    }
  }

}