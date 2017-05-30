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

package scouter.server.core.app;

import java.util.HashSet
import java.util.Set
import scouter.server.db.TextPermWR
import scouter.server.db.TextRD
import scouter.server.plugin.PlugInManager
import scouter.lang.TextTypes
import scouter.lang.pack.XLogPack
import scouter.util.DateUtil
import scouter.util.HashUtil
import scouter.util.IntIntLinkedMap
import scouter.util.IntSet;
import scouter.util.IntLinkedSet

object XLogGroupUtil {

    val groupMap = new IntIntLinkedMap().setMax(50000);

    def process(p: XLogPack) {
        if (p.group != 0)
            return ;
        p.group = makeGroupHash(p.service, p.endTime);
    }
    private def makeGroupHash(service: Int, endtime: Long): Int = {
        var groupHash = groupMap.get(service);
        if (groupHash == 0 || groupHash == 0) {
            val url = TextRD.getString(DateUtil.yyyymmdd(endtime), TextTypes.SERVICE, service);
            groupHash = getGroupHash(url);
            if (groupHash != 0) {
                groupMap.put(service, groupHash);
            }
        }
        return groupHash;
    }

    private val images = new HashSet[String]();
    private val statics = new HashSet[String]();

    images.add("gif");
    images.add("jpg");
    images.add("png");
    images.add("bmp");
    images.add("ico");

    statics.add("html");
    statics.add("htm");
    statics.add("css");
    statics.add("xml");
    statics.add("js");

    add(HashUtil.hash("*.jsp"), "*.jsp");
    add(HashUtil.hash("**"), "**");
    add(HashUtil.hash("images"), "images");
    add(HashUtil.hash("statics"), "statics");
    add(HashUtil.hash("/**"), "/**");

    private def add(hash: Int, name: String) {
        TextPermWR.add(TextTypes.GROUP, hash, name);
    }

    private val saved = new IntLinkedSet().setMax(1000);

    private val h2 = HashUtil.hash("*.jsp");
    private val h3 = HashUtil.hash("images");
    private val h4 = HashUtil.hash("statics");
    private val h5 = HashUtil.hash("/**");

    def getGroupHash(url: String): Int = {

        if (url == null)
            return 0;

        val x = url.lastIndexOf('.');
        if (x > 0) {
            val postfix = url.substring(x + 1).toLowerCase();
            if ("jsp".equals(postfix))
                return h2;

            if (images.contains(postfix))
                return h3;

            if (statics.contains(postfix))
                return h4;
        }

        if (url.length() == 0 || url.equals("/"))
            return h5;

        val x1 = url.indexOf('/', 1);
        if (x1 < 0) {
            return h5;
        }
        val groupName = url.substring(0, x1);
        val grpHash = HashUtil.hash(groupName);
        if (saved.contains(grpHash) == false) {
            add(grpHash, groupName);
        }
        return grpHash;
    }

}