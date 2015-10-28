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

package scouter.server.netio.data;

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import scouter.server.core.cache.TextCache
import scouter.lang.TextTypes

class MultiPacket(total: Int, objHash: Int, addr: InetAddress) {
    var added = 0
    private val data = new Array[Array[Byte]](total);
    private val openTime = System.currentTimeMillis();

    def set(n: Int, data: Array[Byte]) {
        if (n < total) {
            if (this.data(n) == null)
                added += 1;
            this.data(n) = data;
        }
    }

    def isExpired(): Boolean = {
        (System.currentTimeMillis() - this.openTime) >= 1000
    }

    def isDone(): Boolean = {
        total == added;
    }

    def toBytes(): Array[Byte] = {
        val out = new ByteArrayOutputStream();
        for (i <- 0 to total - 1) {
            out.write(this.data(i));
        }
        return out.toByteArray();
    }

    override def toString(): String = {
        val objName = TextCache.get(TextTypes.OBJECT, objHash);
        val sb = new StringBuffer();
        sb.append("MultiPacket total=").append(total);
        sb.append(" recv=").append(added);
        sb.append(" object=(").append(objHash).append(")").append(objName);
        sb.append(" ").append(addr);
        return sb.toString()
    }
}