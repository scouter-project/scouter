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

import scouter.server.core.cache.TextCache
import scouter.util.HashUtil

object TextPermRD {
  def getString(division: String, hash: Int): String = {
    val out = TextCache.get(division, hash);
    if (out != null)
      return out;

    try {
      val (index, data) = TextPermWR.open(division);
      if (index == null)
        return null;
      val pos = index.get(hash);
      if (pos < 0)
        return null;
      val bytes = data.read(pos);
      val text = new String(bytes, "UTF-8");
      TextCache.put(division, hash, text);
      return text;
    } catch {
      case e: Exception => e.printStackTrace();
    }
    return null;
  }

  def read(division: String, handler: (Array[Byte], Array[Byte]) => Unit) {
    try {
      val (index, data) = TextPermWR.open(division);
      if (index == null)
        return ;
      index.read(handler, data.read);
    } catch {
      case e: Exception => e.printStackTrace();
    }
  }
}
