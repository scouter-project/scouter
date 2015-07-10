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
 *
 */
package scouter.server.tagcnt;

import scouter.util.BitUtil;
import scouter.util.HashUtil;
import scouter.util.LongKeyMap;
import scouter.util.LongSet;
import scouter.util.StringEnumer;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringSet;
import scouter.util.StringUtil;

object TagCountConfig {

    val entries = new StringKeyLinkedMap[StringSet]();

    val keyToName = new LongKeyMap[String]();
   
    class Tag(_tagGroup: String, _tagName: String) {

        val tagGroup = _tagGroup;
        val tagName = _tagName;
        val key = BitUtil.compsite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
        var set = entries.get(tagGroup);
        if (set == null) {
            set = new StringSet();
            entries.put(tagGroup, set);
        }
        set.put(tagName);

        keyToName.put(key, tagName);
    }

    def getTagNames(tagGroup: String): StringEnumer = {
        val tagNames = entries.get(tagGroup);
        return if (tagNames == null) StringSet.emptyEnumer else tagNames.keys();
    }

    def getTagGroups(): StringEnumer = {
        return entries.keys();
    }

    def getTagKey(tagGroup: String, tagName: String): Long = {
        return BitUtil.compsite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
    }

    class Service {
        val total = new Tag("service", "@total");
        val objectName = new Tag("service", "object");
        val ip = new Tag("service", "ip");
        val service = new Tag("service", "service");
        
        val service_elapsed= new Tag("service", "service-elapsed");
        val service_bytes = new Tag("service", "service-bytes");
        val service_errors = new Tag("service", "service-errors");
        
        val userAgent = new Tag("service", "user-agent");
        val error = new Tag("service", "error");
        val referer = new Tag("service", "referer");
        val group = new Tag("service", "group");

        val elapsed = new Tag("service", "elapsed");
        val sqltime = new Tag("service", "sqltime");
        val apitime = new Tag("service", "apitime");

        val city = new Tag("service", "city");
        val nation = new Tag("service", "nation");
        val visitor = new Tag("service", "visitor");
    }
    val service = new Service()

    class Alert {
        val total = new Tag("alert", "@total");
        val objectName = new Tag("alert", "object");
        val level = new Tag("alert", "level");
        val title = new Tag("alert", "title");
    }
    val alert = new Alert()

    def main(args: Array[String]) {
        System.out.println(StringUtil.toString(getTagGroups()));
        System.out.println(StringUtil.toString(getTagNames("service")));
    }
}
