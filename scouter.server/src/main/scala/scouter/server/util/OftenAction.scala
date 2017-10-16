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

package scouter.server.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import scouter.util.CompareUtil;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.StringLongLinkedMap;
import scouter.util.ThreadUtil;

object OftenAction {

    val lastLog = new StringLongLinkedMap().setMax(1000);
    private def checkOk(id: String, sec: Int): Boolean = {
        if (sec > 0) {
            val last = lastLog.get(id);
            val now = System.currentTimeMillis();
            if (now < last + sec * 1000)
                return false;
            lastLog.put(id, now);
        }
        return true;
    }

    def act(id: String, sec: Int)(codeBlock: => Unit): Boolean = {
        if (checkOk(id, sec) == true) {
            codeBlock
            return true
        } else {
            return false
        }
    }
}