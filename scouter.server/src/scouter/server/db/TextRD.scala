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
import scouter.server.db.text.TextTable
import scouter.util.HashUtil
import scouter.lang.TextTypes

object TextRD {

  def getString(date: String, divs: String, hash: Int): String = {
    val out = TextCache.get(divs, hash);
    if (out != null)
      return out;

    try {
      if (TextPermWR.isA(divs)) {
        return TextPermRD.getString(divs, hash);
      }
      val table = TextWR.open(date)
      val b = table.get(divs, hash);

      if (b == null) {
        return TextPermRD.getString(divs, hash)
        //return null;
      }

      val text = new String(b, "UTF-8");
      TextCache.put(divs, hash, text);
      return text;

    } catch {
      case e: Exception => e.printStackTrace()
    }
    return null;
  }
}
